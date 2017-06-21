package org.monarchinitiative.phcompare;

import ontologizer.ontology.TermID;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;

/**
 * A patient is just a collection of HPO term IDs.
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 */
public class Patient {
    Set<TermID> hpoTerms;

    public Patient(File sourceDir, String fname) throws IOException {
        String hpoTermID = "";
        final int colNum = 7;    // column in which HPO terms appear in file

        hpoTerms = new HashSet<>();

        File patientFile = new File(sourceDir, fname);
        if (!patientFile.exists()) {
            throw new IOException("[Patient.Patient] Cannot find patient file " + fname +
                    " in directory " + sourceDir.getPath());

        }
        Scanner scan = new Scanner(patientFile);
        // First line of file is a header line; skip over it.
        if (scan.hasNextLine())
            scan.nextLine();
        for (int i = 0; i < colNum && scan.hasNext(); i++) {
            hpoTermID = scan.next();
        }
        hpoTerms.add(new TermID(hpoTermID));
        scan.nextLine();
    }

    public Set<TermID> getHpoTerms() {
        return hpoTerms;
    }

    public boolean hasHpoTerms() { return !hpoTerms.isEmpty(); }
}
