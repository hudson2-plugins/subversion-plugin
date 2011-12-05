/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Anton Kozak
 *
 *******************************************************************************/
package hudson.model;

import org.apache.commons.lang.StringUtils;
import org.eclipse.hudson.scm.subversion.SubversionSCM;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test to verify backward compatibility with Hudson 2.1.0 configuration
 */
public class Hudson210ConfigTest extends BaseLegacyConverterTest {

    @Override
    protected String getResourceName() {
        return "config-210.xml";
    }

    @Test
    public void testLegacyUnmarshall() throws Exception {
        FreeStyleProject project = (FreeStyleProject) getSourceConfigFile(XSTREAM).read();
        project.setAllowSave(false);
        project.initProjectProperties();
        project.convertScmProperty();
        SubversionSCM scm = (SubversionSCM) project.getScm();
        assertNotNull(scm);
        assertEquals(scm.getLocations().length, 1);
        assertEquals(scm.getLocations()[0].getURL(), "http://10.4.0.50/repos/test3");
        assertEquals(scm.getLocations()[0].getLocalDir(), ".");
        assertEquals(scm.getLocations()[0].getDepthOption(), "infinity");
        assertFalse(scm.getLocations()[0].isIgnoreExternalsOption());
        assertTrue(StringUtils.isEmpty(scm.getExcludedRegions()));
        assertTrue(StringUtils.isEmpty(scm.getIncludedRegions()));
        assertTrue(StringUtils.isEmpty(scm.getExcludedUsers()));
        assertTrue(StringUtils.isEmpty(scm.getExcludedRevprop()));
        assertTrue(StringUtils.isEmpty(scm.getExcludedCommitMessages()));
        assertEquals(scm.getWorkspaceUpdater().getClass().getName(), "org.eclipse.hudson.scm.subversion.UpdateUpdater");
    }

    @Test
    public void testMarshall() throws Exception {
        //read object from config
        Object item = getSourceConfigFile(XSTREAM).read();
        //save to new config file
        getTargetConfigFile(XSTREAM).write(item);
        getTargetConfigFile(XSTREAM).read();
    }
}