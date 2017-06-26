package org.monarchinitiative.phcompare;

import ontologizer.ontology.TermID;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

/**
 * A patient is just a collection of HPO term IDs.
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 */
class Patient {
    private Set<TermID> hpoTerms;

    /**
     * Constructor reads from the specified patient file, extracts the HPO term IDs from the correct
     * column, and stores those term IDs in the instance variable hpoTerms.
     * @param sourceDir      directory in which patient file will be found
     * @param fname          name of patient file
     * @throws IOException   if patient file cannot be opened
     */
    Patient(File sourceDir, String fname) throws IOException {
        String hpoTermID = "";
        final int colNum = 7;    // column in which HPO terms appear in file

        hpoTerms = new TreeSet<>();

        File patientFile = new File(sourceDir, fname);
        if (!patientFile.exists()) {
            throw new IOException("[Patient.Patient] Cannot find patient file " + fname +
                    " in directory " + sourceDir.getPath());

        }
        Scanner scan = new Scanner(patientFile).useDelimiter("\\t");
        // First line of file is a header line; skip over it.
        if (scan.hasNextLine())
            scan.nextLine();
        while (scan.hasNextLine()) {
            // Skip over the uninteresting columns until you reach the HPO term id.
            for (int i = 0; i < colNum && scan.hasNext(); i++) {
                hpoTermID = scan.next();
            }
            hpoTerms.add(new TermID(hpoTermID));
            // Throw away remainder of current line.
            scan.nextLine();
        }
    }

    Patient(TreeSet<TermID> terms) {
        if (terms == null) {
            hpoTerms = new TreeSet<>();
        }
        else hpoTerms = terms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Patient patient = (Patient) o;

        return hpoTerms.equals(patient.hpoTerms);
    }

    Set<TermID> getHpoTerms() {
        return hpoTerms;
    }

    @Override
    public int hashCode() {
        return getHpoTerms().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Patient:\n");
        for (TermID t : getHpoTerms()) {
            sb.append("\t");
            sb.append(t.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
