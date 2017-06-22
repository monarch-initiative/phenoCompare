package org.monarchinitiative.phcompare.stats;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by robinp on 6/14/17.
 */
public class ChiSquaredTest {


    long a[] = {7, 87, 12, 9};
    long b[] = {1, 18, 3, 1};
    long c[] = {3, 84, 4, 7};
    long o[][] = {a, b, c};
    //double e[] = {30, 60, 10};
    static double epsilon = .0000000001;

    @BeforeClass
    public static void setup() throws Exception {

    }


    @Test
    public void testChiSquare() {
        ChiSquared cs = new ChiSquared(o);
        double expected = 5.488545890584;

        Assert.assertEquals(expected,cs.getChiSquare(),epsilon);
    }

    @Test
    public void testChiSquareP() {
        ChiSquared cs = new ChiSquared(o);
        double expected = 0.482842169465;

        Assert.assertEquals(expected,cs.getChiSquareP(),epsilon);
    }

}
