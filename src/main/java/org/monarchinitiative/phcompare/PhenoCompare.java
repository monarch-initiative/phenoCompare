package org.monarchinitiative.phcompare;

import org.apache.commons.cli.*;

import ontologizer.io.obo.OBOParser;
import ontologizer.io.obo.OBOParserException;
import ontologizer.io.obo.OBOParserFileInput;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermContainer;
import ontologizer.ontology.TermID;
import org.monarchinitiative.phcompare.stats.ChiSquared;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *  PhenoCompare compares two groups of patients to judge their overlap/divergence in the Human Phenotype
 *  Ontology.  PhenoCompare calculates for each node of HPO, the number of patients in each of the two groups
 *  who exhibit a phenotype covered by that node. Takes into account the type hierarchy.
 *     @author Hannah Blau (blauh)
 *     @version 0.0.1
 */
public class PhenoCompare {
    private static final int NUM_GROUPS = 2;

    private Ontology hpo;
    private String hpoPath;
    private String groupAdir;
    private String groupBdir;
    private String outfile;
    private PatientGroup[] patientGroups;
    // patientCounts maps from an HPO term to an array of the counts for each group of patients
    private SortedMap<TermID, int[]> patientCounts;

    private PhenoCompare() {
        hpo = null;
        hpoPath = groupAdir = groupBdir = outfile = "";
        patientGroups = new PatientGroup[NUM_GROUPS];
        patientCounts = new TreeMap<>();
    }

    /*
     * Calculates the chi squared statistic for each HPO term, based on counts of patients in each
     * group who have/do not have that phenotype.
     */
    private double calculateChiSq(int[] countsForTermID) {
        long[][] csq = new long[NUM_GROUPS][2];
        for (int g = 0; g < NUM_GROUPS; g++) {
            csq[g][0] = countsForTermID[g];                              // have phenotype
            csq[g][1] = patientGroups[g].size() - countsForTermID[g];    // don't have phenotype
        }
        return new ChiSquared(csq).getChiSquare();
    }

    /*
     * For an individual patient, increments the counts for all phenotypes exhibited by the patient,
     * including all nodes encountered between phenotypes mentioned in the patient's file and the root
     * node of the ontology.
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

    /*
     * For each group of patients, counts how many patients exhibit phenotype associated with
     * each node of ontology. HPO terms that do not appear in any patient file are implicitly given
     * a count of 0 for all patient groups.
     */
    private void countPatients() {
        for (int g = 0; g < NUM_GROUPS; g++) {
            for (Patient p : patientGroups[g].getGroupMembers()) {
                countPatient(p, g);
            }
        }
    }

    /*
     * Each group of patients is created from a set of patient files in the directory specified
     * in parameter paths.
     */
    private void createPatientGroups(String[] paths) throws IOException {
        for (int i = 0; i < NUM_GROUPS; i++) {
            patientGroups[i] = new PatientGroup(paths[i]);
            patientGroups[i].readPatientFiles();
        }
    }

    /*
     * Writes termID, term name, counts for each group of patients, and the chi squared statistic
     * to specified output file.
     */
    private void displayResults(String outPath) throws IOException {
        TermID tid;
        int[] counts;

        BufferedWriter bw = new BufferedWriter(new FileWriter(outPath));
        for (Map.Entry<TermID, int[]> entry : patientCounts.entrySet()) {
            tid = entry.getKey();
            counts = entry.getValue();

            bw.write(String.format("%s \t%s", tid, hpo.getTerm(tid).getName().toString()));
            for (int i = 0; i < NUM_GROUPS; i++) {
                bw.write(String.format("\tgroup%c: %5d", ('A' + i), counts[i]));
            }
            bw.write(String.format("\tChiSq: %7.3f", calculateChiSq(counts)));
            bw.newLine();
        }
        bw.close();
    }

    private void parseCommandLine(String[] args) {
        // create the Options
        Option hpoOpt = Option.builder("i")
                .longOpt("hpoDir")
                .required()
                .desc("directory containing hp.obo")
                .hasArg()
                .optionalArg(false)
                .argName("directory")
                .build();
        Option groupAopt = Option.builder("a")
                .longOpt("groupAdir")
                .required()
                .desc("directory containing group A patient files")
                .hasArg()
                .optionalArg(false)
                .argName("directory")
                .build();
        Option groupBopt = Option.builder("b")
                .longOpt("groupBdir")
                .required()
                .desc("directory containing group B patient files")
                .hasArg()
                .optionalArg(false)
                .argName("directory")
                .build();
        Option outfileOpt = Option.builder("o")
                .longOpt("outputFile")
                .required()
                .desc("results file")
                .hasArg()
                .optionalArg(false)
                .argName("path")
                .build();
        Option help = Option.builder("h")
                .longOpt("help")
                .required(false)
                .hasArg(false)
                .build();
        Options options = new Options();
        options.addOption(hpoOpt);
        options.addOption(groupAopt);
        options.addOption(groupBopt);
        options.addOption(outfileOpt);

        // create the command line parser and help formatter
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try {
            // parse the command line arguments
            CommandLine cmdl = parser.parse( options, args );
            if (cmdl.hasOption("h")) {
                // automatically generate usage information
                formatter.printHelp( "phenoCompare", options );
                System.exit(0);
            }
            hpoPath = cmdl.getOptionValue("i") + "hp.obo";
            groupAdir = cmdl.getOptionValue("a");
            groupBdir = cmdl.getOptionValue("b");
            outfile = cmdl.getOptionValue("o");
        }
        catch( ParseException e ) {
            System.err.println( "Incorrect command line arguments: " + e.getMessage() );
            formatter.printHelp( "phenoCompare", options );
            System.exit(1);
        }
    }

    /*
     * Code inherited from Sebastian Bauer (?) to read specified .obo file and create the corresponding
     * Ontology object.
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

    /*
     * Increments the count mapped to HPO term tid for the specified patient group.
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

    public static void main(String[] args) {
        PhenoCompare phenoC = new PhenoCompare();
        phenoC.parseCommandLine(args);

        // Load ontology from file.
        try {
            phenoC.hpo = parseObo(phenoC.hpoPath);
        } catch (IOException e) {
            System.err.println("ERROR: Problem reading OBO file\n\n");
            e.printStackTrace();
            System.exit(1);
        } catch (OBOParserException e) {
            System.err.println("ERROR: Problem parsing OBO file\n\n");
            e.printStackTrace();
            System.exit(1);
        }

        // Read patient files and create patient groups.
        try {
            phenoC.createPatientGroups(new String[] {phenoC.groupAdir, phenoC.groupBdir});
        } catch (IOException e) {
            System.err.println("ERROR: Problem reading patient files, " + e.getMessage() + "\n\n");
            e.printStackTrace();
            System.exit(1);
        }

        // For each node in the HPO ontology that covers one or more patients, count how many patients
        // in each group fall under that node. Any node of the hierarchy that is not referenced has counts of
        // 0 for each group.
        phenoC.countPatients();

        // Display counts for each node of the ontology that has a non-zero count for one or more groups.
        try {
            phenoC.displayResults(phenoC.outfile);
        } catch (IOException e) {
            System.err.println("ERROR: Problem writing output file " + phenoC.outfile + " : " +
                    e.getMessage() + "\n\n");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
