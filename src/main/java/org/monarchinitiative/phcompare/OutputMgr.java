package org.monarchinitiative.phcompare;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.monarchinitiative.phcompare.stats.HPOChiSquared;
import org.monarchinitiative.phcompare.stats.PatientSimilarity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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
     * Returns subset of hpoTerms that are descendants of target (or identical to target).
     * @param hpoTerms        set of HPO Term Ids
     * @param target          potential ancestor (supertype) of terms in hpoTerms
     * @return Set<TermId>    set containing all terms from hpoTerms that are descendants (subtypes) of
     *                        target, including those that are identical to target.
     */
    Set<TermId> findSubtypes(Set<TermId> hpoTerms, TermId target) {
        Set<TermId> subtypes = new TreeSet<>();
        Ontology<HpoTerm, HpoTermRelation> ontology = PhenoCompare.getOntology();
        for (TermId tid : hpoTerms) {
            if (ontology.getAncestorTermIds(tid).contains(target)) {
                subtypes.add(tid);
            }
        }
        return subtypes;
    }

    /**
     * Creates detail file for specified HPO term, writes header line in file.
     * @param tidAsString        String representation of the HPO Term Id.
     * @param termName           Name of the term in English
     * @return BufferedWriter    for further writes to detail file
     * @throws IOException       if problem occurred in creating file or writing to it
     */
    private BufferedWriter initDetailFile(String tidAsString, String termName) throws IOException {
        File termDetailFile = new File(resultsDir,
                tidAsString.replace(':', '-') + ".tsv");
        termDetailFile.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(termDetailFile));
        // write header lines in the term detail file
        bw.write("# " + tidAsString + " " + termName);
        bw.newLine();
        bw.write("# GroupNum\tId Summary\tPubMed\tGene\tHPO term\tTerm name");
        bw.newLine();
        return bw;
    }

    /**
     * Writes Chi-squared statistics and p values to file named results.tsv in the results directory.
     * Writes detail file for each HPO term listing patients that fall under that term.
     * @throws IOException    if problem writing to any output file
     */
    void writeChiSquared() throws IOException {
        File chiSquaredFile = new File(resultsDir, "chiSquared.tsv");
        int nGroups = phenoC.getNumGroups();
        Map<TermId, PatientGroup[]> hpoPatientSubgroups = phenoC.getHpoPatientSubgroups();
        PatientGroup[] patientGroups = phenoC.getPatientGroups();
        PatientGroup[] subgroups;
        Map<TermId, HpoTerm> termMap = PhenoCompare.getTermMap();
        TermId tid;
        String tidString, termName;

        chiSquaredFile.createNewFile();
        BufferedWriter chisq = new BufferedWriter(new FileWriter(chiSquaredFile));
        // write header line
        chisq.write("#HPO TermId\tTerm Name\t");
        for (int g = 1; g <= nGroups; g++) {
            chisq.write(String.format("%s%d\t", "Group", g));
        }
        chisq.write("ChiSq\tUncorr p Value\tCorr p Value");
        chisq.newLine();
        // write one line for each HPO term in the Chi-squared file
        // write term detail file for each HPO term with listing of patients in each subgroup
        // who fall under that term
        for (HPOChiSquared hcs : phenoC.getTermChiSq()) {
            tid = hcs.getHPOTermId();
            tidString = tid.getIdWithPrefix();
            termName = termMap.get(tid).getName();
            subgroups = hpoPatientSubgroups.get(tid);

            BufferedWriter termDetail = initDetailFile(tidString, termName);
            chisq.write(String.format("%s\t%s", tidString, termName));
            for (int i = 0; i < nGroups; i++) {
                chisq.write(String.format("\t%5d/%d", subgroups[i].size(), patientGroups[i].size()));
                writeSubgroupDetail(termDetail, i + 1, subgroups[i], tid);
            }
            chisq.write(String.format("\t%7.3f\t%9.5f\t%9.5f", hcs.getChiSquare(), hcs.getChiSquareP(),
                    hcs.getCorrectedP()));
            chisq.newLine();
            termDetail.close();
        }
        chisq.close();
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

    /**
     * Writes detailed information about patients in each subgroup that are covered by the term tid.
     * If patient belongs under tid for multiple reasons (multiple paths in the ontology) then
     * one line is written for each of patient's HPO terms that falls under (or is equal to) tid.
     * @param bw            BufferedWriter for output
     * @param groupNum      number of the subgroup
     * @param subgroup      group of patients who fall under the category of tid
     * @param tid           HPO term that describes all patients in the subgroup either directly or
     *                      through inheritance if patient is annotated with a more specific term
     * @throws IOException  if BufferedWriter cannot write to file
     */
    private void writeSubgroupDetail(BufferedWriter bw, int groupNum, PatientGroup subgroup, TermId tid)
            throws IOException {
        String line;

        for (Patient p : subgroup.getPatients()) {
            line = String.format("%d\t%s\t%s\t%s", groupNum, p.getIdSummary(),
                    "https://www.ncbi.nlm.nih.gov/pubmed/" + p.getPmid(), p.getGene());
            for (TermId subtype : findSubtypes(p.getHpoTerms(), tid)) {
                bw.write(String.format("%s\t%s\t%s", line, subtype.getIdWithPrefix(),
                        PhenoCompare.getTermMap().get(subtype).getName()));
                bw.newLine();
            }
        }
    }
}
