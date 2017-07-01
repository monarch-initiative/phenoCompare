package org.monarchinitiative.phcompare;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A PatientGroup is a list of patients whose description files are in the same directory.
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 */
class PatientGroup {
    private static String FNAME_EXT = ".tab";      // filename extension for patient files
    private File sourceDir;
    private List<Patient> groupMembers;

    PatientGroup(String path) throws IOException {
        sourceDir = new File(path);
        if (!sourceDir.exists()) {
            throw new IOException("[PatientGroup.PatientGroup] Cannot find source directory " + path);
        }
        groupMembers = new ArrayList<>();
    }

    List<Patient> getGroupMembers() {
        return groupMembers;
    }

    /*
     * Reads files for individual patients from directory specified when patientGroup was constructed.
     */
    void readPatientFiles() throws IOException {
        String[] filesInDir;

        FilenameFilter fFilter = (File dir, String name) ->
           name.toLowerCase().endsWith(FNAME_EXT);

        if ((filesInDir = sourceDir.list(fFilter)) != null) {
            for (String f : filesInDir) {
                Patient p = new Patient(sourceDir, f);
                groupMembers.add(p);
            }
        }
    }

    int size() { return groupMembers.size(); }
}
