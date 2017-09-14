package org.monarchinitiative.phcompare;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * GeneGroups reads from a file of gene information to create multiple GeneGroup objects. The file of
 * gene names contains one line for each gene group (and comments if desired). Gene names are Strings.
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 * @since 14 Aug 2017
 */
class GeneGroups {
    private List<GeneGroup> geneGroups = new ArrayList<>();

    /**
     * Reads multiple lists of gene names from specified file and creates corresponding GeneGroup objects
     * @param path              file containing lists of genes
     * @throws IOException      if file cannot be found
     * @throws EmptyGroupException  if no gene groups in the file
     */
    GeneGroups(String path) throws IOException, EmptyGroupException {
        File genesFile = new File(path);

        if (!genesFile.exists()) {
            throw new IOException("[GeneGroups.GeneGroups] Cannot find genes file " + path +
                    System.lineSeparator());
        }
        Scanner scan = new Scanner(genesFile);

        String line;
        GeneGroup g;
        while (scan.hasNextLine()) {
            line = scan.nextLine();
            // skip this line if it is a comment (marked with #) or a blank line
            if (!(line.startsWith("#") || line.isEmpty())) {
                g = new GeneGroup();
                readGeneNames(line, g);
                geneGroups.add(g);
            }
        }
        scan.close();

        if (geneGroups.isEmpty()) { // nothing but blank lines and comments in this file!
            throw new EmptyGroupException("[GeneGroups.GeneGroups] No gene groups found in file " +
                    genesFile + System.lineSeparator());
        }
    }

    /**
     * Finds the GeneGroup specified by the groupNum parameter.
     * @param groupNum          number of GeneGroup to be returned
     * @return GeneGroup        GeneGroup corresponding to groupNum, or null if groupNum is out of range
     */
    GeneGroup getGeneGroup(int groupNum) {
        if (groupNum > -1 && groupNum < geneGroups.size()) {
            return geneGroups.get(groupNum);
        }
        else return null;
    }

    /**
     * Returns number of gene groups in this object.
     * @return int    number of gene groups
     */
    int howManyGroups() {
        return geneGroups.size();
    }

    /**
     * Identifies the GeneGroup to which this geneName belongs (if any)
     * @param geneName   name of gene (String)
     * @return int       number of the GeneGroup that contains this geneName, or -1 if no such group
     */
    int whichGroup(String geneName) {
        for (int i = 0; i < geneGroups.size(); i++) {
            if (geneGroups.get(i).contains(geneName)) {
                return i;
            }
        }
        // searched all the geneGroups and none contains this geneName
        return -1;
    }

    /**
     * Reads a line containing multiple gene names separated by whitespace, adds each gene name to
     * the specified set.
     * @param line      line of input to be parsed
     * @param geneG     GeneGroup (set of gene names)
     */
    private void readGeneNames(String line, GeneGroup geneG) {
        Scanner scn = new Scanner(line);
        while (scn.hasNext()) {
            geneG.addGene(scn.next());
        }
        scn.close();
    }
}
