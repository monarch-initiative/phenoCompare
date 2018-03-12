package org.monarchinitiative.phcompare;

import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.zip.DataFormatException;

/**
 * A patient consists of a short id, a gene name (the mutated gene), PubMed ID, a longer id summary,
 * and a collection of HPO term IDs. The first, second, third, fifth, and seventh fields of the
 * patients file.
 *
 * Sample record from the patients file:
 * #ID	#SYMBOL	PMID	F_AUTH	ID_SUMMARY	VARIANTS	HPO
 * P9-PIGV	PIGV	24129430	Horn	Horn;2014;PIGV;Patient 5	1:27121547C>A[homozygous,codingcoding|missense]	HP:0000750;HP:0011344;HP:0001792;HP:0009381;HP:0003155;HP:0010804;HP:0000283;HP:0009882;HP:0001831;HP:0000271;HP:0001336
 *
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 */
public class Patient {
    // Identifier for this patient
    private String pid;
    // Name of gene that is mutated in this patient
    private String gene;
    // PubMed ID of the paper describing this patient
    private String pmid;
    // Id summary for this patient (reference to paper describing the case, affected gene, and
    // name of the individual family member)
    private String idSummary;
    // Terms from Human Phenotype Ontology that describe this patient
    private Set<TermId> hpoTerms;

    public static TermPrefix HPOPREFIX = new ImmutableTermPrefix("HP");
    private static final Logger logger = LogManager.getLogger();

    /**
     * Constructor extracts the patient id, gene name, id summary, and HPO term IDs from the patient record
     * (one line of the patients file).
     * @param line                   line of text for this patient in the patients file
     * @throws DataFormatException   if fields are not as expected
     */
    Patient(String line) throws DataFormatException {
        pid = gene = pmid = idSummary = "";
        hpoTerms = new TreeSet<>();

        String[] fields = line.split("\t");
        if (fields.length != 7)
            throw new DataFormatException("[Patient.Patient] Wrong number of fields in patient record:\n" + line);
        pid = fields[0];
        gene = fields[1];
        pmid = fields[2];
        idSummary = fields[4];
        parseHPOterms(fields[6]);
        if (pid.equals("") || gene.equals("") || pmid.equals("") || idSummary.equals("") ||
                hpoTerms.isEmpty()) {
            throw new DataFormatException("[Patient.Patient] Cannot parse patient record:\n" + line);
        }
    }

    /**
     * This constructor useful for test classes.
     * @param id       patient id field
     * @param g        gene name field
     * @param pm       PubMed ID field
     * @param summary  id summary field
     * @param terms    TreeSet of TermIDs for HPO terms describing this patient
     */
    Patient(String id, String g, String pm, String summary, TreeSet<TermId> terms) {
        pid = id;
        gene = g;
        pmid = pm;
        idSummary = summary;
        if (terms == null) {
            hpoTerms = new TreeSet<>();
        }
        else hpoTerms = terms;
    }

    /**
     * Two Patient objects are considered equal if they have the same patient id, same gene name,
     * same PubMed ID, and same set of HPO terms.
     * @param o    object to which this patient is compared
     * @return     true if this patient and the object o are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Patient patient = (Patient) o;
        return pid.equals(patient.pid) && gene.equals(patient.gene) &&
                pmid.equals(patient.pmid) && hpoTerms.equals(patient.hpoTerms);
    }

    /**
     * @return    String containing the patient id listed in the patient's record
     */
    String getPid() { return pid; }

    /**
     * @return    String containing name of the gene listed in the patient's record
     */
    String getGene() { return gene; }

    /**
     * @return    String containing PubMed ID listed in the patient's record
     */
    String getPmid() { return pmid; }

    /**
     * @return    String containing id summary listed in the patient's record
     */
    String getIdSummary() { return idSummary; }

    /**
     * @return    Set of HPO TermIDs for the terms listed in the patient's record
     */
    Set<TermId> getHpoTerms() { return hpoTerms; }

    public List<TermId> getListOfHpoTerms() { return new ArrayList<>(hpoTerms);}

    /**
     * Relies on the hashcode of the String and TreeSet classes. Assumes all instance variables
     * are non-null.
     * @return     hash value for this Patient.
     */
    @Override
    public int hashCode() {
        int result = pid.hashCode();
        result = 31 * result + gene.hashCode();
        result = 31 * result + pmid.hashCode();
        result = 31 * result + idSummary.hashCode();
        result = 31 * result + hpoTerms.hashCode();
        return result;
    }

    /**
     * Parses list of HPO terms from patient record. The HPO terms are separated by semicolons.
     * Each term is added to this object's hpoTerms.
     * @param listOfTerms    String consisting of HPO term IDs separated by semicolons
     */
    private void parseHPOterms(String listOfTerms) {
        String[] hpoTermIds = listOfTerms.split(";");

        for (String hpostring : hpoTermIds) {
            int i = hpostring.indexOf(":");
            if (i < 0) {
                logger.error("ERROR -- Could not parse " + hpostring+" because we did not find a :");
                return;
            } else {
                hpostring = hpostring.substring(i+1);
            }
            TermId id = new ImmutableTermId(HPOPREFIX,hpostring);
            hpoTerms.add(id);
        }
    }

    /**
     * Lists the patient id, gene, and HPO term IDs of this Patient.
     * @return     String containing textual representation of Patient object.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Patient: Id = ");
        sb.append(pid);
        sb.append("; Gene = ");
        sb.append(gene);
        sb.append("; PMID = ");
        sb.append(pmid);
        sb.append("; IdSummary = ");
        sb.append(idSummary);
        sb.append("; HPO Terms = ");
        sb.append(System.lineSeparator());
        for (TermId t : getHpoTerms()) {
            sb.append("\t");
            sb.append(t.toString());
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
