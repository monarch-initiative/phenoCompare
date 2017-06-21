package org.monarchinitiative.phcompare;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A PatientGroup is a list of patients whose description files are in the same directory.
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 */
public class PatientGroup {
    File sourceDir;
    List<Patient> groupMembers;

    public PatientGroup(String path) throws IOException {
        File sourceDir = new File(path);
        if (!sourceDir.exists()) {
            throw new IOException("[PatientGroup.PatientGroup] Cannot find source directory " + path);
        }
        groupMembers = new ArrayList<>();
    }

    public List<Patient> getGroupMembers() {
        return groupMembers;
    }

    public void readPatientFiles() throws IOException {
        String[] filesInDir;

        if ((filesInDir = sourceDir.list()) != null) {
            for (String f : filesInDir) {
                Patient p = new Patient(sourceDir, f);
                groupMembers.add(p);
            }
        }
    }

}
