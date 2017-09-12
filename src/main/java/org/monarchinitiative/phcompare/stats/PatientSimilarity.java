package org.monarchinitiative.phcompare.stats;

import com.github.phenomics.ontolib.formats.hpo.HpoOntology;
import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.io.obo.hpo.HpoOboParser;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.similarity.JaccardSimilarity;
import ontologizer.ontology.TermID;
import org.monarchinitiative.phcompare.Patient;

import java.io.File;
import java.io.IOException;
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
    private JaccardSimilarity<HpoTerm, HpoTermRelation> similarity=null;



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



    public double getJaccardSimilarity(List<com.github.phenomics.ontolib.ontology.data.TermId> patient1, List<com.github.phenomics.ontolib.ontology.data.TermId> patient2) {
       return this.similarity.computeScore(patient1, patient2);
    }



    public void setupJaccard(String hpopath) {
        Ontology<HpoTerm, HpoTermRelation> ontology = parseOntology(hpopath);
        this.similarity =   new JaccardSimilarity<>(ontology);

    }


    public Ontology<HpoTerm, HpoTermRelation> parseOntology(String HPOpath) {
        HpoOntology hpo;
        Ontology<HpoTerm, HpoTermRelation> abnormalPhenoSubOntology =null;
        try {
            HpoOboParser hpoOboParser = new HpoOboParser(new File(HPOpath));
            hpo = hpoOboParser.parse();
            abnormalPhenoSubOntology = hpo.getPhenotypicAbnormalitySubOntology();
        } catch (IOException e) {
//            logger.error(String.format("Unable to parse HPO OBO file at %s", HPOpath ));
//            logger.error(e,e);
            System.exit(1);
        }
        return abnormalPhenoSubOntology;
    }


}
