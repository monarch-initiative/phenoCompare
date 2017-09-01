package org.monarchinitiative.phcompare;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import static org.monarchinitiative.phcompare.PhenoCompare.NUM_GROUPS;

/**
 * GeneGroups reads from a file of gene information to create multiple GeneGroup objects.
 * sets of patients to be compared by {@link PhenoCompare}. Gene names are Strings. The file of gene names
 * contains two lines, one for each set of gene names. The gene name file is in tsv format.
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 * @since 14 Aug 2017
 */
class GeneGroups {
    private GeneGroup[] geneGroups = new GeneGroup[NUM_GROUPS];

    /**
     * Reads multiple lists of gene names from specified file and creates corresponding GeneGroup objects
     * @param path              file containing lists of genes
     * @throws IOException      if file cannot be found
     * @throws EmptyGroupException  if after reading file, there's an empty gene group
     */
    GeneGroups(String path) throws IOException, EmptyGroupException {
        File genesFile = new File(path);

        if (!genesFile.exists()) {
            throw new IOException("[GeneGroups.GeneGroups] Cannot find genes file " + path +
                    System.lineSeparator());
        }
        Scanner scan = new Scanner(genesFile);
        String line;
        int groupNum = 0;
        while (groupNum < NUM_GROUPS && scan.hasNextLine()) {
            line = scan.nextLine();
            if (!line.startsWith("#")) {     // # marks a header line or comment in the input file
                geneGroups[groupNum] = new GeneGroup();
                readGeneNames(line, geneGroups[groupNum]);
                groupNum++;
            }
        }
        scan.close();
        for (int i = 0; i < NUM_GROUPS; i++) {
            // a geneGroup could wind up empty if genesFile does not contain enough lines, or if
            // it contains a line with no tokens
            if (geneGroups[i].isEmpty()) {
                throw new EmptyGroupException("[GeneGroups.GeneGroups] Empty group of genes from file " +
                        genesFile + System.lineSeparator());
            }
        }
    }

    /**
     * Finds the GeneGroup specified by the groupNum parameter.
     * @param groupNum          number of GeneGroup to be returned
     * @return GeneGroup        GeneGroup corresponding to groupNum, or null if groupNum is out of range
     */
    GeneGroup getGeneGroup(int groupNum) {
        if (groupNum > -1 && groupNum < NUM_GROUPS) {
            return geneGroups[groupNum];
        }
        else return null;
    }

    /**
     * Identifies the GeneGroup to which this geneName belongs (if any)
     * @param geneName   name of gene (String)
     * @return int       number of the GeneGroup that contains this geneName, or -1 if no such group
     */
    int whichGroup(String geneName) {
        for (int i = 0; i < NUM_GROUPS; i++) {
            if (geneGroups[i].contains(geneName)) {
                return i;
            }
        }
        // searched all the geneGroups and none contains this geneName
        return -1;
    }

    /**
     * Reads a line containing multiple gene names separated by tab characters, adds each gene name to
     * the specified set.
     * @param line      line of input to be parsed
     * @param geneG     GeneGroup (set of gene names)
     */
    private void readGeneNames(String line, GeneGroup geneG) {
        Scanner scn = new Scanner(line).useDelimiter("\\t");
        while (scn.hasNext()) {
            geneG.addGene(scn.next());
        }
        scn.close();
    }
}
