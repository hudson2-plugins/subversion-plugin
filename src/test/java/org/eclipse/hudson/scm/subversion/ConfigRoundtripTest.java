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
import org.eclipse.hudson.scm.subversion.browsers.Sventon;

import java.net.URL;
import java.util.Arrays;

/**
 * @author Kohsuke Kawaguchi
 */
public class ConfigRoundtripTest extends AbstractSubversionTest {

    public void testModuleLocationWithDepthIgnoreExternalsOption() throws Exception {
        FreeStyleProject project= createFreeStyleProject();

        SubversionSCM scm = new SubversionSCM(
                Arrays.asList(
                        new SubversionSCM.ModuleLocation("http://svn.apache.org/repos/asf/subversion/trunk/doc", "c", "infinity", true),
                        new SubversionSCM.ModuleLocation("http://svn.apache.org/repos/asf/subversion/trunk/doc", "d", "files", false)),
                new CheckoutUpdater(), null, null, null, null, null, null);
        project.setScm(scm);
        submit(createWebClient().getPage(project, "configure").getFormByName("config"));
        verify(scm, (SubversionSCM) project.getScm());
    }


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

    private void verify(SubversionSCM lhs, SubversionSCM rhs) {
        SubversionSCM.ModuleLocation[] ll = lhs.getLocations();
        SubversionSCM.ModuleLocation[] rl = rhs.getLocations();
        assertEquals(ll.length, rl.length);
        for (int i = 0; i < ll.length; i++) {
            assertEquals(ll[i].local, rl[i].local);
            assertEquals(ll[i].remote, rl[i].remote);
            assertEquals(ll[i].getDepthOption(), rl[i].getDepthOption());
            assertEquals(ll[i].isIgnoreExternalsOption(), rl[i].isIgnoreExternalsOption());
        }

        assertNullEquals(lhs.getExcludedRegions(), rhs.getExcludedRegions());
        assertNullEquals(lhs.getExcludedUsers(), rhs.getExcludedUsers());
        assertNullEquals(lhs.getExcludedRevprop(), rhs.getExcludedRevprop());
        assertNullEquals(lhs.getExcludedCommitMessages(), rhs.getExcludedCommitMessages());
        assertNullEquals(lhs.getIncludedRegions(), rhs.getIncludedRegions());
    }

    private void assertNullEquals(String left, String right) {
        if (left == null) {
            left = "";
        }
        if (right == null) {
            right = "";
        }
        assertEquals(left, right);
    }

}
