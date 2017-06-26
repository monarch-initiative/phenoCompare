package org.monarchinitiative.phcompare;

import ontologizer.io.obo.OBOParser;
import ontologizer.io.obo.OBOParserException;
import ontologizer.io.obo.OBOParserFileInput;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermContainer;
import ontologizer.ontology.TermID;

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
    private static final String hpoPath = "/Users/blauh/phenoCompare/hp.obo";
    private static final int NUM_GROUPS = 2;

    private Ontology hpo;
    private PatientGroup[] patientGroups = new PatientGroup[NUM_GROUPS];
    // patientCounts maps from an HPO term to an array of the counts for each group of patients
    private SortedMap<TermID, int[]> patientCounts = new TreeMap<>();

    PhenoCompare(String hpoPath) throws IOException, OBOParserException {
        // Load ontology from file.
        hpo = parseObo(hpoPath);
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
//            System.err.println(tid.toString() + " " + patientCounts.get(tid)[0] + " " + patientCounts.get(tid)[1]);
    }

    /*
     * Writes termID, term name, counts for each group of patients to specified output file.
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
                bw.write(String.format("\t%s%c: %5d", "group", ('A' + i), counts[i]));
            }
            bw.newLine();
        }
        bw.close();
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
    private static SortedMap<TermID, Double> computeInformationContent(Ontology ontology,
                                                                       List<Association> associations) {
        // First, build mapping from term to database ID
        HashMap<TermID, HashSet<ByteString>> termToDbId =
                new HashMap<TermID, HashSet<ByteString>>();

        for (Association a : associations) {
            if (!termToDbId.containsKey(a.getTermID())) {
                termToDbId.put(a.getTermID(), new HashSet<ByteString>());
            }
            termToDbId.get(a.getTermID()).add(a.getDB_Object());
        }

        // From this, derive absolute frequencies for annotation of dabase object ID with term
        Map<TermID, Integer> termFreqAbs = new HashMap<TermID, Integer>();
        for (TermID t : termToDbId.keySet()) {
            termFreqAbs.put(t, termToDbId.get(t).size());
        }

        // Get total number of genes with annotation
        final int numDbObjects = termFreqAbs.size();

        // From this, we can easily compute the information content
        TreeMap<TermID, Double> termInformationContent = new TreeMap<TermID, Double>();
        for (Entry<TermID, Integer> e : termFreqAbs.entrySet()) {
            termInformationContent.put(e.getKey(),
                    -Math.log(((double) e.getValue()) / numDbObjects));
        }
        return termInformationContent;
    }
*/

    public static void main(String[] args) {
        PhenoCompare phenoC = null;

        if (args.length != NUM_GROUPS + 1) {
            System.err.println("Error: incorrect number of arguments");
            // TODO: fix usage message so it works for any number of groups not just 2
            System.err.println("Usage: java -jar phenoCompare.jar groupAdirectory groupBdirectory outFile");
            System.exit(1);
        }

        // Load ontology from file.
        try {
            phenoC = new PhenoCompare(hpoPath);
        } catch (IOException e) {
            System.err.println("ERROR: Problem reading OBO file " + hpoPath + "\n\n");
            e.printStackTrace();
            System.exit(1);
        } catch (OBOParserException e) {
            System.err.println("ERROR: Problem parsing OBO file " + hpoPath + "\n\n");
            e.printStackTrace();
            System.exit(1);
        }

        // Read patient files and create patient groups.
        try {
            phenoC.createPatientGroups(args);
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
            phenoC.displayResults(args[NUM_GROUPS]);
        } catch (IOException e) {
            System.err.println("ERROR: Problem writing output file " + args[NUM_GROUPS] + " : " +
                    e.getMessage() + "\n\n");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
