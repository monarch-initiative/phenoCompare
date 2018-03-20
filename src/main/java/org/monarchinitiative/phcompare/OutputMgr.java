package org.monarchinitiative.phcompare;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
// import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
// import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.monarchinitiative.phcompare.stats.HPOChiSquared;
import org.monarchinitiative.phcompare.stats.PatientSimilarity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 * @since 19 Mar 2018
 *
 * OutputMgr writes all the phenoCompare output files, including:
 *    --- dissimilarity matrix;
 *    --- Chi-squared stats and p-values for the HPO terms whose stats are significant;
 *    --- detail files for each of the HPO terms to record which patients fell into each of the patient groups
 *    for that term.
 */
public class OutputMgr {
    private static final Logger logger = LogManager.getLogger();
    private static PhenoCompare phenoC;
    private static File resultsDir;

    OutputMgr(PhenoCompare ph) {
        phenoC = ph;
        resultsDir = new File(ph.getResultsPath());
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }
    }

    /**
     * Writes Chi-squared statistics and p values to file named results.tsv in the results directory.
     * @throws IOException    if problem writing to any output file
     */
    void writeChiSquared() throws IOException {
        File chiSquaredFile = new File(resultsDir, "chiSquared.tsv");
        int nGroups = phenoC.getNumGroups();
        Map<TermId, PatientGroup[]> hpoPatientSubgroups = phenoC.getHpoPatientSubgroups();
        PatientGroup[] patientGroups = phenoC.getPatientGroups();
        PatientGroup[] subgroups;
        Map<TermId,HpoTerm> termMap = PhenoCompare.getTermMap();
        TermId tid;

        chiSquaredFile.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(chiSquaredFile));
        // write header line
        bw.write("#HPO TermId\tTerm Name\t");
        for (int g = 1; g <= nGroups; g++) {
            bw.write(String.format("%s%d\t", "Group", g));
        }
        bw.write("ChiSq\tUncorr p Value\tCorr p Value");
        bw.newLine();
        // write one line for each HPO term
        for (HPOChiSquared hcs : phenoC.getTermChiSq()) {
            tid = hcs.getHPOTermId();
            subgroups = hpoPatientSubgroups.get(tid);
            bw.write(String.format("%s\t%s", tid.getIdWithPrefix(), termMap.get(tid).getName()));
            for (int i = 0; i < nGroups; i++) {
                bw.write(String.format("\t%5d/%d", subgroups[i].size(), patientGroups[i].size()));
            }
            bw.write(String.format("\t%7.3f\t%9.5f\t%9.5f", hcs.getChiSquare(), hcs.getChiSquareP(),
                    hcs.getCorrectedP()));
            bw.newLine();
        }
        bw.close();
    }

    /**
     * Writes dissimilarity matrix to file named dissim.tsv in results directory. Converts
     * similarity matrix into dissimilarity matrix as it writes values to file.
     * R clustering function requires a dissimilarity matrix. Columns are separated by tabs.
     * @throws IOException     if problem writing to file
     */
    void writeDissim() throws IOException {
        File dissimFile = new File(resultsDir, "dissim.tsv");
        StringBuilder sb = new StringBuilder();
        BufferedWriter bw = new BufferedWriter(new FileWriter(dissimFile));

        dissimFile.createNewFile();
        PatientGroup[] patientGroups = phenoC.getPatientGroups();

        // Combine patient groups together to get one list of all patients
        List<Patient> pats = new ArrayList<>(patientGroups[0].getPatients());
        for (int g = 1; g < phenoC.getNumGroups(); g++) {
            pats.addAll(patientGroups[g].getPatients());
        }

        // compute similarity matrix for all patients
        int dim = pats.size();
        PatientSimilarity pSim = new PatientSimilarity(pats, PhenoCompare.getOntology());
        double[][] matrix = pSim.getSimilarityMatrix();

        // write header line for dissimilarity matrix
        // map Patient::getPid on pats, append to sb
        for (Patient p : pats)
            sb.append(String.format("\t%s", p.getPid()));
        sb.append(System.lineSeparator());

        // write dissimilarity matrix to outFile
        for (int r = 0; r < dim; r++) {
            for (int c = 0; c < dim; c++) {
                sb.append(String.format("\t%4.2f", 1.0 - matrix[r][c]));
            }
            sb.append(System.lineSeparator());
        }
        bw.write(sb.toString());
        bw.close();
    }
}
