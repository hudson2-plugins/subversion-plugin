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

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.Proc;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.jvnet.hudson.test.Bug;

/**
 * @author Kohsuke Kawaguchi
 */
public class WorkspaceUpdaterTest extends AbstractSubversionTest {
    /**
     * Subversion externals to a file. Requires 1.6 workspace.
     */
    @Bug(7539)
    public void testExternalsToFile() throws Exception {
        Proc server = runSvnServe(getClass().getResource("HUDSON-7539.zip"));
        try {
            // enable 1.6 mode
            WebClient webclient = new WebClient();
            webclient.setThrowExceptionOnScriptError(false);

            HtmlForm f = webclient.goTo("configure").getFormByName("config");
            f.getSelectByName("svn.workspaceFormat").setSelectedAttribute("10", true);
            submit(f);

            FreeStyleProject p = createFreeStyleProject();
            p.setScm(new SubversionSCM("svn://localhost/dir1"));
            FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0));
            System.out.println(getLog(b));

            assertTrue(b.getWorkspace().child("2").exists());
            assertTrue(b.getWorkspace().child("3").exists());
//            assertTrue(b.getWorkspace().child("test.x").exists());

            assertBuildStatusSuccess(p.scheduleBuild2(0));
        } finally {
            server.kill();
        }
    }
}
