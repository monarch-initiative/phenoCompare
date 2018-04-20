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
    private double correctedP = -1.0;   // p-value after Bonferroni correction for multiple comparisons
    private TermId HPOTermId;    // HPO term for which this is the chi-squared statistic

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

    public double correctPvalue(int numComparisons) {
        correctedP = Math.min(chiSquareP * numComparisons, 1.0);
        return correctedP;
    }

    /**
     * Two HPOChiSquared objects are considered equal if they have the same chiSquare value and
     * the same HPO term id.
     * @param o    object to which this HPOChiSquared is compared
     * @return     true if this HPOChiSquared and the object o are equal, false otherwise
     */
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

    public double getCorrectedP() { return correctedP; }

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
