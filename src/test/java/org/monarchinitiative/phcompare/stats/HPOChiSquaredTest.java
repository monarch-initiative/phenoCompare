package org.monarchinitiative.phcompare.stats;

import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.monarchinitiative.phcompare.Patient.HPOPREFIX;

/**
 * @author Hannah Blau (blauh) based on ChiSquaredTest.java created by robinp on 6/14/17
 * @version 0.0.1
 * @since 15 Sep 2017
 */
public class HPOChiSquaredTest {

    private static HPOChiSquared hcs0, hcs1;
    private static long a[] = {7, 87, 12, 9};
    private static long b[] = {1, 18, 3, 1};
    private static long c[] = {3, 84, 4, 7};
    private static long o[][] = {a, b, c};
    private static TermId tid0, tid1;
    private static double epsilon = .0000000001;

    @BeforeClass
    public static void before() {
        tid0 = new ImmutableTermId(HPOPREFIX, "0001252");
        tid1 = new ImmutableTermId(HPOPREFIX, "0001804");
        hcs0 = new HPOChiSquared(tid0, o);
        hcs1 = new HPOChiSquared(tid1, o);
    }

    @Test
    public void testHPOChiSquare() {
        double expected = 5.488545890584;
        assertEquals(expected, hcs0.getChiSquare(), epsilon);
    }

    @Test
    public void testHPOChiSquareP() {
        double expected = 0.482842169465;
        assertEquals(expected, hcs1.getChiSquareP(), epsilon);
    }

    @Test
    public void testCompareTo() {
        assertTrue(hcs0.compareTo(hcs1) < 0);
    }

    @Test
    public void testHPOCorrectedP() {
        double expected = 0.482842169465;
        double cp = hcs1.correctPvalue(2);
        assertEquals(expected * 2, hcs1.getCorrectedP(), epsilon);
        cp = hcs1.correctPvalue(10);
        assertEquals(1.0, hcs1.getCorrectedP(), epsilon);
    }
}