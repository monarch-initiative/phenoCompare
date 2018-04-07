package org.monarchinitiative.phcompare;

import com.github.phenomics.ontolib.ontology.data.ImmutableTermId;
import com.github.phenomics.ontolib.ontology.data.TermId;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.monarchinitiative.phcompare.Patient.HPOPREFIX;

/**
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 * @since 22 Mar 2018
 */
public class OutputMgrTest {

    @Test
    public void findSubtypesTest() throws Exception {
        String[] args = {"-o", "src/main/resources/", "-p", "patfile", "-g", "genefile", "-r", "outfile"};
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
        assertFalse("AbnMetabolism subsumes glaucoma but should not",
                subtypes.contains(glaucoma));
        assertFalse("AbnMetabolism subsumes abnormality of endocrine system but should not",
                subtypes.contains(abnEndocrine));
        assertTrue("AbnMetabolism does not subsume abnormality of metabolism/homeostatis but it should",
                subtypes.contains(abnMetabolism));
        assertTrue("AbnMetabolism does not subsume abnormality of urine homeostasis but it should",
                subtypes.contains(abnUrineHomeo));
        assertTrue("AbnMetabolism does not subsume hyperuricosuria but it should",
                subtypes.contains(hyperuricosuria));
        assertFalse("AbnMetabolism subsumes increased intraocular pressure but should not",
                subtypes.contains(incIntraocular));
        assertFalse("AbnMetabolism subsumes abnormality of urinary system physiology but should not",
                subtypes.contains(abnUrinarySys));
        assertFalse("AbnMetabolism subsumes abnormal eye physiology but should not",
                subtypes.contains(abnEyePhys));
        assertTrue("AbnMetabolism does not subsume bacteriuria but it should",
                subtypes.contains(bacteriuria));
        assertFalse("AbnMetabolism subsumes abnormal intraocular pressure but should not",
                subtypes.contains(abnIntraocular));
        assertTrue("AbnMetabolism does not subsume abnormality of vitamin D metabolism but it should",
                subtypes.contains(abnVitD));

        subtypes = omgr.findSubtypes(terms, abnEyePhys);
        assertTrue("AbnEyePhys does not subsume glaucoma but it should",
                subtypes.contains(glaucoma));
        assertFalse("AbnEyePhys subsumes abnormality of endocrine system but should not",
                subtypes.contains(abnEndocrine));
        assertFalse("AbnEyePhys subsumes abnormality of metabolism/homeostatis but should not",
                subtypes.contains(abnMetabolism));
        assertFalse("AbnEyePhys subsumes abnormality of urine homeostasis but should not",
                subtypes.contains(abnUrineHomeo));
        assertFalse("AbnEyePhys subsumes hyperuricosuria but should not",
                subtypes.contains(hyperuricosuria));
        assertTrue("AbnEyePhys does not subsume increased intraocular pressure but it should",
                subtypes.contains(incIntraocular));
        assertFalse("AbnEyePhys subsumes abnormality of urinary system physiology but should not",
                subtypes.contains(abnUrinarySys));
        assertTrue("AbnEyePhys does not subsume abnormal eye physiology but it should",
                subtypes.contains(abnEyePhys));
        assertFalse("AbnEyePhys subsumes bacteriuria but should not",
                subtypes.contains(bacteriuria));
        assertTrue("AbnEyePhys does not subsume abnormal intraocular pressure but it should",
                subtypes.contains(abnIntraocular));
        assertFalse("AbnEyePhys subsumes abnormality of vitamin D metabolism but should not",
                subtypes.contains(abnVitD));
        
        subtypes = omgr.findSubtypes(terms, abnUrineHomeo);
        assertFalse("AbnUrineHomeo subsumes glaucoma but should not",
                subtypes.contains(glaucoma));
        assertFalse("AbnUrineHomeo subsumes abnormality of endocrine system but should not",
                subtypes.contains(abnEndocrine));
        assertFalse("AbnUrineHomeo subsumes abnormality of metabolism/homeostatis but should not",
                subtypes.contains(abnMetabolism));
        assertTrue("AbnUrineHomeo does not subsume abnormality of urine homeostasis but it should",
                subtypes.contains(abnUrineHomeo));
        assertTrue("AbnUrineHomeo does not subsume hyperuricosuria but it should",
                subtypes.contains(hyperuricosuria));
        assertFalse("AbnUrineHomeo subsumes increased intraocular pressure but should not",
                subtypes.contains(incIntraocular));
        assertFalse("AbnUrineHomeo subsumes abnormality of urinary system physiology but should not",
                subtypes.contains(abnUrinarySys));
        assertFalse("AbnUrineHomeo subsumes abnormal eye physiology but should not",
                subtypes.contains(abnEyePhys));
        assertTrue("AbnUrineHomeo does not subsume bacteriuria but it should",
                subtypes.contains(bacteriuria));
        assertFalse("AbnUrineHomeo subsumes abnormal intraocular pressure but should not",
                subtypes.contains(abnIntraocular));
        assertFalse("AbnUrineHomeo subsumes abnormality of vitamin D metabolism but should not",
                subtypes.contains(abnVitD));
    }
}