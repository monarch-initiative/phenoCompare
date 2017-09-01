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
        GeneGroups ggs = new GeneGroups("src/test/resources/geneFiles/goodGenes.tsv");
        GeneGroup earlyg = ggs.getGeneGroup(0);
        GeneGroup lateg = ggs.getGeneGroup(1);
        assertEquals("whichGroup returns wrong group for PIGO",
                0, ggs.whichGroup("PIGO"));
        assertEquals("whichGroup returns wrong group for PIGV",
                1, ggs.whichGroup("PIGV"));
        assertTrue(earlyg.contains("PIGG"));
        assertTrue(earlyg.contains("PIGM"));
        assertFalse(earlyg.contains("PIGV"));
        assertFalse(lateg.contains("PIGG"));
    }
}