package org.monarchinitiative.phcompare;

//import ontologizer.ontology.TermID;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermPrefix;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.TermPrefix;
import com.github.phenomics.ontolib.ontology.data.TermId;

import java.util.*;
import java.util.zip.DataFormatException;

/**
 * A patient consists of a gene name (the mutated gene) and a collection of HPO term IDs.
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 */
public class Patient {
    // Identifier for this patient
    private String pid;
    // Name of gene that is mutated in this patient
    private String gene;
    // Terms from Human Phenotype Ontology that describe this patient
    private Set<TermId> hpoTerms;

    public static TermPrefix HPOPREFIX = new ImmutableTermPrefix("HP");

    /**
     * Constructor extracts the gene name and HPO term IDs from the patient record
     * (one line of the patients file).
     * @param line                   line of text for this patient in the patients file
     * @throws DataFormatException   if gene name and/or HPO terms are not as expected
     */
    Patient(String line) throws DataFormatException {
        pid = gene = "";
        hpoTerms = new TreeSet<>();

        Scanner scan = new Scanner(line).useDelimiter("\\t");
        // Patient Id is in first column.
        if (scan.hasNext())
            pid = scan.next();
        // Gene name is in second column.
        if (scan.hasNext())
            gene = scan.next();
        // Skip over the three intervening columns until you reach the list of HPO term ids.
        for (int i = 0; i < 3 && scan.hasNext(); i++) {
            scan.next();
        }
        // Read list of HPO terms up to end of line.
        if (scan.hasNext()) {
            parseHPOterms(scan.next());
        }
        scan.close();
        if (pid.equals("") || gene.equals("") || hpoTerms.isEmpty()) {
            throw new DataFormatException("[Patient.Patient] Cannot parse patient record: " +
                    line + System.lineSeparator());
        }
    }

    /**
     * This constructor useful for test classes.
     * @param terms    TreeSet of TermIDs for HPO terms describing this patient
     */
    Patient(String id, String g, TreeSet<TermId> terms) {
        pid = id;
        gene = g;
        if (terms == null) {
            hpoTerms = new TreeSet<>();
        }
        else hpoTerms = terms;
    }

    /**
     * Two Patient objects are considered equal if they have the same id, same gene name,
     * and same set of HPO terms.
     * @param o    object to which this patient is compared
     * @return     true if this patient and the object o are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Patient patient = (Patient) o;
        return pid.equals(patient.pid) && gene.equals(patient.gene) && hpoTerms.equals(patient.hpoTerms);
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
        result = 31 * result + hpoTerms.hashCode();
        return result;
    }

    /**
     * Parses list of HPO terms from patient record. The HPO terms are separated by semicolons.
     * Each term is added to this object's hpoTerms.
     * @param listOfTerms    String consisting of HPO term IDs separated by semicolons
     */
    private void parseHPOterms(String listOfTerms) {
        Scanner scan = new Scanner(listOfTerms).useDelimiter(";");
        while (scan.hasNext()) {
            String hpostring=scan.next();
            int i=hpostring.indexOf(":");
            if (i<0) {
                System.err.println("ERROR -- Could not parse "+hpostring+" because we did not find a :");
                return;
            } else {
                hpostring=hpostring.substring(i+1);
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
