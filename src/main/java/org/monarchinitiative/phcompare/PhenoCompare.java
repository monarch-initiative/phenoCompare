package org.monarchinitiative.phcompare;

import com.github.phenomics.ontolib.formats.hpo.HpoOntology;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.io.obo.hpo.HpoOboParser;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

//import ontologizer.io.obo.OBOParser;
//import ontologizer.io.obo.OBOParserException;
//import ontologizer.io.obo.OBOParserFileInput;
//import ontologizer.ontology.Ontology;
//import ontologizer.ontology.TermContainer;
//import ontologizer.ontology.TermID;
import org.monarchinitiative.phcompare.stats.HPOChiSquared;
import org.monarchinitiative.phcompare.stats.PatientSimilarity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;
import java.util.zip.DataFormatException;

/**
 *  PhenoCompare compares several groups of patients to judge their overlap/divergence in the Human Phenotype
 *  Ontology.  PhenoCompare calculates for each node of HPO, the number of patients in each of the groups
 *  who exhibit a phenotype covered by that node. Takes into account the type hierarchy. Then applies a
 *  chi-squared test to determine for which HPO terms is there any significant difference among the groups.
 *     @author Hannah Blau (blauh)
 *     @version 0.0.1
 */
public class PhenoCompare {
    private GeneGroups geneGroups; // groups of genes corresponding to disease categories
    private String genesPath;      // path for input file containing lists of genes for the patient groups
//    private Ontology hpo;          // ontology of HPO terms
    private String hpoPath;        // path to directory containing .obo file for HPO
    private int numGroups;                 // number of gene groups (and hence patient groups)
    // patientCounts maps from an HPO term to an array of the counts for each group of patients
    private SortedMap<TermId, int[]> patientCounts;
    private PatientGroup[] patientGroups;    // array of patient groups
    private String patientsPath;   // path for input file containing one line per patient
    private String resultsFile;    // path for output file
    // termChiSq is a list of objects that pair an HPO term to the Chi-squared statistic for that term
    private List<HPOChiSquared> termChiSq;
    /** The fully parsed HPO Ontology from ontolib */
    private static com.github.phenomics.ontolib.ontology.data.Ontology<HpoTerm, HpoTermRelation> ontology=null;

    static private Map<TermId,HpoTerm> termMap=null;

    private PhenoCompare(String[] args) {

        // Initialize hpoPath, genesPath, patientsPath, and resultsFile from the command line arguments
        parseCommandLine(args);
        patientCounts = new TreeMap<>();
        termChiSq = new ArrayList<>();
    }

    /**
     * Creates a HPOChiSquared object for each HPO term whose expected counts meet the
     * minimum threshold. Adds the HPOChiSquared object to the list termChiSq.
     */
    private void calculateChiSq() {
        HPOChiSquared hcs;

        for (TermId tid : patientCounts.keySet()) {
            hcs = createChiSq(tid, patientCounts.get(tid));
            if (hcs != null) {
                termChiSq.add(hcs);
            }
        }
    }

    /**
     * Creates a HPOChiSquared object for the HPO term, based on counts of patients in each
     * group who have/do not have that phenotype.
     * @param hpoTerm         HPO termID
     * @param countsForTermID array of int containing counts of patients in each group
     * @return null           if one of expected counts is below threshold of 5
     *         HPOChiSquared     otherwise, object containg HPO termID and Chi-squared statistic
     */
    private HPOChiSquared createChiSq(TermId hpoTerm, int[] countsForTermID) {
        double expected;
        int[] totalHaveOrDont = new int[2];          // column totals of matrix
        int totalPatients;                           // grand total of all patients
        long[][] csq = new long[numGroups][2];
        for (int g = 0; g < numGroups; g++) {
            csq[g][0] = countsForTermID[g];                              // have phenotype
            csq[g][1] = patientGroups[g].size() - countsForTermID[g];    // don't have phenotype
            totalHaveOrDont[0] += csq[g][0];
            totalHaveOrDont[1] += csq[g][1];
        }
        totalPatients = totalHaveOrDont[0] + totalHaveOrDont[1];

        // Expected value for each cell of the matrix csq must be >= 5 for Chi-squared
        // statistic to be meaningful
        for (int g = 0; g < numGroups; g++) {
            for (int c = 0; c < 2; c++) {
                expected = (countsForTermID[g] * totalHaveOrDont[c]) / (double) totalPatients;
                if (expected < 5) {
                    return null;
               }
            }
        }

        return new HPOChiSquared(hpoTerm, csq);
    }

