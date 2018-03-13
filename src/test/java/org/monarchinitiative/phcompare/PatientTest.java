package org.monarchinitiative.phcompare;

import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.TermId;
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
import static org.monarchinitiative.phcompare.Patient.HPOPREFIX;

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
        thrown.expectMessage("Wrong number of fields in patient record");
        while (goodAndBadPatients.ready()) {
            // third entry of this file contains a patient record with no HPO terms
            // should throw a DataFormatException when we try to create a Patient from that line
            line = goodAndBadPatients.readLine();
            if (!line.startsWith("#")) {
                Patient p = new Patient(line);
            }
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

        // P1-PIGN	PIGN	24852103	Brady	Brady;2014;PIGN;Patient	18:59777066C>T[homozygous,splicingsplicing|5ss|disrupted]	HP:0001626;HP:0002089;HP:0001831;HP:0030030;HP:0000054;HP:0000175;HP:0000110;HP:0012165;HP:0002623;HP:0002566;HP:0000028;HP:0001060;HP:0025193;HP:0003244;HP:0000776;HP:0000316;HP:0000445;HP:0000377;HP:0000463;HP:0000271
        Set<String> expected = new HashSet<>();
        expected.add("HP:0000028");
        expected.add("HP:0000054");
        expected.add("HP:0000110");
        expected.add("HP:0000175");
        expected.add("HP:0000271");
        expected.add("HP:0000316");
        expected.add("HP:0000377");
        expected.add("HP:0000445");
        expected.add("HP:0000463");
        expected.add("HP:0000776");
        expected.add("HP:0001060");
        expected.add("HP:0001626");
        expected.add("HP:0001831");
        expected.add("HP:0002089");
        expected.add("HP:0002566");
        expected.add("HP:0002623");
        expected.add("HP:0003244");
        expected.add("HP:0012165");
        expected.add("HP:0025193");
        expected.add("HP:0030030");

        Set<String> pTerms = new HashSet<>();
        for (TermId t : p.getHpoTerms()) {
            pTerms.add(t.getIdWithPrefix());
        }
        assertEquals("Patient id read from file is not as expected", "P1-PIGN", p.getPid());
        assertEquals("Gene name read from file is not as expected", "PIGN", p.getGene());
        assertEquals("PubMed ID read from file is not as expected", "24852103", p.getPmid());
        assertEquals("Id-summary read from file is not as expected", "Brady;2014;PIGN;Patient", p.getIdSummary());
        assertEquals("HPO terms read from file are not as expected", expected, pTerms);
    }

    @Test
    public void testEquals() throws Exception {
        BufferedReader onlyGoodPatients = new BufferedReader(new FileReader(
                "src/test/resources/patientFiles/onlyGoodPatients.tsv"));
        for (int i = 0; i < 7; i++) {
            onlyGoodPatients.readLine();
        }
        // patient on eighth line of file
        // P7-PIGG	PIGG	26996948	Makrythanasis	Makrythanasis;2016;PIGG;JP01	4:517638C>T[homozygous,codingcoding|missense]	HP:0001250;HP:0004396;HP:0001263;HP:0000750;HP:0001252;HP:0001321;HP:0001510;HP:0002059;HP:0000717
        Patient p = new Patient(onlyGoodPatients.readLine());
        onlyGoodPatients.close();

        Patient q = new Patient("", "", "", "", null);
        TreeSet<TermId> hpot = new TreeSet<>();
        hpot.add(new ImmutableTermId(HPOPREFIX,"0000717"));
        hpot.add(new ImmutableTermId(HPOPREFIX,"0000750"));
        hpot.add(new ImmutableTermId(HPOPREFIX,"0001250"));
        hpot.add(new ImmutableTermId(HPOPREFIX,"0001252"));
        hpot.add(new ImmutableTermId(HPOPREFIX,"0001263"));
        hpot.add(new ImmutableTermId(HPOPREFIX,"0001321"));
        hpot.add(new ImmutableTermId(HPOPREFIX,"0001510"));
        hpot.add(new ImmutableTermId(HPOPREFIX,"0002059"));
        hpot.add(new ImmutableTermId(HPOPREFIX,"0004396"));
        Patient r = new Patient("P7-PIGG","PIGG", "26996948", "Makrythanasis;2016;PIGG;JP01", hpot);

        assertFalse("Patient from file equals array of int!" + System.lineSeparator() + p.toString(),
                p.equals(new int[] {1, 2, 3}));
        assertFalse("Patient from file equals empty patient!" + System.lineSeparator() + p.toString(),
                p.equals(q));
        assertTrue("Patient from file does not equal new patient with same elements." +
                System.lineSeparator() + p.toString() + r.toString(), p.equals(r));
    }
}
