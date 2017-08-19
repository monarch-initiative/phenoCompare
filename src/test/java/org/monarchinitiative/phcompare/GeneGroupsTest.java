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
        Set<String> earlyg = new TreeSet<>();
        Set<String> lateg = new TreeSet<>();
        earlyg.add("PIGO");
        earlyg.add("PIGG");
        earlyg.add("PIGM");
        lateg.add("PIGV");
        GeneGroups gg = new GeneGroups("src/test/resources/geneFiles/goodGenes.tsv");
        assertEquals("Early genes are not as expected. ", earlyg, gg.getEarlyGenes());
        assertEquals("Late genes are not as expected. ", lateg, gg.getLateGenes());
        assertFalse("PIGF should not be an early gene", gg.isEarlyGene("PIGF"));
        assertTrue("PIGV should be recognized as a late gene", gg.isLateGene("PIGV"));
    }
}