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
 * Kohsuke Kawaguchi, Bruce Chapman, Yahoo! Inc., Anton Kozak, Nikita Levyankov
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
public class ConfigRoundtripTest extends AbstractSubversionTest {

    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject project= createFreeStyleProject();
        SubversionSCM scm = new SubversionSCM(
                Arrays.asList(
                        new SubversionSCM.ModuleLocation(
                                "http://svn.apache.org/repos/asf/subversion/trunk/doc", "c", "infinity", true),
                        new SubversionSCM.ModuleLocation(
                                "http://svn.apache.org/repos/asf/subversion/trunk/doc", "d", "files", false)),
                new CheckoutUpdater(), new Sventon(new URL("http://www.sun.com/"), "test"), "excludedRegions", "excludedUsers",
                "excludedRevprop", "excludedCommitMessages", "includedRegions");
        project.setScm(scm);
        submit(createWebClient().getPage(project, "configure").getFormByName("config"));
        verify(scm, (SubversionSCM) project.getScm());

        scm = new SubversionSCM(
                Arrays.asList(
                        new SubversionSCM.ModuleLocation(
                                "http://svn.apache.org/repos/asf/subversion/trunk/doc", "c")),
                false, null, "", "", "", "");
        project.setScm(scm);
        submit(createWebClient().getPage(project, "configure").getFormByName("config"));
        verify(scm, (SubversionSCM) project.getScm());

        scm = new SubversionSCM(
                Arrays.asList(
                        new SubversionSCM.ModuleLocation(
                                "http://svn.apache.org/repos/asf/subversion/trunk/doc", "")),
                true, null, null, null, null, null);
        project.setScm(scm);
        submit(createWebClient().getPage(project, "configure").getFormByName("config"));
        verify(scm, (SubversionSCM) project.getScm());
    }
}
