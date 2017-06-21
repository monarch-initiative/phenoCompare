package org.monarchinitiative.phcompare;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import ontologizer.association.Association;
import ontologizer.io.annotation.AssociationParser;
import ontologizer.io.obo.OBOParser;
import ontologizer.io.obo.OBOParserException;
import ontologizer.io.obo.OBOParserFileInput;
import ontologizer.ontology.Ontology;
import ontologizer.ontology.TermContainer;
import ontologizer.ontology.TermID;
import ontologizer.types.ByteString;

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
    private static final int GROUPA = 0;
    private static final int GROUPB = 1;

    private static Ontology hpo;
    private static PatientGroup[] patientGroups = new PatientGroup[NUM_GROUPS];
    private static Map<TermID, Integer[]> patientCounts = new HashMap<>();

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Error: incorrect number of arguments");
            System.err.println("Usage: java -jar phenoCompare.jar groupAdirectory groupBdirectory outDirectory");
            System.exit(1);
        }

        try {
            // Load Ontology from file
            hpo = parseObo(hpoPath);
        } catch (IOException e) {
            System.err.println("ERROR: Problem reading OBO file " + hpoPath + "\n\n");
            e.printStackTrace();
            System.exit(1);
        } catch (OBOParserException e) {
            System.err.println("ERROR: Problem parsing OBO file " + hpoPath + "\n\n");
            e.printStackTrace();
            System.exit(1);
        }
        // Read patient files for groups A and B
        try {
            for (int i = 0; i < NUM_GROUPS; i++) {
                patientGroups[i] = new PatientGroup(args[i]);
                patientGroups[i].readPatientFiles();
            }
        } catch (IOException e) {
            System.err.println("ERROR: Problem reading patient files, " + e.getMessage() + "\n\n");
            e.printStackTrace();
            System.exit(1);
        }

        // For each node in the HPO ontology that covers one or more patients, count how many patients
        // in each group fall under that node. Any node of the hierarchy that is not referenced has counts of
        // 0 for each group.
        countPatients();
    }

    private static void countPatients() {
        for (int g = 0; g < NUM_GROUPS; g++) {
            for (Patient p : patientGroups[g].getGroupMembers()) {
                for (TermID tid : p.getHpoTerms()) {
                    updateCount(tid, g);
                    for (TermID pid : hpo.getTermParents(tid)) {
                        updateCount(pid, g);
                    }
                }
            }
        }
    }

            /* Load associations
            List<Association> associations = parseAssociations(args[1], hpo);

            // Compute information content
            SortedMap<TermID, Double> informationContent =
                    computeInformationContent(hpo, associations);

            // Write out information content.
            writeInformationContent(args[2], informationContent);
            */

    private static void updateCount(TermID tid, int g) {
//        if (patientCounts.get(tid))
    }

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

    private static List<Association> parseAssociations(String pathGaf, Ontology ontology)
            throws IOException {
        System.err.println("Reading associations from GAF file " + pathGaf + " ...");
        AssociationParser parser =
                new AssociationParser(new OBOParserFileInput(pathGaf), ontology.getTermMap());
        while (!parser.parse())
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // swallow
            }
        System.err.println("=> done reading GAF file");
        return parser.getAssociations();
    }

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

    private static void writeInformationContent(String pathTxt,
                                                Map<TermID, Double> informationContent) throws FileNotFoundException {
        System.err.println("Writing ontology as .txt file to " + pathTxt + " ...");
        PrintWriter out = new PrintWriter(new File(pathTxt));
        for (Entry<TermID, Double> e : informationContent.entrySet()) {
            out.println(e.getKey() + "\t" + e.getValue());
        }
        out.close();
        System.err.println("=> done writing DOT file");
    }
}
