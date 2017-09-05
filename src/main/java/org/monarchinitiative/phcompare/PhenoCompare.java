package org.monarchinitiative.phcompare;

import org.monarchinitiative.phcompare.stats.ChiSquared;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ontologizer.io.obo.OBOParser;
import ontologizer.io.obo.OBOParserException;
import ontologizer.io.obo.OBOParserFileInput;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermContainer;
import ontologizer.ontology.TermID;
import org.monarchinitiative.phcompare.stats.HPOChiSquared;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;
import java.util.zip.DataFormatException;

/**
 *  PhenoCompare compares two groups of patients to judge their overlap/divergence in the Human Phenotype
 *  Ontology.  PhenoCompare calculates for each node of HPO, the number of patients in each of the two groups
 *  who exhibit a phenotype covered by that node. Takes into account the type hierarchy.
 *     @author Hannah Blau (blauh)
 *     @version 0.0.1
 */
public class PhenoCompare {
    static final int NUM_GROUPS = 2;
    private GeneGroups geneGroups; // groups of early and late genes
    private String genesPath;      // path for input file containing lists of genes for the patient groups
    private Ontology hpo;          // ontology of HPO terms
    private String hpoPath;        // path to directory containing .obo file for HPO
    // patientCounts maps from an HPO term to an array of the counts for each group of patients
    private SortedMap<TermID, int[]> patientCounts;
    private PatientGroup[] patientGroups;    // array of patient groups for early, late gene mutations
    private String patientsPath;   // path for input file containing one line per patient
    private String resultsFile;    // path for output file
    // termChiSq maps from an HPO term to the chi squared object for that term
    private List<HPOChiSquared> termChiSq;

    private PhenoCompare() {
        hpo = null;
        hpoPath = genesPath = patientsPath = resultsFile = "";
        patientGroups = new PatientGroup[NUM_GROUPS];
        for (int g = 0; g < NUM_GROUPS; g++) {
            patientGroups[g] = new PatientGroup();
        }
        patientCounts = new TreeMap<>();
        termChiSq = new ArrayList<>();
    }

    /**
     * Creates a HPOChiSquared object for each HPO term and adds it to the list termChiSq.
     */
    private void calculateChiSq() {
        for (TermID tid : patientCounts.keySet()) {
            termChiSq.add(createChiSq(tid, patientCounts.get(tid)));
        }
    }

    /**
     * Creates a HPOChiSquared object for the HPO term, based on counts of patients in each
     * group who have/do not have that phenotype.
     * @param countsForTermID array of int containing counts of patients in each group
     * @return ChiSquared     object containg Chi-squared statistic and its p value
     */
    private HPOChiSquared createChiSq(TermID hpoTerm, int[] countsForTermID) {
        long[][] csq = new long[NUM_GROUPS][2];
        for (int g = 0; g < NUM_GROUPS; g++) {
            csq[g][0] = countsForTermID[g];                              // have phenotype
            csq[g][1] = patientGroups[g].size() - countsForTermID[g];    // don't have phenotype
        }
        return new HPOChiSquared(hpoTerm, csq);
    }

