package org.monarchinitiative.phcompare.stats;

import org.monarchinitiative.phcompare.Patient;

import java.util.List;

/**
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 * @since 07 Sep 2017
 */
public class PatientSimilarity {
    private double[][] similarityMatrix;

    /**
     * Computes similarity matrix for a list of patients.
     * Assumes that 1.0 is the maximum value of the similarity metric. Assumes the similarity metric
     * is symmetric, so half of the matrix does not need to be calculated.
     * @param Patients        List of Patients for which pairwise similarity metric is computed.
     */
    public PatientSimilarity(List<Patient> Patients) {
        int dim = Patients.size();
        similarityMatrix = new double[dim][dim];
        for (int r = 0; r < dim; r++) {
            similarityMatrix[r][r] = 1.0;
            for (int c = 0; c < r; c++) {
                similarityMatrix[r][c] = similarityMatrix[c][r] =
                        Patients.get(r).similarity(Patients.get(c));
            }
        }
    }

    /**
     * Returns matrix of this PatientSimilarity object.
     * @return double[][]      matrix of similarity values
     */
    public double[][] getSimilarityMatrix() {
        return similarityMatrix;
    }
}
