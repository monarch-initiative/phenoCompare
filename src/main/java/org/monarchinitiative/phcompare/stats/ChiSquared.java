package org.monarchinitiative.phcompare.stats;
import static org.apache.commons.math3.stat.inference.TestUtils.chiSquare;
import static org.apache.commons.math3.stat.inference.TestUtils.chiSquareTest;


/**
 * Created by robinp on 6/14/17.
 */
public class ChiSquared {


    /** Value of observed group 1. */
    private long o[][];
    /** Value of observed group 2. */
    //private double e[];

    private double chisquare;
    private double chiSquareP;

    public ChiSquared(long[][] observed) {
        //e=expected;
        o=observed;
        calculateChi();
        calculateChiP();
    }

    public ChiSquared() {

    }

    private void calculateChi() {
        ChiSquared cs = new ChiSquared();
        chisquare = chiSquare(o);
    }

    private void calculateChiP() {
        ChiSquared cs = new ChiSquared();
        chiSquareP = chiSquareTest(o);
    }

    public double getChiSquare() { return chisquare; }

    public double getChiSquareP() { return chiSquareP; }
}
