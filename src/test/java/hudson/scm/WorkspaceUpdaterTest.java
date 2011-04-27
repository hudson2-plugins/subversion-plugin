/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Bruce Chapman, Yahoo! Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.scm;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.scm.subversion.CheckoutUpdater;
import hudson.scm.subversion.UpdateUpdater;
import hudson.scm.subversion.UpdateWithCleanUpdater;
import hudson.scm.subversion.UpdateWithRevertUpdater;
import hudson.scm.subversion.WorkspaceUpdater;
import hudson.util.StreamTaskListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.TestBuilder;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;


/**
 * @author Kohsuke Kawaguchi
 */
public class WorkspaceUpdaterTest extends AbstractSubversionTest {

    String kind = ISVNAuthenticationManager.PASSWORD;

    /**
     * Ensures that the introduction of {@link hudson.scm.subversion.WorkspaceUpdater} maintains backward compatibility with
     * existing data.
     */
    public void testWorkspaceUpdaterCompatibility() throws Exception {
        Proc p = runSvnServe(getClass().getResource("small.zip"));
        try {
            verifyCompatibility("legacy-update.xml", UpdateUpdater.class);
            verifyCompatibility("legacy-checkout.xml", CheckoutUpdater.class);
            verifyCompatibility("legacy-revert.xml", UpdateWithRevertUpdater.class);
        } finally {
            p.kill();
        }
    }

    public void testUpdateWithCleanUpdater() throws Exception {
        // this contains an empty "a" file and svn:ignore that ignores b
        Proc srv = runSvnServe(getClass().getResource("clean-update-test.zip"));
        try {
            FreeStyleProject p = createFreeStyleProject();
            SubversionSCM scm = new SubversionSCM("svn://localhost/");
            scm.setWorkspaceUpdater(new UpdateWithCleanUpdater());
            p.setScm(scm);

            p.getBuildersList().add(new TestBuilder() {
                @Override
                public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                    FilePath ws = build.getWorkspace();
                    // create two files
                    ws.child("b").touch(0);
                    ws.child("c").touch(0);
                    return true;
                }
            });
            FreeStyleBuild b = buildAndAssertSuccess(p);

            // this should have created b and c
            FilePath ws = b.getWorkspace();
            assertTrue(ws.child("b").exists());
            assertTrue(ws.child("c").exists());

            // now, remove the builder that makes the workspace dirty and rebuild
            p.getBuildersList().clear();
            b = buildAndAssertSuccess(p);
            System.out.println(b.getLog());

            // those files should have been cleaned
            ws = b.getWorkspace();
            assertFalse(ws.child("b").exists());
            assertFalse(ws.child("c").exists());
        } finally {
            srv.kill();
        }
    }

    /**
     * Used for experimenting the memory leak problem.
     * This test by itself doesn't detect that, but I'm leaving it in anyway.
     */
    @Bug(8061)
    public void testPollingLeak() throws Exception {
        Proc p = runSvnServe(getClass().getResource("small.zip"));
        try {
            FreeStyleProject b = createFreeStyleProject();
            b.setScm(new SubversionSCM("svn://localhost/"));
            b.setAssignedNode(createSlave());

            assertBuildStatusSuccess(b.scheduleBuild2(0));

            b.poll(new StreamTaskListener(System.out, Charset.defaultCharset()));
        } finally {
            p.kill();
        }
    }

    /**
     * Subversion externals to a file. Requires 1.6 workspace.
     */
    @Bug(7539)
    public void testExternalsToFile() throws Exception {
        Proc server = runSvnServe(getClass().getResource("HUDSON-7539.zip"));
        try {
            // enable 1.6 mode
            HtmlForm f = createWebClient().goTo("configure").getFormByName("config");
            f.getSelectByName("svn.workspaceFormat").setSelectedAttribute("10", true);
            submit(f);

            FreeStyleProject p = createFreeStyleProject();
            p.setScm(new SubversionSCM("svn://localhost/dir1"));
            FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0));
            System.out.println(getLog(b));

            assertTrue(b.getWorkspace().child("2").exists());
            assertTrue(b.getWorkspace().child("3").exists());
            assertTrue(b.getWorkspace().child("test.x").exists());

            assertBuildStatusSuccess(p.scheduleBuild2(0));
        } finally {
            server.kill();
        }
    }

    private void verifyCompatibility(String resourceName, Class<? extends WorkspaceUpdater> expected)
        throws IOException {
        InputStream io = null;
        AbstractProject job = null;
        try {
            io = getClass().getResourceAsStream(resourceName);
            job = (AbstractProject) hudson.createProjectFromXML("update", io);
        } finally {
            IOUtils.closeQuietly(io);
        }
        assertEquals(expected, ((SubversionSCM) job.getScm()).getWorkspaceUpdater().getClass());
    }
}
