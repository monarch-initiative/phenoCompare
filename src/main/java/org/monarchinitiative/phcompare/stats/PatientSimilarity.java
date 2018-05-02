package org.monarchinitiative.phcompare.stats;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import com.github.phenomics.ontolib.ontology.similarity.JaccardSimilarity;
import org.monarchinitiative.phcompare.Patient;

import java.util.List;

/**
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 * @since 07 Sep 2017
 */
public class PatientSimilarity {
    private double[][] similarityMatrix;
    /** Object to determine similarity between two patients by compairing the sets of HPO terms (as TermID objects)
     * representing the phgenotypic profiles of the two patients.
     */
    private JaccardSimilarity<HpoTerm, HpoTermRelation> similarity;

    /**
     * Computes similarity matrix for a list of patients.
     * Assumes that 1.0 is the maximum value of the similarity metric. Assumes the similarity metric
     * is symmetric, so half of the matrix does not need to be calculated.
     * @param Patients        List of Patients for which pairwise similarity metric is computed.
     */
    public PatientSimilarity(List<Patient> Patients,Ontology<HpoTerm, HpoTermRelation> ontology) {
        int dim = Patients.size();
        this.similarity =   new JaccardSimilarity<>(ontology);
        similarityMatrix = new double[dim][dim];
        for (int r = 0; r < dim; r++) {
            similarityMatrix[r][r] = 1.0;
            for (int c = 0; c < r; c++) {
                similarityMatrix[r][c] = similarityMatrix[c][r] =
                        getJaccardSimilarity(Patients.get(r).getListOfHpoTerms() , Patients.get(c).getListOfHpoTerms());
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

    private double getJaccardSimilarity(List<TermId> patient1, List<TermId> patient2) {
        return this.similarity.computeScore(patient1, patient2);
    }
}
