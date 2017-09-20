package org.monarchinitiative.phcompare;


import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedReader;
import java.io.FileReader;
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
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testFormatError() throws Exception {
        String line;

        BufferedReader goodAndBadPatients = new BufferedReader(new FileReader(
                "src/test/resources/patientFiles/goodAndBadPatients.tsv"));
        thrown.expect(DataFormatException.class);
        thrown.expectMessage("Cannot parse patient record");
        while (goodAndBadPatients.ready()) {
            // third line of this file contains a patient record with no HPO terms
            // should throw a DataFormatException when we try to create a Patient from that line
            line = goodAndBadPatients.readLine();
            Patient p = new Patient(line);
        }
        goodAndBadPatients.close();
    }

    @Test
    public void testGetters() throws Exception {
        BufferedReader onlyGoodPatients = new BufferedReader(new FileReader(
                "src/test/resources/patientFiles/onlyGoodPatients.tsv"));
        String firstLine = onlyGoodPatients.readLine();
        if (firstLine.startsWith("#"))
            firstLine = onlyGoodPatients.readLine();
        Patient p = new Patient(firstLine);
        onlyGoodPatients.close();

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
        for (TermId t : p.getHpoTerms()) {
            pTerms.add(t.getIdWithPrefix());
        }
        assertEquals( "Gene name read from file is not as expected", "PIGO", p.getGene());
        assertEquals("HPO terms read from file are not as expected", expected, pTerms);
    }

    @Test
    public void testEquals() throws Exception {
        BufferedReader onlyGoodPatients = new BufferedReader(new FileReader(
                "src/test/resources/patientFiles/onlyGoodPatients.tsv"));
        for (int i = 0; i < 7; i++) {
            onlyGoodPatients.readLine();
        }
        // patient on eighth line of file, after two comment lines and five patient records
        Patient p = new Patient(onlyGoodPatients.readLine());
        onlyGoodPatients.close();

        Patient q = new Patient("",null);
        TreeSet<TermId> hpot = new TreeSet<>();
        TermPrefix hpoprefix=new ImmutableTermPrefix("HP");
        hpot.add(new ImmutableTermId(hpoprefix,"0001804"));
        hpot.add(new ImmutableTermId(hpoprefix,"0000455"));
        hpot.add(new ImmutableTermId(hpoprefix,"0001821"));
        hpot.add(new ImmutableTermId(hpoprefix,"0003155"));
        hpot.add(new ImmutableTermId(hpoprefix,"0000316"));
        hpot.add(new ImmutableTermId(hpoprefix,"0000126"));
        hpot.add(new ImmutableTermId(hpoprefix,"0200007"));
        hpot.add(new ImmutableTermId(hpoprefix,"0010804"));
        hpot.add(new ImmutableTermId(hpoprefix,"0000431"));
        hpot.add(new ImmutableTermId(hpoprefix,"0000175"));
        hpot.add(new ImmutableTermId(hpoprefix,"0001629"));
        hpot.add(new ImmutableTermId(hpoprefix,"0000072"));
        Patient r = new Patient("PIGV", hpot);

        assertFalse("Patient from file equals array of int!" + System.lineSeparator() + p.toString(),
                p.equals(new int[] {1, 2, 3}));
        assertFalse("Patient from file equals empty patient!" + System.lineSeparator() + p.toString(),
                p.equals(q));
        assertTrue("Patient from file does not equal new patient with same elements." +
                System.lineSeparator() + p.toString() + r.toString(), p.equals(r));
    }
}
