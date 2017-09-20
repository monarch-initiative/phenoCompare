package org.monarchinitiative.phcompare.stats;
import com.github.phenomics.ontolib.ontology.data.TermId;

import static org.apache.commons.math3.stat.inference.TestUtils.chiSquare;
import static org.apache.commons.math3.stat.inference.TestUtils.chiSquareTest;

/**
 * @author Hannah Blau (blauh) based on ChiSquared.java created by robinp on 6/14/17
 * @version 0.0.1
 * @since 01 Sep 2017
 */
public class HPOChiSquared implements Comparable<HPOChiSquared> {
    private double chiSquare;    // chi-squared statistic
    private double chiSquareP;   // p-value associated with the chi-squared statistic for this HPO term
    private TermId HPOTermId;    // HPO term for which this is the chi-squared statistic
//    private long observed[][];   // observed counts for patients who have, do not have this HPO term

    public HPOChiSquared(TermId hpoTerm, long[][] observed) {
        chiSquare = chiSquare(observed);
        chiSquareP = chiSquareTest(observed);
        HPOTermId = hpoTerm;
    }

    /**
     * Compares the argument to this HPOChiSquared object. Orders first by chiSquare value, then by
     * HPOTermId.
     * @param hcs     the HPOChiSquared object to which this object is compared
     * @return int    0 if they are equal; negative if this precedes (is less than) hcs,
     *                positive if hcs precedes (is less than) this
     * @throws        IllegalArgumentException if argument is null
     */
    public int compareTo(HPOChiSquared hcs) throws IllegalArgumentException {
        if (hcs == null)
            throw new IllegalArgumentException("[HPOChiSquared.compareTo] Cannot compare to null object");
        int cmp = Double.compare(this.chiSquare, hcs.chiSquare);
        return cmp != 0 ? cmp : HPOTermId.getId().compareTo(hcs.HPOTermId.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HPOChiSquared that = (HPOChiSquared) o;
        return  Double.compare(chiSquare, that.chiSquare) == 0 &&
                HPOTermId.getIdWithPrefix().equals(that.HPOTermId.getIdWithPrefix());
    }

    public double getChiSquare() { return chiSquare; }

    public double getChiSquareP() { return chiSquareP; }

    public TermId getHPOTermId() { return HPOTermId; }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(chiSquare);
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + HPOTermId.hashCode();
        return result;
    }
}
