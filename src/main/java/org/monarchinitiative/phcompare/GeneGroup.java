package org.monarchinitiative.phcompare;

import java.util.Set;
import java.util.TreeSet;

/**
 * GeneGroup is a set of gene names (Strings) that corresponds to a group of patients who have mutations in
 * genes from this group.
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 * @since 30 Aug 2017
 */
public class GeneGroup {
    private Set<String> geneNames;    // names of the genes in this GeneGroup

    /**
     * Initializes the GeneGroup object.
     */
    GeneGroup() {
        geneNames = new TreeSet<>();
    }

    /**
     * Adds a gene to the set of genes for this group.
     * @param geneName      name of gene to be added to group
     */
    void addGene(String geneName) {
        geneNames.add(geneName);
    }

    /**
     * Indicates whether or not this geneGroup contains the specified geneName.
     * @param geneName      name of gene (String) to search for
     * @return boolean      true if this geneGroup contains the geneName, false otherwise
     */
    boolean contains(String geneName) {
        return geneNames.contains(geneName);
    }

    /**
     * Indicates whether or not this geneGroup is empty.
     * @return    boolean true if this geneGroup contains no gene names, false otherwise
     */
    boolean isEmpty() {
        return geneNames.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String str : geneNames) {
            sb.append(str);
            sb.append(", ");
        }
        return sb.substring(0, sb.lastIndexOf(","));
    }
}
