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

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.FilePath;
import hudson.Proc;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.scm.browsers.Sventon;
import hudson.scm.credential.SVNSSLAuthentication;
import hudson.scm.subversion.UpdateUpdater;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.acegisecurity.context.SecurityContextHolder;
import org.dom4j.Document;
import org.dom4j.io.DOMReader;
import org.junit.Ignore;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.HudsonHomeLoader.CopyExisting;
import org.jvnet.hudson.test.recipes.PresetData;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;

import static hudson.scm.SubversionSCM.compareSVNAuthentications;

/**
 * @author Kohsuke Kawaguchi
 */
public class SubversionCommonTest extends AbstractSubversionTest {

    private static final String GUEST_ACCESS_REPOSITORY_RESOURCE = "guest_access_svn.zip";
    private static final String realm = "<svn://localhost:3690>";
    private static final String SVN_URL = "svn://localhost/bob";
    private static final String GUEST_USER_LOGIN = "guest";
    private static final String GUEST_USER_PASSWORD = "guestpass";
    private static final String BOGUS_USER_LOGIN = "bogus";
    private static final String BOGUS_USER_PASSWORD = "boguspass";
    private static final Integer LOG_LIMIT = 1000;
    protected static final String SVN_URL1 = "http://svn.apache.org/repos/asf/subversion/trunk/doc";
    protected static final String SVN_URL2 = "http://svn.apache.org/repos/asf/subversion/trunk/packages";

    public void testMatcher() {
        check("http://foobar/");
        check("https://foobar/");
        check("file://foobar/");
        check("svn://foobar/");
        check("svn+ssh://foobar/");
    }

    public void testMacther2() {
        String[] r = "abc\\ def ghi".split("(?<!\\\\)[ \\r\\n]+");
        for (int i = 0; i < r.length; i++) {
            r[i] = r[i].replaceAll("\\\\ ", " ");
        }
        System.out.println(Arrays.asList(r));
        assertEquals(r.length, 2);
    }

    private void check(String url) {
        assertTrue(SubversionSCM.URL_PATTERN.matcher(url).matches());
    }

    //TODO Investigate why System user is used instead of anonymous after migration to 2.0.0 version
    @PresetData(PresetData.DataSet.ANONYMOUS_READONLY)
    @Bug(2380)
    public void testTaggingPermission() throws Exception {
        // create a build
        FreeStyleProject p = createFreeStyleProject();
        //Set anonymous user for authentication.
        SecurityContextHolder.getContext().setAuthentication(Hudson.ANONYMOUS);
        p.setScm(loadSvnRepo());
        FreeStyleBuild b = p.scheduleBuild2(0, new Cause.UserCause()).get();
        System.out.println(b.getLog(LOG_LIMIT.intValue()));
        assertBuildStatus(Result.SUCCESS, b);

        SubversionTagAction action = b.getAction(SubversionTagAction.class);
        assertFalse(b.hasPermission(action.getPermission()));

        WebClient wc = new WebClient();
        HtmlPage html = wc.getPage(b);

        // make sure there's no link to the 'tag this build'
        Document dom = new DOMReader().read(html);
        assertNull(dom.selectSingleNode("//A[text()='Tag this build']"));
        for (HtmlAnchor a : html.getAnchors()) {
            assertFalse(a.getHrefAttribute().contains("/tagBuild/"));
        }

        // and no tag form on tagBuild page
        html = wc.getPage(b, "tagBuild/");
        try {
            html.getFormByName("tag");
            fail("should not have been found");
        } catch (ElementNotFoundException e) {
        }

        // and that tagging would fail
        try {
            wc.getPage(b, "tagBuild/submit?name0=test&Submit=Tag");
            fail("should have been denied");
        } catch (FailingHttpStatusCodeException e) {
            // make sure the request is denied
            assertEquals(e.getResponse().getStatusCode(), 403);
        }

        // now login as alice and make sure that the tagging would succeed
        wc = new WebClient();
        wc.login("alice", "alice");
        html = wc.getPage(b, "tagBuild/");
        HtmlForm form = html.getFormByName("tag");
        submit(form);
    }

