package org.monarchinitiative.phcompare;

import java.util.ArrayList;
import java.util.List;

/**
 * A PatientGroup is a list of patients described in the patient records file.
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 */
class PatientGroup {
    private List<Patient> patients;   // list of Patients in this group

    /**
     * Initializes the PatientGroup object.
     */
    PatientGroup() {
        patients = new ArrayList<>();
    }

    /**
     * Adds a patient to the list of patients for this group.
     * @param p   Patient to be added to group
     */
    void addPatient(Patient p) {
        patients.add(p);
    }

    /**
     * @return    the list of patients for this group
     */
    List<Patient> getPatients() {
        return patients;
    }

    /**
     * Indicates whether or not this patientGroup is empty.
     * @return    boolean true if this patientGroup contains no patients, false otherwise
     */
    boolean isEmpty() { return patients.isEmpty(); }

    /**
     * Size of patientGroup is the number of Patient objects in the list of group members.
     * @return    int number of patients in this PatientGroup
     */
    int size() { return patients.size(); }
}