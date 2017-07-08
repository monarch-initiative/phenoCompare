package org.monarchinitiative.phcompare;

import ontologizer.ontology.TermID;
import org.junit.Test;

import java.io.IOException;
import java.util.TreeSet;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Tests for the PatientGroup class.
 *
 * @author Hannah Blau (blauh)
 * @version 0.0.2
 */
public class PatientGroupTest {
    private static String emptyDir = "src/test/resources/patientFiles/groupC/";
    private static String missingDir = "src/test/resources/patientFiles/groupD/";
    private static String normalDir = "src/test/resources/patientFiles/groupB/";

    @Test(expected = IOException.class)
    public void testFileNotFound() throws Exception {
        PatientGroup mpg = new PatientGroup(missingDir);
    }

    @Test
    public void testEmptyDirectory() throws Exception {
        PatientGroup epg = new PatientGroup(emptyDir);
        epg.readPatientFiles();
        assertTrue("Test 1: patient group should be empty but is not.", epg.getGroupMembers().isEmpty());
    }

    @Test
    public void testReadPatientFiles() throws Exception {
        PatientGroup pg = new PatientGroup(normalDir);
        pg.readPatientFiles();
//        System.out.println("\nPatients from " + normalDir);
//        for (Patient p : pg.getGroupMembers()) {
//            System.out.println(p);
//        }

        TreeSet<TermID> f239300 = new TreeSet<>();
        f239300.add(new TermID("HP:0000007"));
        f239300.add(new TermID("HP:0000175"));
        f239300.add(new TermID("HP:0000204"));
        f239300.add(new TermID("HP:0000219"));
        f239300.add(new TermID("HP:0000238"));
        f239300.add(new TermID("HP:0000272"));
        f239300.add(new TermID("HP:0000303"));
        f239300.add(new TermID("HP:0000316"));
        f239300.add(new TermID("HP:0000322"));
        f239300.add(new TermID("HP:0000358"));
        f239300.add(new TermID("HP:0000431"));
        f239300.add(new TermID("HP:0000455"));
        f239300.add(new TermID("HP:0000582"));
        f239300.add(new TermID("HP:0000637"));
        f239300.add(new TermID("HP:0000750"));
        f239300.add(new TermID("HP:0001090"));
        f239300.add(new TermID("HP:0001182"));
        f239300.add(new TermID("HP:0001216"));
        f239300.add(new TermID("HP:0001249"));
        f239300.add(new TermID("HP:0001250"));
        f239300.add(new TermID("HP:0001252"));
        f239300.add(new TermID("HP:0001344"));
        f239300.add(new TermID("HP:0001357"));
        f239300.add(new TermID("HP:0001545"));
        f239300.add(new TermID("HP:0001792"));
        f239300.add(new TermID("HP:0001831"));
        f239300.add(new TermID("HP:0002019"));
        f239300.add(new TermID("HP:0002251"));
        f239300.add(new TermID("HP:0002553"));
        f239300.add(new TermID("HP:0002714"));
        f239300.add(new TermID("HP:0003155"));
        f239300.add(new TermID("HP:0008611"));
        f239300.add(new TermID("HP:0009882"));
        f239300.add(new TermID("HP:0010804"));
        f239300.add(new TermID("HP:0010864"));
        f239300.add(new TermID("HP:0011800"));
        Patient p239300 = new Patient(f239300);

        TreeSet<TermID> f614207 = new TreeSet<>();
        f614207.add(new TermID("HP:0000007"));
        f614207.add(new TermID("HP:0000252"));
        f614207.add(new TermID("HP:0001250"));
        f614207.add(new TermID("HP:0001252"));
        f614207.add(new TermID("HP:0001256"));
        f614207.add(new TermID("HP:0001263"));
        f614207.add(new TermID("HP:0002059"));
        f614207.add(new TermID("HP:0002905"));
        f614207.add(new TermID("HP:0003155"));
        f614207.add(new TermID("HP:0003577"));
        f614207.add(new TermID("HP:0010864"));
        Patient p614207 = new Patient(f614207);

        TreeSet<TermID> f614749 = new TreeSet<>();
        f614749.add(new TermID("HP:0000007"));
        f614749.add(new TermID("HP:0000076"));
        f614749.add(new TermID("HP:0000252"));
        f614749.add(new TermID("HP:0000316"));
        f614749.add(new TermID("HP:0000431"));
        f614749.add(new TermID("HP:0000455"));
        f614749.add(new TermID("HP:0000637"));
        f614749.add(new TermID("HP:0000750"));
        f614749.add(new TermID("HP:0001249"));
        f614749.add(new TermID("HP:0001250"));
        f614749.add(new TermID("HP:0001252"));
        f614749.add(new TermID("HP:0001357"));
        f614749.add(new TermID("HP:0001510"));
        f614749.add(new TermID("HP:0001631"));
        f614749.add(new TermID("HP:0002023"));
        f614749.add(new TermID("HP:0002025"));
        f614749.add(new TermID("HP:0002119"));
        f614749.add(new TermID("HP:0003155"));
        f614749.add(new TermID("HP:0003196"));
        f614749.add(new TermID("HP:0003577"));
        f614749.add(new TermID("HP:0006118"));
        f614749.add(new TermID("HP:0010055"));
        f614749.add(new TermID("HP:0010804"));
        Patient p614749 = new Patient(f614749);

        List<Patient> pgm = pg.getGroupMembers();

        assertTrue(pgm.contains(p239300));
        assertTrue(pgm.contains(p614207));
        assertTrue(pgm.contains(p614749));
    }
}
