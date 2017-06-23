package org.monarchinitiative.phcompare;

import ontologizer.ontology.TermID;
import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for the Patient class, primarily the constructor (reads the patient file).
 *     @author Hannah Blau (blauh)
 *     @version 0.0.1
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class PatientTest {
    private static File f;
    private static Patient p;

    @BeforeClass
    public static void setUp() throws Exception {
        f = new File("/Users/blauh/phenoCompare/groupA/");
    }

    @Test
    public void test2GetHpoTerms() throws Exception {
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
        assertEquals(expected, pTerms);
    }

    @Test
    public void test0FileNotFound() throws Exception {
        try {
            p = new Patient(f, "OMIM-missing.tab");
        }
        catch (IOException e) {
            System.err.println("test0FileNotFound: catching IOException");
            System.err.println("ERROR: Problem reading patient files, " + e.getMessage() + "\n\n");
            e.printStackTrace();
        }
    }

    @Test
    public void test1Constructor() throws Exception {
        p = new Patient(f, "OMIM-611553.tab");
        System.out.println("\nHPO terms from file OMIM-611553");
        for (TermID t : p.getHpoTerms()) {
            System.out.println(t);
        }
    }
}