    /**
     * For an individual patient, increments the counts for all phenotypes exhibited by the patient,
     * including all nodes encountered between phenotypes mentioned in the patient's file and the root
     * node of the ontology.
     * @param p       patient whose phenotypes we are counting
     * @param group   integer index for patient's group (0 .. numGroups - 1)
     */
    private void countPatient(Patient p, int group) {
        Set<TermId> ancestors = new HashSet<>();
        for (TermId tid : p.getHpoTerms()) {
            // getTermsOfInducedGraph returns a set of TermIDs for ancestors of tid
            try {
                // Merging sets of TermIDs eliminates duplicates if a given ontology node appears
                // in the induced graph for more than one of patient's HPO terms.
//                ancestors.addAll(hpo.getTermsOfInducedGraph(null, tid));
                ancestors.addAll(ontology.getAncestorTermIds( tid));
            }
            // If tid is no longer in the ontology, getTermsOfInducedGraph results in
            // IllegalArgumentException.
            catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
            }
        }
        // Increment the count for the group containing this patient in all the ontology nodes that
        // cover the patient's reported phenotypes.
        for (TermId ancestor : ancestors) {
            updateCount(ancestor, group);
        }
    }

    /**
     * For each group of patients, counts how many patients exhibit phenotype associated with
     * each node of ontology. HPO terms that do not appear in any patient file are implicitly given
     * a count of 0 for all patient groups.
     */
    private void countPatients() {
        for (int g = 0; g < numGroups; g++) {
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

        // Initialize patient groups.
        patientGroups = new PatientGroup[numGroups];
        for (int g = 0; g < numGroups; g++) {
            patientGroups[g] = new PatientGroup();
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
        for (int g = 0; g < numGroups; g++) {
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
     * Writes termID, term name, counts for each group of patients, and the Chi-squared statistic
     * to output file specified as command line argument. Also writes dissimilarity matrix to file
     * named dissim.tsv in the same directory.
     * @throws IOException    if problem writing to either output file
     */
    private void displayResults() throws IOException {
        TermId tid;
        int[] counts;

        File outFile = new File(resultsFile);
        writeDissimilarity(new File(outFile.getParent(), "dissim.tsv"));
        BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
        // write header line
        bw.write("#HPO TermId\tTerm Name\t");
        for (int g = 1; g <= numGroups; g++) {
            bw.write(String.format("%s%d\t", "Group", g));
        }
        bw.write("ChiSq\tp Value");
        bw.newLine();
        // write one line for each HPO term
        for (HPOChiSquared hcs : termChiSq) {
            tid = hcs.getHPOTermId();
            counts = patientCounts.get(tid);
//            bw.write(String.format("%s\t%s", tid, hpo.getTerm(tid).getName().toString()));
//            HpoTerm t = termMap.get(tid);
            bw.write(String.format("%s\t%s", tid.getIdWithPrefix(), termMap.get(tid).getName()));
            for (int i = 0; i < numGroups; i++) {
                bw.write(String.format("\t%5d/%d", counts[i],patientGroups[i].size()));
            }
            bw.write(String.format("\t%7.3f\t%9.5f", hcs.getChiSquare(), hcs.getChiSquareP()));
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


    private static com.github.phenomics.ontolib.ontology.data.Ontology<HpoTerm, HpoTermRelation> getOntolibOntology(String HPOpath) {
        HpoOntology hpo;
        com.github.phenomics.ontolib.ontology.data.Ontology<HpoTerm, HpoTermRelation> abnormalPhenoSubOntology = null;
        try {
            HpoOboParser hpoOboParser = new HpoOboParser(new File(HPOpath));
            hpo = hpoOboParser.parse();
            abnormalPhenoSubOntology = hpo.getPhenotypicAbnormalitySubOntology();
        } catch (IOException e) {
//            logger.error(String.format("Unable to parse HPO OBO file at %s", HPOpath ));
//            logger.error(e,e);
            System.exit(1);
        }
        return abnormalPhenoSubOntology;
    }

    /*
     * Code inherited from Sebastian Bauer (?) to read specified .obo file and create the corresponding
     * Ontology object.
     *
     * @param pathObo    path to .obo file we want to read
     * @return Ontology  the Ontology object created from .obo file
     *
     */
//    private static Ontology parseObo(String pathObo) throws IOException, OBOParserException {
//        System.err.println("Reading ontology from OBO file " + pathObo + " ...");
//        OBOParser parser = new OBOParser(new OBOParserFileInput(pathObo));
//        String parseResult = parser.doParse();
//
//        System.err.println("Information about parse result:");
//        System.err.println(parseResult);
//        TermContainer termContainer =
//                new TermContainer(parser.getTermMap(), parser.getFormatVersion(), parser.getDate());
//        final Ontology ontology = Ontology.create(termContainer);
//        System.err.println("=> done reading OBO file");
//        return ontology;
//    }

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
     * @param group  index for patient group (0 .. numGroups - 1)
     * @param tid    HPO term ID
     */
    private void updateCount(TermId tid, int group) {
        if (patientCounts.containsKey(tid)) {
            // Have already seen this termID before, just increment existing array element.
            patientCounts.get(tid)[group]++;
        }
        else {
            // First time we have seen this termID, create a new map entry for it and record count of 1 for
            // specified group.
            int[] counts = new int[numGroups];
            counts[group]++;
            patientCounts.put(tid, counts);
        }
    }

    /**
     * Converts similarity matrix into dissimilarity matrix as it writes values to specified output file.
     * R clustering function requires a dissimilarity matrix. Columns are separated by tabs.
     * @param outFile          file to which matrix is written
     * @throws IOException     if problem writing to file
     */
    private void writeDissimilarity(File outFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));

        // Combine patient groups together to get one list of all patients
        List<Patient> pats = new ArrayList<>(patientGroups[0].getPatients());
        for (int g = 1; g < numGroups; g++) {
            pats.addAll(patientGroups[g].getPatients());
        }

        // compute similarity matrix for all patients
        int dim = pats.size();
        PatientSimilarity pSim = new PatientSimilarity(pats,ontology);
        double[][] matrix = pSim.getSimilarityMatrix();

        // write dissimilarity matrix to outFile
        for (int r = 0; r < dim; r++) {
            for (int c = 0; c < dim; c++) {
                sb.append(String.format("%4.2f\t", 1.0 - matrix[r][c]));
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(System.lineSeparator());
        }
        bw.write(sb.toString());
        bw.close();
    }

    /**
     * Main method for PhenoCompare class. The constructor parses command line arguments to find
     * input file and directory information. Creates the Ontology object for HPO from .obo file. Reads
     * groups of genes from genes file, then groups of patients from patients file. For each phenotype
     * mentioned in the patient files, counts the number of patients in each group who exhibit that
     * phenotype, while also updating counts for all ancestors of the phenotype in the HPO DAG.
     * Calculates Chi-squared statistic for each HPO term that has sufficiently high expected counts.
     * Writes results to user-specified output file.
     * @param args     command line arguments typed by user
     */
    public static void main(String[] args) {
        PhenoCompare phenoC = new PhenoCompare(args);

//        // Load ontology from file.
//        try {
//            phenoC.hpo = parseObo(phenoC.hpoPath);
//        } catch (IOException e) {
//            System.err.println("[PhenoCompare.main] Problem reading OBO file" + System.lineSeparator());
//            e.printStackTrace();
//            System.exit(1);
//        } catch (OBOParserException e) {
//            System.err.println("[PhenoCompare.main] Problem parsing OBO file" + System.lineSeparator());
//            e.printStackTrace();
//            System.exit(1);
//        }


        ontology= getOntolibOntology(phenoC.hpoPath);
        termMap=ontology.getTermMap();

        // Read genes file to form groups of genes
        try {
            phenoC.geneGroups = new GeneGroups(phenoC.genesPath);
            phenoC.numGroups = phenoC.geneGroups.howManyGroups();
        } catch (IOException | EmptyGroupException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Read file of patient records and create patient groups corresponding to gene groups.
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
        // Chi-squared statistic.
        phenoC.calculateChiSq();
        phenoC.termChiSq.sort(null);

        // Display counts and Chi-squared stats for each node of the ontology that meets the threshold for
        // Chi-squared to be meaningful.
        try {
            phenoC.displayResults();
        } catch (IOException e) {
            System.err.println("[PhenoCompare.main] Problem writing output file" + System.lineSeparator());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
