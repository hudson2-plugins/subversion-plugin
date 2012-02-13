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

import antlr.ANTLRException;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequestSettings;
import com.gargoylesoftware.htmlunit.WebResponse;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.triggers.SCMTrigger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.Email;
import org.jvnet.hudson.test.HudsonHomeLoader.CopyExisting;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNStatus;

/**
 * @author Kohsuke Kawaguchi
 */
public class SubversionCommitTest extends AbstractSubversionTest {

    private static final int LOG_LIMIT = 1000;

    String kind = ISVNAuthenticationManager.PASSWORD;

    /**
     * {@link SubversionSCM#pollChanges(AbstractProject, hudson.Launcher, FilePath, TaskListener)} should notice
     * if the workspace and the current configuration is inconsistent and schedule a new build.
     */
    @Email("http://www.nabble.com/Proper-way-to-switch---relocate-SVN-tree---tt21173306.html")
    public void testPollingAfterRelocation() throws Exception {
        // fetch the current workspace
        FreeStyleProject p = createFreeStyleProject();
        p.setScm(loadSvnRepo());
        p.scheduleBuild2(0, new Cause.UserCause()).get();

        // as a baseline, this shouldn't detect any change
        TaskListener listener = createTaskListener();
        assertFalse(p.pollSCMChanges(listener));

        // now switch the repository to a new one.
        // this time the polling should indicate that we need a new build
        p.setScm(loadSvnRepo());
        assertTrue(p.pollSCMChanges(listener));

        // build it once again to switch
        p.scheduleBuild2(0, new Cause.UserCause()).get();

        // then no more change should be detected
        assertFalse(p.pollSCMChanges(listener));
    }

    public void testURLWithVariable() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        String url = "http://svn.codehaus.org/sxc/tags/sxc-0.5/sxc-core/src/test/java/com/envoisolutions/sxc/builder/";
        p.setScm(new SubversionSCM("$REPO" + url.substring(10)));

        String var = url.substring(0, 10);