    /**
     * For an individual patient, increments the counts for all phenotypes exhibited by the patient,
     * including all nodes encountered between phenotypes mentioned in the patient's file and the root
     * node of the ontology.
     * @param p       patient whose phenotypes we are counting
     * @param group   integer index for patient's group (0 .. NUM_GROUPS - 1)
     */
    private void countPatient(Patient p, int group) {
        Set<TermID> ancestors = new HashSet<>();
        for (TermID tid : p.getHpoTerms()) {
            // getTermsOfInducedGraph returns a set of TermIDs for ancestors of tid
            try {
                // Merging sets of TermIDs eliminates duplicates if a given ontology node appears
                // in the induced graph for more than one of patient's HPO terms.
                ancestors.addAll(hpo.getTermsOfInducedGraph(null, tid));
            }
            // If tid is no longer in the ontology, getTermsOfInducedGraph results in
            // IllegalArgumentException.
            catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
            }
        }
        // Increment the count for the group containing this patient in all the ontology nodes that
        // cover the patient's reported phenotypes.
        for (TermID ancestor : ancestors) {
            updateCount(ancestor, group);
        }
    }

    /**
     * For each group of patients, counts how many patients exhibit phenotype associated with
     * each node of ontology. HPO terms that do not appear in any patient file are implicitly given
     * a count of 0 for all patient groups.
     */
    private void countPatients() {
        for (int g = 0; g < NUM_GROUPS; g++) {
            for (Patient p : patientGroups[g].getPatients()) {
                countPatient(p, g);
            }
        }
    }

    /**
     * Each group of patients is created from patient records in the patients file.
     * @throws DataFormatException   if thrown by Patient constructor
     * @throws IOException           if problem opening or reading patients file
     * @throws EmptyGroupException   if one or both patient groups is/are empty
     */
    private void createPatientGroups() throws IOException, DataFormatException, EmptyGroupException {
        String geneName, line;
        int group;
        Patient pat;
        File patientsFile = new File(patientsPath);
        if (!patientsFile.exists()) {
            throw new IOException("[PhenoCompare.createPatientGroups] Cannot find patients file " +
                    patientsPath);
        }

        // Read patients file line by line; each line is one patient record. Create a patient
        // object and add it to the correct patient group according to which gene is mutated.
        Scanner scan = new Scanner(patientsFile);
        while (scan.hasNextLine()) {
            line = scan.nextLine();
            if (!line.startsWith("#")) {     // # marks a header line or comment in the input file
                pat = new Patient(line);
                geneName = pat.getGene();
                group = geneGroups.whichGroup(geneName);
                if (group > -1) {
                    patientGroups[group].addPatient(pat);
                } else {  // group = -1, this patient has an unkown gene
                    throw new DataFormatException("[PhenoCompare.createPatientGroups] Unknown gene name: " +
                            geneName + System.lineSeparator());
                }
            }
        }

        // Check whether one or more of the patient groups is/are empty.
        StringBuilder sb = new StringBuilder();
        for (int g = 0; g < NUM_GROUPS; g++) {
            if (patientGroups[g].isEmpty()) {
                sb.append("[PhenoCompare.createPatientGroups] Empty patient group ");
                sb.append(g);
                sb.append(System.lineSeparator());
            }
        }
        if (sb.length() > 0) {
            throw new EmptyGroupException(sb.toString());
        }
    }

    /**
     * Writes termID, term name, counts for each group of patients, and the chi squared statistic
     * to specified output file.
     * @param outPath         path (including filename) for output file
     * @throws IOException    if problem writing to output file
     */
    private void displayResults(String outPath) throws IOException {
        TermID tid;
        int[] counts;

        BufferedWriter bw = new BufferedWriter(new FileWriter(outPath));
        // write header line
        bw.write("#HPO TermId\tTerm Name\t");
        for (int g = 1; g <= NUM_GROUPS; g++) {
            bw.write(String.format("%s%d\t", "Group", g));
        }
        bw.write("ChiSq\tp Value");
        bw.newLine();
        // write one line for each HPO term
        for (HPOChiSquared hcs : termChiSq) {
            tid = hcs.getHPOtermID();
            counts = patientCounts.get(tid);
            bw.write(String.format("%s\t%s", tid, hpo.getTerm(tid).getName().toString()));
            for (int i = 0; i < NUM_GROUPS; i++) {
                bw.write(String.format("\t%5d", counts[i]));
            }
            bw.write(String.format("\t%7.3f\t%7.3f", hcs.getChiSquare(), hcs.getChiSquareP()));
            bw.newLine();
        }
        bw.close();
    }

    /**
     * Checks path string and adds a final separator character if not already there.
     * @param path       String containing path as user typed it on command line
     * @return String    path with final separator added if it was not already there
     */
    private String fixFinalSeparator(String path) {
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    /**
     * Parses the command line options with Apache Commons CLI library. First looks for (optional) help option.
     * If no help option, looks for four required options:
     *     -g full path including filename for file of gene names
     *     -o directory where hp.obo file can be found
     *     -p full path including filename for file of patient data
     *     -r full path including filename for output file
     * Sets the instance variables of this PhenoCompare object accordingly.
     * @param args    the arguments user typed on command line
     */
    private void parseCommandLine(String[] args) {
        // create the Options
        Option helpOpt= Option.builder("h")
                .longOpt("help")
                .required(false)
                .hasArg(false)
                .build();
        Option genesOpt = Option.builder("g")
                .longOpt("genes")
                .desc("file containing list of genes for each group")
                .hasArg()
                .optionalArg(false)
                .argName("path")
                .required()
                .build();
        Option hpoOpt = Option.builder("o")
                .longOpt("hpo")
                .desc("directory containing hp.obo")
                .hasArg()
                .optionalArg(false)
                .argName("directory")
                .required()
                .build();
        Option patientsOpt = Option.builder("p")
                .longOpt("patients")
                .desc("file containing patient records")
                .hasArg()
                .optionalArg(false)
                .argName("path")
                .required()
                .build();
        Option resultsOpt = Option.builder("r")
                .longOpt("results")
                .desc("results file")
                .hasArg()
                .optionalArg(false)
                .argName("path")
                .required()
                .build();
        Options helpOptions = new Options();
        helpOptions.addOption(helpOpt);
        Options reqOptions = new Options();
        reqOptions.addOption(genesOpt);
        reqOptions.addOption(hpoOpt);
        reqOptions.addOption(patientsOpt);
        reqOptions.addOption(resultsOpt);
        Options allOptions = reqOptions.addOption(helpOpt);

        // create the command line parser and help formatter
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            // parse the command line looking for help option
            CommandLine cmdl = parser.parse(helpOptions, args);
            if (cmdl.hasOption("h")) {
                // automatically generate usage information, write to System.out
                formatter.printHelp("phenoCompare", allOptions);
                System.exit(0);
            }
            else {
                // parse the command line looking for required options
                // This branch executed if command line does not match help option but also does not trigger a
                // ParseException. Seems to occur only when user types the directory paths but omits -i -o -a -b.
                parseRequiredOptions(parser, reqOptions, args);
            }
        }
        catch(ParseException e) {
            try {
                // parse the command line looking for required options
                parseRequiredOptions(parser, reqOptions, args);
            } catch (ParseException pe) {
                System.err.println("Incorrect command line arguments --- " + pe.getMessage());
                formatter.printHelp(new PrintWriter(System.err, true), 80,
                        "phenoCompare", null, allOptions, formatter.getLeftPadding(),
                        formatter.getDescPadding(), null);
                System.exit(1);
            }
        }
    }

    /**
     * Code inherited from Sebastian Bauer (?) to read specified .obo file and create the corresponding
     * Ontology object.
     *
     * @param pathObo    path to .obo file we want to read
     * @return Ontology  the Ontology object created from .obo file
     *
     */
    private static Ontology parseObo(String pathObo) throws IOException, OBOParserException {
        System.err.println("Reading ontology from OBO file " + pathObo + " ...");
        OBOParser parser = new OBOParser(new OBOParserFileInput(pathObo));
        String parseResult = parser.doParse();

        System.err.println("Information about parse result:");
        System.err.println(parseResult);
        TermContainer termContainer =
                new TermContainer(parser.getTermMap(), parser.getFormatVersion(), parser.getDate());
        final Ontology ontology = Ontology.create(termContainer);
        System.err.println("=> done reading OBO file");
        return ontology;
    }

    /**
     * Parses the command line arguments typed by user to look for the required (not help) options
     * @param psr                 command line parser
     * @param reqOptions          Options object containing all required options
     * @param args                command line arguments typed by user
     * @throws ParseException     if problem parsing args to find reqOptions
     */
    private void parseRequiredOptions(CommandLineParser psr, Options reqOptions, String[] args) throws ParseException {
        CommandLine cmdl = psr.parse(reqOptions, args);
        genesPath = cmdl.getOptionValue("g");
        hpoPath = fixFinalSeparator(cmdl.getOptionValue("o")) + "hp.obo";
        patientsPath = cmdl.getOptionValue("p");
        resultsFile = cmdl.getOptionValue("r");
    }

    /**
     * Increments the count mapped to HPO term tid for the specified patient group.
     *
     * @param group  index for patient group (0 .. NUM_GROUPS - 1)
     * @param tid    HPO term ID
     */
    private void updateCount(TermID tid, int group) {
        if (patientCounts.containsKey(tid)) {
            // Have already seen this termID before, just increment existing array element.
            patientCounts.get(tid)[group]++;
        }
        else {
            // First time we have seen this termID, create a new map entry for it and record count of 1 for
            // specified group.
            int[] counts = new int[NUM_GROUPS];
            counts[group]++;
            patientCounts.put(tid, counts);
        }
    }

    /**
     * Main method for PhenoCompare class. Parses the command line arguments to find directory information,
     * then creates the Ontology object for HPO from .obo file. Reads two groups (A, B) of patient files.
     * For each phenotype mentioned in the patient files, counts the number of patients in each group that
     * exhibit that phenotype, while also updating counts for all ancestors of the phenotype in the HPO DAG.
     * Calculates Chi-squared statistic for each HPO term with counts > 0 and writes results to
     * user-specified output file.
     * @param args     command line arguments typed by user
     */
    public static void main(String[] args) {
        PhenoCompare phenoC = new PhenoCompare();
        phenoC.parseCommandLine(args);

        // Load ontology from file.
        try {
            phenoC.hpo = parseObo(phenoC.hpoPath);
        } catch (IOException e) {
            System.err.println("[PhenoCompare.main] Problem reading OBO file" + System.lineSeparator());
            e.printStackTrace();
            System.exit(1);
        } catch (OBOParserException e) {
            System.err.println("[PhenoCompare.main] Problem parsing OBO file" + System.lineSeparator());
            e.printStackTrace();
            System.exit(1);
        }

        // Form two groups of genes corresponding to the two groups of patients
        try {
            phenoC.geneGroups = new GeneGroups(phenoC.genesPath);
        } catch (IOException | EmptyGroupException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Read file of patient records and create patient groups.
        try {
            phenoC.createPatientGroups();
        } catch (DataFormatException | IOException | EmptyGroupException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // For each node in the HPO ontology that covers one or more patients, count how many patients
        // in each group fall under that node. Any node of the hierarchy that is not referenced has counts of
        // 0 for each group.
        phenoC.countPatients();

        // For each HPO term whose expected frequency meets the minimum threshold, calculate the
        // chi squared statistic.
        phenoC.calculateChiSq();
        phenoC.termChiSq.sort(null);

        // Display counts and chi squared stats for each node of the ontology that meets the threshold for
        // Chi Squared to be meaningful.
        try {
            phenoC.displayResults(phenoC.resultsFile);
        } catch (IOException e) {
            System.err.println("[PhenoCompare.main] Problem writing output file " + phenoC.resultsFile +
                    System.lineSeparator());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
