package org.monarchinitiative.phcompare;

import ontologizer.ontology.TermID;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.*;

/**
 * Tests for the Patient class. The constructor reads the patient file.
 *     @author Hannah Blau (blauh)
 *     @version 0.0.2
 */
public class PatientTest {
    private static File f;

    @BeforeClass
    public static void setUp() throws Exception {
        f = new File("src/test/resources/patientFiles/groupA/");
    }

    @Test(expected = IOException.class)
    public void testFileNotFound() throws Exception {
        Patient p = new Patient(f, "OMIM-missing.tab");
    }

    @Test
    public void testGetHpoTerms() throws Exception {
        Patient p = new Patient(f, "OMIM-611553.tab");
        Set<String> expected = new HashSet<>();
        Set<String> pTerms = new HashSet<>();
        expected.add("HP:0000470");
        expected.add("HP:0001263");
        expected.add("HP:0004322");
        expected.add("HP:0000465");
        expected.add("HP:0000766");
        expected.add("HP:0001639");
        expected.add("HP:0001631");
        expected.add("HP:0001999");
        expected.add("HP:0000006");

        for (TermID t : p.getHpoTerms()) {
            pTerms.add(t.toString());
        }
        assertEquals("HPO terms read from OMIM-611553.tab are not as expected", expected, pTerms);
    }

    @Test
    public void testEquals() throws Exception {
        Patient p = new Patient(f, "OMIM-613706.tab");
        Patient q = new Patient(null);
        TreeSet<TermID> hpot = new TreeSet<>();
        hpot.add(new TermID("HP:0008872"));
        hpot.add(new TermID("HP:0004322"));
        hpot.add(new TermID("HP:0001249"));
        hpot.add(new TermID("HP:0001252"));
        hpot.add(new TermID("HP:0000268"));
        hpot.add(new TermID("HP:0011220"));
        hpot.add(new TermID("HP:0000316"));
        hpot.add(new TermID("HP:0000369"));
        hpot.add(new TermID("HP:0000391"));
        hpot.add(new TermID("HP:0001642"));
        hpot.add(new TermID("HP:0001631"));
        hpot.add(new TermID("HP:0000953"));
        hpot.add(new TermID("HP:0000006"));

        assertFalse("Patient from OMIM-613706.tab equals array of int!", p.equals(new int[] {1, 2, 3}));
        assertFalse("Patient from OMIM-613706.tab equals empty patient!", p.equals(q));
        assertTrue("Patient from OMIM-613706.tab does not equal new patient with same HPO terms",
                p.equals(new Patient(hpot)));
    }
}