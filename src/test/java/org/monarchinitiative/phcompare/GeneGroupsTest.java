package org.monarchinitiative.phcompare;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 * @since 17 Aug 2017
 */
public class GeneGroupsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testMissingFile() throws Exception {
        thrown.expect(IOException.class);
        thrown.expectMessage("Cannot find genes file");
        GeneGroups gg = new GeneGroups("src/test/resources/geneFiles/missingGenes.tsv");
    }

    @Test
    public void testBadFileFormat() throws Exception {
        thrown.expect(EmptyGroupException.class);
        thrown.expectMessage("Empty group of genes");
        GeneGroups gg = new GeneGroups("src/test/resources/geneFiles/badGenes.tsv");
    }

    @Test
    public void testNormalGeneFile() throws Exception {
        GeneGroup earlyg = new GeneGroup();
        GeneGroup lateg = new GeneGroup();
        earlyg.addGene("PIGO");
        earlyg.addGene("PIGG");
        earlyg.addGene("PIGM");
        lateg.addGene("PIGV");
        GeneGroups gg = new GeneGroups("src/test/resources/geneFiles/goodGenes.tsv");
        assertEquals("Group 0 genes are not as expected. ", earlyg, gg.getGeneGroup(0));
        assertEquals("Group 1 genes are not as expected. ", lateg, gg.getGeneGroup(1));
    }
}