        FreeStyleBuild b = p.scheduleBuild2(0, new Cause.UserCause(),
            new ParametersAction(new StringParameterValue("REPO", var))).get();
        System.out.println(b.getLog(LOG_LIMIT));
        assertBuildStatus(Result.SUCCESS, b);
        assertTrue(b.getWorkspace().child("Node.java").exists());
    }

    /**
     * Test that multiple repository URLs are all polled.
     */
    @Bug(3168)
    public void testPollMultipleRepositories() throws Exception {
        // fetch the current workspaces
        FreeStyleProject p = createFreeStyleProject();
        String svnBase = "file://" + new CopyExisting(getClass().getResource("/svn-repo.zip")).allocate()
            .toURI()
            .toURL()
            .getPath();
        SubversionSCM scm = new SubversionSCM(
            Arrays.asList(new SubversionSCM.ModuleLocation(svnBase + "trunk", null),
                new SubversionSCM.ModuleLocation(svnBase + "branches", null)),
            false, false, null, null, null, null, null);
        p.setScm(scm);
        p.scheduleBuild2(0, new Cause.UserCause()).get();

        // as a baseline, this shouldn't detect any change
        TaskListener listener = createTaskListener();
        assertFalse(p.pollSCMChanges(listener));

        createCommit(scm, "branches/foo");
        assertTrue("any change in any of the repository should be detected", p.pollSCMChanges(listener));
        assertFalse("no change since the last polling", p.pollSCMChanges(listener));
        createCommit(scm, "trunk/foo");
        assertTrue("another change in the repo should be detected separately", p.pollSCMChanges(listener));
    }

    /**
     * Makes sure that Subversion doesn't check out workspace in 1.6
     */
    @Email("http://www.nabble.com/SVN-1.6-td24081571.html")
    public void testWorkspaceVersion() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.setScm(loadSvnRepo());
        FreeStyleBuild b = p.scheduleBuild2(0).get();

        SVNClientManager wc = SubversionSCM.createSvnClientManager((AbstractProject) null);
        SVNStatus st = wc.getStatusClient().doStatus(new File(b.getWorkspace().getRemote() + "/a"), false);
        int wcf = st.getWorkingCopyFormat();
        System.out.println(wcf);
        assertEquals(SVNAdminAreaFactory.WC_FORMAT_14, wcf);
    }

    private static String readFileAsString(File file)
        throws java.io.IOException {
        BufferedReader reader = null;
        StringBuilder fileData = new StringBuilder(1000);
        try {
            reader = new BufferedReader(new FileReader(file));
            char[] buf = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(buf)) != -1) {
                fileData.append(buf, 0, numRead);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return fileData.toString();
    }

    /**
     * Makes sure the symbolic link is checked out correctly. There seems to be
     * @throws IOException 
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    @Bug(3904)
    //TODO fix when svn repository for the tests will be created
    public void ignore_testSymbolicLinkCheckout() throws IOException, InterruptedException, ExecutionException {
        // Only perform if symlink behavior is enabled
        if (!"true".equals(System.getProperty("svnkit.symlinks"))) {
            return;
        }

        FreeStyleProject p = createFreeStyleProject();
        p.setScm(new SubversionSCM("https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/issue-3904"));

        FreeStyleBuild b = p.scheduleBuild2(0, new Cause.UserCause()).get();
        File source = new File(b.getWorkspace().getRemote() + "/readme.txt");
        File linked = new File(b.getWorkspace().getRemote() + "/linked.txt");
        assertEquals("Files '" + source + "' and '" + linked + "' are not identical from user view.",
            readFileAsString(source), readFileAsString(linked));
    }

    public void testExcludeByUser() throws InterruptedException, ExecutionException, IOException {
        FreeStyleProject p = createFreeStyleProject("testExcludeByUser");
        p.setScm(new SubversionSCM(
            Arrays.asList(new SubversionSCM.ModuleLocation(
                "https://datacard.googlecode.com/svn/trunk@145", null)),
            true, null, "", "bg_one@mail.ru", "", "")
        );
        // Do a build to force the creation of the workspace. This works around
        // pollChanges returning true when the workspace does not exist.
        p.scheduleBuild2(0).get();

        boolean foundChanges = p.pollSCMChanges(createTaskListener());
        assertFalse("Polling found changes that should have been ignored", foundChanges);
    }

    /**
     * Test excluded regions
     */
    @Bug(6030)
    public void testExcludedRegions() throws Exception {
//        SLAVE_DEBUG_PORT = 8001;
        File repo = new CopyExisting(getClass().getResource("HUDSON-6030.zip")).allocate();
        SubversionSCM scm = new SubversionSCM(
            SubversionSCM.ModuleLocation.parse(new String[]{"file://" + repo.getPath()},
                new String[]{"."}, null, null),
            true, false, null, ".*//*bar", "", "", "", "");

        FreeStyleProject p = createFreeStyleProject("testExcludedRegions");
        p.setScm(scm);
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());

        // initial polling on the slave for the code path that doesn't find any change
        assertFalse(p.pollSCMChanges(createTaskListener()));

        createCommit(scm, "bar");

        // polling on the slave for the code path that does have a change but should be excluded.
        assertFalse("Polling found changes that should have been ignored",
            p.pollSCMChanges(createTaskListener()));

        createCommit(scm, "foo");

        // polling on the slave for the code path that doesn't find any change
        assertTrue("Polling didn't find a change it should have found.",
            p.pollSCMChanges(createTaskListener()));

    }

    /**
     * Test included regions
     */
    @Bug(6030)
    public void testIncludedRegions() throws Exception {
//        SLAVE_DEBUG_PORT = 8001;
        File repo = new CopyExisting(getClass().getResource("HUDSON-6030.zip")).allocate();
        SubversionSCM scm = new SubversionSCM(
            SubversionSCM.ModuleLocation.parse(new String[]{"file://" + repo.getPath()},
                new String[]{"."}, null, null),
            true, false, null, "", "", "", "", ".*//*foo");

        FreeStyleProject p = createFreeStyleProject("testExcludedRegions");
        p.setScm(scm);
        assertBuildStatusSuccess(p.scheduleBuild2(0).get());

        // initial polling on the slave for the code path that doesn't find any change
        assertFalse(p.pollSCMChanges(createTaskListener()));

        createCommit(scm, "bar");

        // polling on the slave for the code path that does have a change but should be excluded.
        assertFalse("Polling found changes that should have been ignored",
            p.pollSCMChanges(createTaskListener()));

        createCommit(scm, "foo");

        // polling on the slave for the code path that doesn't find any change
        assertTrue("Polling didn't find a change it should have found.",
            p.pollSCMChanges(createTaskListener()));

    }

    /**
     * Tests a checkout triggered from the post-commit hook
     */
    //TODO fix when svn repository for the tests will be created
    public void ignore_testPostCommitTrigger() throws Exception {
        FreeStyleProject p = createPostCommitTriggerJob();
        FreeStyleBuild b = sendCommitTrigger(p, true);

        assertTrue(getActualRevision(b, "https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/trivial-ant")
            <= 13000);
    }

    /**
     * Tests a checkout triggered from the post-commit hook without revision
     * information.
     */
    //TODO fix when svn repository for the tests will be created
    public void ignore_testPostCommitTriggerNoRevision() throws Exception {
        FreeStyleProject p = createPostCommitTriggerJob();
        FreeStyleBuild b = sendCommitTrigger(p, false);

        assertTrue(
            getActualRevision(b, "https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/trivial-ant") > 13000);
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

    /**
     * Loads a test Subversion repository into a temporary directory, and creates {@link SubversionSCM} for it.
     */
    private SubversionSCM loadSvnRepo() throws Exception {
        return new SubversionSCM(
            "file://" + new CopyExisting(getClass().getResource("/svn-repo.zip")).allocate().toURI().toURL().getPath()
                + "trunk/a", "a");
    }

    private FreeStyleBuild sendCommitTrigger(FreeStyleProject p, boolean includeRevision) throws Exception {
        String repoUUID = "71c3de6d-444a-0410-be80-ed276b4c234a";

        WebClient wc = new WebClient();
        WebRequestSettings wr = new WebRequestSettings(new URL(getURL() + "subversion/" + repoUUID + "/notifyCommit"),
            HttpMethod.POST);
        wr.setRequestBody("A   trunk/hudson/test-projects/trivial-ant/build.xml");
        wr.setAdditionalHeader("Content-Type", "text/plain;charset=UTF-8");

        if (includeRevision) {
            wr.setAdditionalHeader("X-Hudson-Subversion-Revision", "13000");
        }

        WebConnection conn = wc.getWebConnection();
        WebResponse resp = conn.getResponse(wr);
        assertTrue(isGoodHttpStatus(resp.getStatusCode()));

        waitUntilNoActivity();
        FreeStyleBuild b = p.getLastBuild();
        assertNotNull(b);
        assertBuildStatus(Result.SUCCESS, b);

        return b;
    }

    /**
     * Manufactures commits by adding files in the given names.
     */
    private void createCommit(SubversionSCM scm, String... paths) throws Exception {
        FreeStyleProject forCommit = createFreeStyleProject();
        forCommit.setScm(scm);
        forCommit.setAssignedLabel(hudson.getSelfLabel());
        FreeStyleBuild b = assertBuildStatusSuccess(forCommit.scheduleBuild2(0).get());
        SVNClientManager svnm = SubversionSCM.createSvnClientManager((AbstractProject) null);

        List<File> added = new ArrayList<File>();
        for (String path : paths) {
            FilePath newFile = b.getWorkspace().child(path);
            added.add(new File(newFile.getRemote()));
            if (!newFile.exists()) {
                newFile.touch(System.currentTimeMillis());
                svnm.getWCClient()
                    .doAdd(new File(newFile.getRemote()), false, false, false, SVNDepth.INFINITY, false, false);
            } else {
                newFile.write("random content", "UTF-8");
            }
        }
        SVNCommitClient cc = svnm.getCommitClient();
        cc.doCommit(added.toArray(new File[added.size()]), false, "added", null, null, false, false, SVNDepth.EMPTY);
    }

    private Long getActualRevision(FreeStyleBuild b, String url) throws Exception {
        SVNRevisionState revisionState = b.getAction(SVNRevisionState.class);
        if (revisionState == null) {
            throw new Exception("No revision found!");
        }

        return revisionState.revisions.get(url).longValue();

    }


    private FreeStyleProject createPostCommitTriggerJob() throws IOException, ANTLRException {
        // Disable crumbs because HTMLUnit refuses to mix request bodies with
        // request parameters
        hudson.setCrumbIssuer(null);

        FreeStyleProject p = createFreeStyleProject();
        String url = "https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/trivial-ant";
        SCMTrigger trigger = new SCMTrigger("0 */6 * * *");

        p.setScm(new SubversionSCM(url));
        p.addTrigger(trigger);
        trigger.start(p, true);

        return p;
    }
}