    public void testConfigRoundtrip() throws Exception {
        FreeStyleProject p = createFreeStyleProject();

        SubversionSCM scm = new SubversionSCM(
            Arrays.asList(
                new SubversionSCM.ModuleLocation(
                    "https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/testSubversionExclusion", "c"),
                new SubversionSCM.ModuleLocation(
                    "https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/testSubversionExclusion", "d")),
            true, new Sventon(new URL("http://www.sun.com/"), "test"), "exclude", "user", "revprop", "excludeMessage");
        p.setScm(scm);
        submit(new WebClient().getPage(p, "configure").getFormByName("config"));
        verify(scm, (SubversionSCM) p.getScm());

        scm = new SubversionSCM(
            Arrays.asList(
                new SubversionSCM.ModuleLocation(
                    "https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/testSubversionExclusion", "c")),
            false, null, "", "", "", "");
        p.setScm(scm);
        submit(new WebClient().getPage(p, "configure").getFormByName("config"));
        verify(scm, (SubversionSCM) p.getScm());
    }

    @Bug(7944)
    @Ignore
    public void testConfigRoundtrip2() throws IOException {
        FreeStyleProject p = createFreeStyleProject();

        SubversionSCM scm = new SubversionSCM(
            Arrays.asList(
                new SubversionSCM.ModuleLocation(
                    "https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/testSubversionExclusion", "")),
            true, null, null, null, null, null);
        p.setScm(scm);
//        configRoundtrip(p);
        verify(scm, (SubversionSCM) p.getScm());
    }

    public void testMasterPolling() throws Exception {
        File repo = new CopyExisting(getClass().getResource("two-revisions.zip")).allocate();
        SubversionSCM scm = new SubversionSCM("file:///" + repo.getPath());
        SubversionSCM.POLL_FROM_MASTER = true;

        FreeStyleProject p = createFreeStyleProject();
        p.setScm(scm);
        p.setAssignedLabel(createSlave().getSelfLabel());
        assertBuildStatusSuccess(p.scheduleBuild2(2).get());

        // initial polling on the master for the code path that doesn't find any change
        assertFalse(p.poll(new StreamTaskListener(System.out, null)).hasChanges());

        // create a commit
        FreeStyleProject forCommit = createFreeStyleProject();
        forCommit.setScm(scm);
        forCommit.setAssignedLabel(hudson.getSelfLabel());
        FreeStyleBuild b = assertBuildStatusSuccess(forCommit.scheduleBuild2(0).get());
        FilePath newFile = b.getWorkspace().child("foo");
        newFile.touch(System.currentTimeMillis());
        SVNClientManager svnm = SubversionSCM.createSvnClientManager(p);
        svnm.getWCClient().doAdd(new File(newFile.getRemote()), false, false, false, SVNDepth.INFINITY, false, false);
        SVNCommitClient cc = svnm.getCommitClient();
        cc.doCommit(new File[]{new File(newFile.getRemote())}, false, "added", false, false);

        // polling on the master for the code path that doesn't find any change
        assertTrue(p.poll(new StreamTaskListener(System.out, null)).hasChanges());
    }


    public void testCompareSVNAuthentications() {
        assertFalse(compareSVNAuthentications(new SVNUserNameAuthentication("me", true, null, false),
            new SVNSSHAuthentication("me", "me", 22, true, null, false)));
        // same object should compare equal
        _idem(new SVNUserNameAuthentication("me", true, null, false));
        _idem(new SVNSSHAuthentication("me", "pass", 22, true, null, false));
        _idem(new SVNSSHAuthentication("me", new File("./some.key"), null, 23, false, null, false));
        _idem(new SVNSSHAuthentication("me", "key".toCharArray(), "phrase", 0, false, null, false));
        _idem(new SVNPasswordAuthentication("me", "pass", true, null, false));
        _idem(new SVNSSLAuthentication("certificate".getBytes(), null, true));

        // make sure two Files and char[]s compare the same
        assertTrue(compareSVNAuthentications(
            new SVNSSHAuthentication("me", new File("./some.key"), null, 23, false, null, false),
            new SVNSSHAuthentication("me", new File("./some.key"), null, 23, false, null, false)));
        assertTrue(compareSVNAuthentications(
            new SVNSSHAuthentication("me", "key".toCharArray(), "phrase", 0, false, null, false),
            new SVNSSHAuthentication("me", "key".toCharArray(), "phrase", 0, false, null, false)));

        // negative cases
        assertFalse(compareSVNAuthentications(
            new SVNSSHAuthentication("me", new File("./some1.key"), null, 23, false, null, false),
            new SVNSSHAuthentication("me", new File("./some2.key"), null, 23, false, null, false)));
        assertFalse(compareSVNAuthentications(
            new SVNSSHAuthentication("me", "key".toCharArray(), "phrase", 0, false, null, false),
            new SVNSSHAuthentication("yo", "key".toCharArray(), "phrase", 0, false, null, false)));

    }

