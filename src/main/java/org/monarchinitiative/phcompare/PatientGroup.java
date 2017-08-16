package org.monarchinitiative.phcompare;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A PatientGroup is a list of patients described in the patient records file.
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 *
class PatientGroup {
    private List<Patient> patients;   // list of Patients in this group

    /**
     * Initializes the PatientGroup object.
     *
    PatientGroup() {
        patients = new ArrayList<>();
    }

    List<Patient> getPatients() {
        return patients;
    }

    /**
     * Reads files for individual patients from directory specified when patientGroup was constructed.
     * If directory contains no files with the correct extension, the patientGroup remains empty.
     * @throws IOException     if error reading from patient file (Patient constructor throws
     *                         IOException)

    void readPatientFiles() throws IOException {
        String[] filesInDir;

        FilenameFilter fFilter = (File dir, String name) ->
           name.toLowerCase().endsWith(FNAME_EXT);

        if ((filesInDir = patientsFile.list(fFilter)) != null) {
            for (String f : filesInDir) {
                Patient p = new Patient(patientsFile, f);
                patients.add(p);
            }
        }
    }
     *

    /**
     * Size of patientGroup is the number of Patient objects in the list of group members.
     * @return    int number of patients in this PatientGroup
     *
    int size() { return patients.size(); }

    /**
     * Indicates whether or not this patientGroup is empty.
     * @return    boolean true if this patientGroup contains no patients, false otherwise
     *
    boolean isEmpty() { return patients.isEmpty(); }

} */
