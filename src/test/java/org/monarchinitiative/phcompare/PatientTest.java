package org.monarchinitiative.phcompare;

import ontologizer.ontology.TermID;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.DataFormatException;

import static org.junit.Assert.*;

/**
 * Tests for the Patient class.
 * @author Hannah Blau (blauh)
 * @version 0.0.2
 */
public class PatientTest {
    private static File f;

    @BeforeClass
    public static void setUp() throws Exception {
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testFormatError() throws DataFormatException {
        thrown.expect(DataFormatException.class);
        thrown.expectMessage("Cannot parse patient record");
        Patient p = new Patient("PIGG\t26996948\tMakrythanasis\t4:517638C>T[homozygous,codingcoding|missense]\t");
    }

    @Test
    public void testGetters() throws Exception {
        Patient p = new Patient("PIGO\t22683086\tKrawitz\t" +
                "9:35090263G>A[heterozygous,codingcoding|missense];9:35091522T>TG[heterozygous,codingcoding|stop-codon]\t" +
                "HP:0003155;HP:0001252;HP:0001263;HP:0010804;HP:0002025;HP:0000316;HP:0001250;HP:0004322;HP:0000750;" +
                "HP:0001270;HP:0000455;HP:0000431;HP:0006118;HP:0000076;HP:0000637\n");
        Set<String> expected = new HashSet<>();
        expected.add("HP:0003155");
        expected.add("HP:0001252");
        expected.add("HP:0001263");
        expected.add("HP:0010804");
        expected.add("HP:0002025");
        expected.add("HP:0000316");
        expected.add("HP:0001250");
        expected.add("HP:0004322");
        expected.add("HP:0000750");
        expected.add("HP:0001270");
        expected.add("HP:0000455");
        expected.add("HP:0000431");
        expected.add("HP:0006118");
        expected.add("HP:0000076");
        expected.add("HP:0000637");

        Set<String> pTerms = new HashSet<>();
        for (TermID t : p.getHpoTerms()) {
            pTerms.add(t.toString());
        }

        System.out.print(p);
        assertEquals( "Gene name read from file is not as expected", "PIGO", p.getGene());
        assertEquals("HPO terms read from file are not as expected", expected, pTerms);
    }

    @Test
    public void testEquals() throws Exception {
        Patient p = new Patient("PIGV\t24129430\tHorn\t" +
                "1:27121547C>A[heterozygous,codingcoding|missense];1:27124258C>T[heterozygous,codingcoding|missense]\t" +
                "HP:0001804;HP:0000455;HP:0001821;HP:0003155;HP:0000316;HP:0000126;HP:0200007;HP:0010804;HP:0000431;" +
                "HP:0000175;HP:0001629;HP:0000072\n");
        Patient q = new Patient("",null);
        TreeSet<TermID> hpot = new TreeSet<>();
        hpot.add(new TermID("HP:0001804"));
        hpot.add(new TermID("HP:0000455"));
        hpot.add(new TermID("HP:0001821"));
        hpot.add(new TermID("HP:0003155"));
        hpot.add(new TermID("HP:0000316"));
        hpot.add(new TermID("HP:0000126"));
        hpot.add(new TermID("HP:0200007"));
        hpot.add(new TermID("HP:0010804"));
        hpot.add(new TermID("HP:0000431"));
        hpot.add(new TermID("HP:0000175"));
        hpot.add(new TermID("HP:0001629"));
        hpot.add(new TermID("HP:0000072"));

        assertFalse("Patient from file equals array of int!", p.equals(new int[] {1, 2, 3}));
        assertFalse("Patient from file equals empty patient!", p.equals(q));
        assertTrue("Patient from file does not equal new patient with same elements.",
                p.equals(new Patient("PIGV", hpot)));
    }
}
