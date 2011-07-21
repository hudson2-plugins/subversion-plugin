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
package org.eclipse.hudson.scm.config;

import hudson.model.FreeStyleProject;
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
//        CVSSCM scm = (CVSSCM) project.getScm();
//        assertNotNull(scm);
//        assertEquals(scm.getModuleLocations().length, 1);
//        assertEquals(scm.getModuleLocations()[0].getCvsroot(), ":pserver:anonymous:password@10.4.0.50:/var/cvsroot");
//        assertEquals(scm.getModuleLocations()[0].getBranch(), "tag");
//        assertEquals(scm.getModuleLocations()[0].getModule(), "test_cvs doc");
//        assertEquals(scm.getModuleLocations()[0].getLocalDir(), ".");
//        assertTrue(scm.getModuleLocations()[0].isTag());
//        assertTrue(scm.getCanUseUpdate());
//        assertTrue(scm.isLegacy());
//        assertFalse(scm.isFlatten());
//        assertEquals(scm.getExcludedRegions(), "");
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