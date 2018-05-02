package org.monarchinitiative.phcompare;

import com.github.phenomics.ontolib.formats.hpo.HpoOntology;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.io.obo.hpo.HpoOboParser;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.monarchinitiative.phcompare.stats.HPOChiSquared;

import java.io.File;
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
    private String hpoPath;        // path to directory containing .obo file for HPO
    // hpoPatientSubgroups maps from an HPO term to an array of the patient subgroups covered by that term
    private SortedMap<TermId, PatientGroup[]> hpoPatientSubgroups;
    private int numGroups;         // number of gene groups (and hence patient groups)
    private Ontology<HpoTerm, HpoTermRelation> ontology;   // fully parsed HPO Ontology from ontolib
    private PatientGroup[] patientGroups;   // array of patient groups
    private String patientsPath;   // path for input file containing one line per patient
    private String resultsPath;    // path for output file
    // termChiSq is a list of objects that pair an HPO term to the Chi-squared statistic for that term
    private List<HPOChiSquared> termChiSq;

    private static final Logger logger = LogManager.getLogger();

    /**
     * PhenoCompare constructor.
     * @param args             command line args typed by user
     * @throws IOException     if thrown by getOntolibOntology
     * @throws ParseException  if parseCommandLine indicates that execution should halt
     */
    PhenoCompare(String[] args) throws IOException, ParseException {
        // Initialize hpoPath, genesPath, patientsPath, and resultsPath from the command line arguments
        if (parseCommandLine(args)) {
            // Initialize ontology fields
            ontology = getOntolibOntology(hpoPath);
            hpoPatientSubgroups = new TreeMap<>();
            termChiSq = new ArrayList<>();
        } else {
            throw new ParseException("");
        }
    }

    /**
     * Creates a HPOChiSquared object for each HPO term whose expected counts meet the
     * minimum threshold. Adds the HPOChiSquared object to the list termChiSq.
     * When all Chi-squared comparisons are complete, computes Bonferroni correction
     * for the p-values and retains only those terms for which the corrected p-value
     * is <= .05.
     */
    private void calculateChiSq() {
        HPOChiSquared hcs;
        int numComparisons = 0;
        int[] patientCounts = new int[numGroups];

        for (TermId tid : hpoPatientSubgroups.keySet()) {
            for (int i = 0; i < numGroups; i++) {
                // construct array of subgroup sizes for the HPO term tid
                patientCounts[i] = hpoPatientSubgroups.get(tid)[i].size();
            }
            hcs = createChiSq(tid, patientCounts);
            if (hcs != null) {
                termChiSq.add(hcs);
                numComparisons++;
            }
        }

        // Iterate through the termChiSq list. For each element, calculate the corrected P value
        // according to number of comparisons performed. Toss out any HPOChiSquared object whose
        // corrected P value exceeds the threshold of 0.05.
        Iterator<HPOChiSquared> iter = termChiSq.iterator();
        while (iter.hasNext()) {
            HPOChiSquared chi = iter.next();
            if (chi.correctPvalue(numComparisons) > 0.05) {
                iter.remove();
            }
        }
    }

    /**
     * Creates a HPOChiSquared object for the HPO term, based on counts of patients in each
     * group who have/do not have that phenotype.
     * @param hpoTerm         HPO termID
     * @param countsForTermID array of int containing counts of patients in each group
     *                        who have phenotype encoded by HPO termID
     * @return null           if one of expected counts is below threshold of 5
     *         HPOChiSquared  otherwise, object containg HPO termID and Chi-squared statistic
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
                expected = (patientGroups[g].size() * totalHaveOrDont[c]) / (double) totalPatients;
                if (expected < 5.0) {
                    return null;
                }
            }
        }

        return new HPOChiSquared(hpoTerm, csq);
    }

    /**
     * For each group of patients, counts how many patients exhibit phenotype associated with
     * each node of ontology. HPO terms that do not appear in any patient file are implicitly given
     * a count of 0 for all patient groups.
     */
    private void countPatients() {
        for (int g = 0; g < numGroups; g++) {
            for (Patient p : patientGroups[g].getPatients()) {
                recordPatientPhenotypes(p, g);
            }
        }
    }

    /**
     * Each group of patients is created from patient records in the patients file.
     * @throws IOException           if problem opening or reading patients file
     * @throws EmptyGroupException   if one or more patient groups is/are empty
     */
    private void createPatientGroups() throws IOException, EmptyGroupException {
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
        // If cannot parse the patient record or gene name is not recognizable, skip
        // over that line and log a warning message.
        Scanner scan = new Scanner(patientsFile);
        while (scan.hasNextLine()) {
            line = scan.nextLine();
            if (!line.startsWith("#")) {     // # marks a header line or comment in the input file
                try {
                    pat = new Patient(line);
                    geneName = pat.getGene();
                    group = geneGroups.whichGroup(geneName);
                    if (group > -1) {
                        patientGroups[group].addPatient(pat);
                    }
                    else {  // group = -1, this patient has an unknown gene
                        throw new DataFormatException(String.format(
                                "[PhenoCompare.createPatientGroups] Patient %s has an unrecognized gene: %s",
                                pat.getPid(), geneName));
                    }
                }
                catch (DataFormatException e) {
                    logger.warn(e.getMessage());
                }
            }
        }

        // Check whether one or more of the patient groups is/are empty.
        StringBuilder sb = new StringBuilder("[PhenoCompare.createPatientGroups] Empty patient group(s)");
        boolean emptyGroup = false;
        for (int g = 0; g < numGroups; g++) {
            if (patientGroups[g].isEmpty()) {
                emptyGroup = true;
                sb.append(String.format(" %d,", g));
            }
        }
        if (emptyGroup) {
            throw new EmptyGroupException(sb.substring(0, sb.lastIndexOf(",")));
        }
    }


    /**
     * Checks path string and adds a final separator character if not already there.
     * @param path       String containing path as user typed it on command line
     * @return String    path with final separator added if it was not already there
     */
    private String fixFinalSeparator(String path) {
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    SortedMap<TermId, PatientGroup[]> getHpoPatientSubgroups() {
        return hpoPatientSubgroups;
    }

    int getNumGroups() {
        return numGroups;
    }

    private static Ontology<HpoTerm, HpoTermRelation> getOntolibOntology(String HPOpath) throws IOException {
        HpoOntology hpo;
        Ontology<HpoTerm, HpoTermRelation> abnormalPhenoSubOntology;
        try {
            HpoOboParser hpoOboParser = new HpoOboParser(new File(HPOpath));
            hpo = hpoOboParser.parse();
            abnormalPhenoSubOntology = hpo.getPhenotypicAbnormalitySubOntology();
            return abnormalPhenoSubOntology;
        } catch (IOException e) {
            throw new IOException("[PhenoCompare.getOntolibOntology] Unable to parse HPO OBO file at " +
                    HPOpath, e);
        }
    }

    Ontology<HpoTerm, HpoTermRelation> getOntology() {
        return ontology;
    }

    PatientGroup[] getPatientGroups() {
        return patientGroups;
    }

    String getResultsPath() {
        return resultsPath;
    }

    List<HPOChiSquared> getTermChiSq() {
        return termChiSq;
    }

    Map<TermId, HpoTerm> getTermMap() {
        return ontology.getTermMap();
    }

    /**
     * Parses the command line options with Apache Commons CLI library. First looks for (optional) help option.
     * If no help option, looks for four required options:
     *     -g full path including filename for file of gene names
     *     -o directory where hp.obo file can be found
     *     -p full path including filename for file of patient data
     *     -r directory for output files
     * Sets the instance variables of this PhenoCompare object accordingly.
     * @param args    the arguments user typed on command line
     * @return boolean true if execution should continue, false if execution should terminate
     */
    private boolean parseCommandLine(String[] args) {
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
                .desc("directory for results")
                .hasArg()
                .optionalArg(false)
                .argName("directory")
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
                return false;
            }
            else {
                // parse the command line looking for required options
                // This branch executed if command line does not match help option but also does not trigger a
                // ParseException. Seems to occur only when user types the directory paths but omits -g -o -p -r.
                parseRequiredOptions(parser, reqOptions, args);
                return true;
            }
        }
        catch(ParseException e) {
            try {
                // parse the command line looking for required options
                parseRequiredOptions(parser, reqOptions, args);
                return true;
            } catch (ParseException pe) {
                System.err.println("Incorrect command line arguments --- " + pe.getMessage());
                formatter.printHelp(new PrintWriter(System.err, true), 80,
                        "phenoCompare", null, allOptions, formatter.getLeftPadding(),
                        formatter.getDescPadding(), null);
                return false;
            }
        }
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
        resultsPath = fixFinalSeparator(cmdl.getOptionValue("r"));
    }

    /**
     * For an individual patient, adds patient to appropriate patient subgroup for each phenotype
     * exhibited by the patient. This includes all nodes encountered between phenotypes mentioned
     * in the patient's file and the root node of the ontology.
     * @param p       patient whose phenotypes we are recording
     * @param group   integer index for patient's group (0 .. numGroups - 1)
     */
    private void recordPatientPhenotypes(Patient p, int group) {
        Set<TermId> ancestors = new HashSet<>();
        for (TermId tid : p.getHpoTerms()) {
            // Merging sets of TermIDs eliminates duplicates if a given ontology node appears
            // in the induced graph for more than one of patient's HPO terms.
            ancestors.addAll(ontology.getAncestorTermIds(tid));
        }
        // Add patient to list for the appropriate group in all the ontology nodes that
        // cover the patient's reported phenotypes.
        for (TermId ancestor : ancestors) {
            updatePatientSubgroups(ancestor, p, group);
        }
    }

    /**
     * Adds patient to the appropriate subgroup for specified HPO term tid.
     *
     * @param group  index for patient group (0 .. numGroups - 1)
     * @param pat    patient for which we are recording phenotypes
     * @param tid    HPO term ID
     */
    private void updatePatientSubgroups(TermId tid, Patient pat, int group) {
        if (hpoPatientSubgroups.containsKey(tid)) {
            // Have already seen this termID before, just add new patient to existing group.
            hpoPatientSubgroups.get(tid)[group].addPatient(pat);
        }
        else {
            // First time we have seen this termID. Initialize patient subgroups, add pat to correct
            // subgroup, and add new termID to mapping.
            PatientGroup[] subgroups = new PatientGroup[numGroups];
            for (int i = 0; i < numGroups; i++) {
                subgroups[i] = new PatientGroup();
            }
            subgroups[group].addPatient(pat);
            hpoPatientSubgroups.put(tid, subgroups);
        }
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
        try {
            PhenoCompare phenoC = new PhenoCompare(args);
            OutputMgr omgr = new OutputMgr(phenoC);

            // Read genes file to form groups of genes
            phenoC.geneGroups = new GeneGroups(phenoC.genesPath);
            phenoC.numGroups = phenoC.geneGroups.howManyGroups();

            // Read file of patient records and create patient groups corresponding to gene groups.
            phenoC.createPatientGroups();

            // For each node in the HPO ontology that covers one or more patients, count how many patients
            // in each group fall under that node. Any node of the hierarchy that is not referenced has counts of
            // 0 for each group.
            phenoC.countPatients();

            // For each HPO term whose expected frequency meets the minimum threshold, calculate the
            // Chi-squared statistic.
            phenoC.calculateChiSq();
            // sort the Chi-squared values so that the most significant results (higher Chi-squared)
            // are earlier in the list.
            phenoC.termChiSq.sort(Comparator.reverseOrder());

            // Output counts and Chi-squared stats for each node of the ontology that meets the threshold for
            // Chi-squared to be meaningful. Write dissimilarity matrix.
            omgr.writeChiSquared();
            omgr.writeDissim();
        } catch (ParseException e) {
            // Command line parsing indicates execution should terminate. parseCommandLine method already has
            // printed an error message, no need to do anything more
        } catch (Exception e) {
            logger.fatal("", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
