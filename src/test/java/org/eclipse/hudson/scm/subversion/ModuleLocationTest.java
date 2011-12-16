/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi, Bruce Chapman, Yahoo! Inc., Anton Kozak
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import hudson.model.FreeStyleProject;
import java.net.URL;
import java.util.Arrays;
import org.eclipse.hudson.scm.subversion.browsers.Sventon;

/**
 * @author Kohsuke Kawaguchi
 */
public class ModuleLocationTest extends AbstractSubversionTest {
    //TODO replace with unit tests and mocks
    public void testModuleLocationWithDepthIgnoreExternalsOption() throws Exception {
        FreeStyleProject project= createFreeStyleProject("test0");

        SubversionSCM scm = new SubversionSCM(
                Arrays.asList(
                        new SubversionSCM.ModuleLocation("http://svn.apache.org/repos/asf/subversion/trunk/doc", "c", "infinity", true),
                        new SubversionSCM.ModuleLocation("http://svn.apache.org/repos/asf/subversion/trunk/doc", "d", "files", false)),
                new CheckoutUpdater(), null, null, null, null, null, null);
        project.setScm(scm);
        submit(createWebClient().getPage(project, "configure").getFormByName("config"));
        verify(scm, (SubversionSCM) project.getScm());
    }

}
