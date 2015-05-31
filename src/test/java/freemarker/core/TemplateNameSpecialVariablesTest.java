package freemarker.core;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import freemarker.template.utility.StringUtil;
import freemarker.test.TemplateTest;

public class TemplateNameSpecialVariablesTest extends TemplateTest  {

    private static final Version[] BREAK_POINT_VERSIONS = new Version[] { Configuration.VERSION_2_3_0, Configuration.VERSION_2_3_22, Configuration.VERSION_2_3_23 };

    private static TemplateLoader createTemplateLoader(String specVar) {
        StringTemplateLoader tl = new StringTemplateLoader();
            tl.putTemplate("main.ftl",
                    "In main: ${" + specVar + "}\n"
                    + "<#import 'imp.ftl' as i>"
                    + "In imp: ${inImp}\n"
                    + "In main: ${" + specVar + "}\n"
                    + "<@i.impM>${" + specVar + "}</@>\n"
                    + "<@i.impM2 />\n"
                    + "In main: ${" + specVar + "}\n"
                    + "<#include 'inc.ftl'>"
                    + "In main: ${" + specVar + "}\n"
                    + "<@incM>${" + specVar + "}</@>\n"
                    + "<@incM2 />\n"
                    + "In main: ${" + specVar + "}\n"
                    );
            tl.putTemplate("imp.ftl",
                    "<#global inImp = " + specVar + ">"
                    + "<#macro impM>"
                        + "${" + specVar + "}\n"
                        + "{<#nested>}"
                    + "</#macro>"
                    + "<#macro impM2>"
                        + "In imp call imp:\n"
                        + "<@impM>${" + specVar + "}</@>\n"
                        + "After: ${" + specVar + "}"
                    + "</#macro>"
                    );
            tl.putTemplate("inc.ftl",
                    "In inc: ${" + specVar + "}\n"
                    + "In inc call imp:\n"
                    + "<@i.impM>${" + specVar + "}</@>\n"
                    + "<#macro incM>"
                        + "${" + specVar + "}\n"
                        + "{<#nested>}"
                    + "</#macro>"
                    + "<#macro incM2>"
                        + "In inc call imp:\n"
                        + "<@i.impM>${" + specVar + "}</@>"
                    + "</#macro>"
                    );
            return tl;
    }
    
    @Test
    public void testTemplateName230() throws IOException, TemplateException {
        getConfiguration().setTemplateLoader(createTemplateLoader(".templateName"));
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_0);
        assertMainFtlOutput(false);
    }
    
    /** This IcI version was buggy. */
    @Test
    public void testTemplateName2322() throws IOException, TemplateException {
        getConfiguration().setTemplateLoader(createTemplateLoader(".templateName"));
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_22);
        assertMainFtlOutput(true);
    }
    
    @Test
    public void testTemplateName2323() throws IOException, TemplateException {
        getConfiguration().setTemplateLoader(createTemplateLoader(".templateName"));
        getConfiguration().setIncompatibleImprovements(Configuration.VERSION_2_3_23);
        assertMainFtlOutput(false);
    }

    @Test
    public void testMainTemplateName() throws IOException, TemplateException {
        getConfiguration().setTemplateLoader(createTemplateLoader(".mainTemplateName"));
        for (Version ici : BREAK_POINT_VERSIONS) {
            getConfiguration().setIncompatibleImprovements(ici);
            assertMainFtlOutput(true);
        }
    }

    @Test
    public void testCurrentTemplateName() throws IOException, TemplateException {
        getConfiguration().setTemplateLoader(createTemplateLoader(".currentTemplateName"));
        for (Version ici : BREAK_POINT_VERSIONS) {
            getConfiguration().setIncompatibleImprovements(ici);
            assertOutputForNamed("main.ftl",
                    "In main: main.ftl\n"
                    + "In imp: imp.ftl\n"
                    + "In main: main.ftl\n"
                    + "imp.ftl\n"
                    + "{main.ftl}\n"
                    + "In imp call imp:\n"
                    + "imp.ftl\n"
                    + "{imp.ftl}\n"
                    + "After: imp.ftl\n"
                    + "In main: main.ftl\n"
                    + "In inc: inc.ftl\n"
                    + "In inc call imp:\n"
                    + "imp.ftl\n"
                    + "{inc.ftl}\n"
                    + "In main: main.ftl\n"
                    + "inc.ftl\n"
                    + "{main.ftl}\n"
                    + "In inc call imp:\n"
                    + "imp.ftl\n"
                    + "{inc.ftl}\n"
                    + "In main: main.ftl\n");
        }
    }
    
    protected void assertMainFtlOutput(boolean allMain) throws IOException, TemplateException {
        String expected
                = "In main: main.ftl\n"
                + "In imp: imp.ftl\n"
                + "In main: main.ftl\n"
                + "main.ftl\n"
                + "{main.ftl}\n"
                + "In imp call imp:\n"
                + "main.ftl\n"
                + "{imp.ftl}\n"
                + "After: main.ftl\n"
                + "In main: main.ftl\n"
                + "In inc: inc.ftl\n"
                + "In inc call imp:\n"
                + "inc.ftl\n"
                + "{main.ftl}\n"
                + "In main: main.ftl\n"
                + "main.ftl\n"
                + "{main.ftl}\n"
                + "In inc call imp:\n"
                + "main.ftl\n"
                + "{main.ftl}\n"
                + "In main: main.ftl\n";
        if (allMain) {
            expected = StringUtil.replace(expected, "imp.ftl", "main.ftl");
            expected = StringUtil.replace(expected, "inc.ftl", "main.ftl");
        }
        assertOutputForNamed("main.ftl", expected);
    }

    @Before
    public void setup() {
        Configuration cfg = getConfiguration();
        cfg.setWhitespaceStripping(false);
    }
    
}