    /**
     * Make sure that a failed credential doesn't result in an infinite loop
     */
    @Bug(2909)
    public void testInfiniteLoop() throws Exception {
        //Start local svn repository
        Proc server = runSvnServe(getClass().getResource(GUEST_ACCESS_REPOSITORY_RESOURCE));
        SVNURL repo = SVNURL.parseURIDecoded(SVN_URL);
        try {
            // creates a purely in memory auth manager
            ISVNAuthenticationManager m = createInMemoryManager();

            // double check that it really knows nothing about the fake repo
            try {
                m.getFirstAuthentication(kind, realm, repo);
                fail();
            } catch (SVNCancelException e) {
                // yep
            }
            SVNPasswordAuthentication authentication = new SVNPasswordAuthentication(BOGUS_USER_LOGIN, BOGUS_USER_PASSWORD, true, null, false);
            m.acknowledgeAuthentication(false, kind, realm, null, authentication);

            authentication = new SVNPasswordAuthentication(GUEST_USER_LOGIN, GUEST_USER_PASSWORD, true, null, false);
            m.acknowledgeAuthentication(true, kind, realm, null, authentication);

            // emulate the call flow where the credential fails
            List<SVNAuthentication> attempted = new ArrayList<SVNAuthentication>();
            SVNAuthentication a = m.getFirstAuthentication(kind, realm, repo);
            assertNotNull(a);
            attempted.add(a);
            for (int i = 0; i < 10; i++) {
                m.acknowledgeAuthentication(false, kind, realm, SVNErrorMessage.create(SVNErrorCode.RA_NOT_AUTHORIZED),
                    a);
                try {
                    a = m.getNextAuthentication(kind, realm, repo);
                    assertNotNull(a);
                    attempted.add(a);
                } catch (SVNCancelException e) {
                    // make sure we've tried our fake credential
                    for (SVNAuthentication aa : attempted) {
                        if (aa instanceof SVNPasswordAuthentication) {
                            SVNPasswordAuthentication pa = (SVNPasswordAuthentication) aa;
                            if (GUEST_USER_LOGIN.equals(pa.getUserName())
                                && GUEST_USER_PASSWORD.equals(pa.getPassword())) {
                                return; // yep
                            }
                        }
                    }
                    fail("Hudson didn't try authentication");
                }
            }
            fail("Looks like we went into an infinite loop");
        } finally {
            server.kill();
        }
    }


    public void testMultiModuleEnvironmentVariables() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        SubversionSCM.ModuleLocation[] locations = {
            new SubversionSCM.ModuleLocation(SVN_URL1, null),
            new SubversionSCM.ModuleLocation(SVN_URL2, null)
        };
        p.setScm(new SubversionSCM(Arrays.asList(locations), new UpdateUpdater(), null, null, null, null, null, null));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(builder);

        assertBuildStatusSuccess(p.scheduleBuild2(0).get());

        assertEquals(SVN_URL1, builder.getEnvVars().get("SVN_URL_1"));
        assertEquals(SVN_URL2, builder.getEnvVars().get("SVN_URL_2"));
        assertEquals(getActualRevision(p.getLastBuild(),SVN_URL1).toString(),
            builder.getEnvVars().get("SVN_REVISION_1"));
        assertEquals(getActualRevision(p.getLastBuild(), SVN_URL2).toString(),
            builder.getEnvVars().get("SVN_REVISION_2"));

    }

    public void testSingleModuleEnvironmentVariables() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.setScm(new SubversionSCM(SVN_URL1));

        CaptureEnvironmentBuilder builder = new CaptureEnvironmentBuilder();
        p.getBuildersList().add(builder);

        assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        assertEquals(SVN_URL1, builder.getEnvVars().get("SVN_URL"));
        assertEquals(getActualRevision(p.getLastBuild(), SVN_URL1).toString(),
            builder.getEnvVars().get("SVN_REVISION"));
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

    /**
     * Loads a test Subversion repository into a temporary directory, and creates {@link hudson.scm.SubversionSCM} for it.
     */
    private SubversionSCM loadSvnRepo() throws Exception {
        return new SubversionSCM(
            "file://" + new CopyExisting(getClass().getResource("/svn-repo.zip")).allocate().toURI().toURL().getPath()
                + "trunk/a", "a");
    }

    private Long getActualRevision(FreeStyleBuild b, String url) throws Exception {
        SVNRevisionState revisionState = b.getAction(SVNRevisionState.class);
        if (revisionState == null) {
            throw new Exception("No revision found!");
        }

        return revisionState.revisions.get(url);

    }

    private void _idem(SVNAuthentication a) {
        assertTrue(compareSVNAuthentications(a, a));
    }

}
