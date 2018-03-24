package org.monarchinitiative.phcompare;

import com.github.phenomics.ontolib.formats.hpo.HpoTerm;
import com.github.phenomics.ontolib.formats.hpo.HpoTermRelation;
import com.github.phenomics.ontolib.io.obo.hpo.HpoOboParser;
import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.Ontology;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.*;
import static org.monarchinitiative.phcompare.Patient.HPOPREFIX;

/**
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 * @since 22 Mar 2018
 */
public class OutputMgrTest {

    @Test
    public void findSubtypesTest() {
        String[] args = {"-o", "src/test/resources/", "-p", "patfile", "-g", "genefile", "-r", "outfile"};
        PhenoCompare phc = new PhenoCompare(args);
        OutputMgr omgr = new OutputMgr(phc);

        TermId glaucoma = new ImmutableTermId(HPOPREFIX, "0000501");
        // abnormality of endocrine system
        TermId abnEndocrine = new ImmutableTermId(HPOPREFIX, "0000818");
        // abnormality of metabolism/homeostatis
        TermId abnMetabolism = new ImmutableTermId(HPOPREFIX, "0001939");
        // abnormality of urine homeostasis
        TermId abnUrineHomeo = new ImmutableTermId(HPOPREFIX, "0003110");
        TermId hyperuricosuria = new ImmutableTermId(HPOPREFIX, "0003149");
        // increased intraocular pressure
        TermId incIntraocular = new ImmutableTermId(HPOPREFIX, "0007906");
        // abnormality of urinary system physiology
        TermId abnUrinarySys = new ImmutableTermId(HPOPREFIX, "0011277");
        // abnormal eye physiology
        TermId abnEyePhys = new ImmutableTermId(HPOPREFIX, "0012373");
        TermId bacteriuria = new ImmutableTermId(HPOPREFIX, "0012461");
        // abnormal intraocular pressure
        TermId abnIntraocular = new ImmutableTermId(HPOPREFIX, "0012632");
        // abnormality of vitamin D metabolism
        TermId abnVitD = new ImmutableTermId(HPOPREFIX, "0100511");

        Set<TermId> terms = new HashSet<>();
        terms.add(glaucoma);
        terms.add(abnEndocrine);
        terms.add(abnMetabolism);
        terms.add(abnUrineHomeo);
        terms.add(hyperuricosuria);
        terms.add(incIntraocular);
        terms.add(abnUrinarySys);
        terms.add(abnEyePhys);
        terms.add(bacteriuria);
        terms.add(abnIntraocular);
        terms.add(abnVitD);

        Set<TermId> subtypes = omgr.findSubtypes(terms, abnMetabolism);
        assertFalse("AbnMetabolism contains glaucoma",
                subtypes.contains(glaucoma));
        assertFalse("AbnMetabolism contains abnormality of endocrine system",
                subtypes.contains(abnEndocrine));
        assertTrue("AbnMetabolism does not contain abnormality of metabolism/homeostatis",
                subtypes.contains(abnMetabolism));
        assertTrue("AbnMetabolism does not contain abnormality of urine homeostasis",
                subtypes.contains(abnUrineHomeo));
        assertTrue("AbnMetabolism does not contain hyperuricosuria",
                subtypes.contains(hyperuricosuria));
        assertFalse("AbnMetabolism contains increased intraocular pressure",
                subtypes.contains(incIntraocular));
        assertFalse("AbnMetabolism contains abnormality of urinary system physiology",
                subtypes.contains(abnUrinarySys));
        assertFalse("AbnMetabolism contains abnormal eye physiology",
                subtypes.contains(abnEyePhys));
        assertTrue("AbnMetabolism does not contain bacteriuria",
                subtypes.contains(bacteriuria));
        assertFalse("AbnMetabolism contains abnormal intraocular pressure",
                subtypes.contains(abnIntraocular));
        assertTrue("AbnMetabolism does not contain abnormailty of vitamin D metabolism",
                subtypes.contains(abnVitD));

//        subtypes = omgr.findSubtypes(terms, abnEyePhys);
    }
}