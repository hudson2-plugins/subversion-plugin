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

import org.springframework.security.context.SecurityContextHolder;
import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequestSettings;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.FilePath;
import hudson.Proc;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Result;
import org.eclipse.hudson.scm.subversion.browsers.Sventon;
import hudson.triggers.SCMTrigger;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.dom4j.Document;
import org.dom4j.io.DOMReader;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.jvnet.hudson.test.HudsonHomeLoader.CopyExisting;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSLAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;

import static org.eclipse.hudson.scm.subversion.SubversionSCM.compareSVNAuthentications;

/**
 * @author Kohsuke Kawaguchi
 */
public class SubversionCommonTest extends AbstractSubversionTest {

    private static final Integer LOG_LIMIT = 1000;

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
    //TODO fix me
    @Bug(2380)
    public void ignore_testTaggingPermission() throws Exception {
        File repo = new CopyExisting(getClass().getResource("anonymous-readonly.zip")).allocate();
        SubversionSCM scm = new SubversionSCM("file://" + repo.getPath());
        // create a build
        FreeStyleProject p = createFreeStyleProject();
        p.setScm(scm);
        //Set anonymous user for authentication.
        SecurityContextHolder.getContext().setAuthentication(Hudson.ANONYMOUS);
        p.setScm(loadSvnRepo());
        FreeStyleBuild b = p.scheduleBuild2(0, new Cause.UserCause()).get();
        System.out.println(b.getLog(LOG_LIMIT));
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
        WebClient webclient = new WebClient();
        webclient.setThrowExceptionOnScriptError(false);
        submit(new WebClient().getPage(p, "configure").getFormByName("config"));
        verify(scm, (SubversionSCM) p.getScm());

        scm = new SubversionSCM(
            Arrays.asList(
                new SubversionSCM.ModuleLocation(
                    "https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/testSubversionExclusion", "c")),
            false, null, "", "", "", "");
        p.setScm(scm);
        submit(webclient.getPage(p, "configure").getFormByName("config"));
        verify(scm, (SubversionSCM) p.getScm());

        scm = new SubversionSCM(
            Arrays.asList(
                new SubversionSCM.ModuleLocation(
                    "https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/testSubversionExclusion", "")),
            true, null, null, null, null, null);
        p.setScm(scm);
        submit(webclient.getPage(p, "configure").getFormByName("config"));
        verify(scm, (SubversionSCM) p.getScm());
    }

    public void testMasterPolling() throws Exception {
        File repo = new CopyExisting(getClass().getResource("two-revisions.zip")).allocate();
        SubversionSCM scm = new SubversionSCM("file://" + repo.getPath());
        SubversionSCM.POLL_FROM_MASTER = true;

        FreeStyleProject p = createFreeStyleProject();
        p.setScm(scm);
        p.setAssignedLabel(createSlave().getSelfLabel());
        assertBuildStatusSuccess(p.scheduleBuild2(2).get());

        // initial polling on the master for the code path that doesn't find any change
        assertFalse(p.pollSCMChanges(new StreamTaskListener(System.out)));

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
        assertTrue(p.pollSCMChanges(new StreamTaskListener(System.out)));
    }


    public void testCompareSVNAuthentications() throws Exception {
        assertFalse(compareSVNAuthentications(new SVNUserNameAuthentication("me", true),
            new SVNSSHAuthentication("me", "me", 22, true)));
        // same object should compare equal
        _idem(new SVNUserNameAuthentication("me", true));
        _idem(new SVNSSHAuthentication("me", "pass", 22, true));
        _idem(new SVNSSHAuthentication("me", new File("./some.key"), null, 23, false));
        _idem(new SVNSSHAuthentication("me", "key".toCharArray(), "phrase", 0, false));
        _idem(new SVNPasswordAuthentication("me", "pass", true));
        _idem(new SVNSSLAuthentication("certificate".getBytes(), null, true));

        // make sure two Files and char[]s compare the same
        assertTrue(compareSVNAuthentications(
            new SVNSSHAuthentication("me", new File("./some.key"), null, 23, false),
            new SVNSSHAuthentication("me", new File("./some.key"), null, 23, false)));
        assertTrue(compareSVNAuthentications(
            new SVNSSHAuthentication("me", "key".toCharArray(), "phrase", 0, false),
            new SVNSSHAuthentication("me", "key".toCharArray(), "phrase", 0, false)));

        // negative cases
        assertFalse(compareSVNAuthentications(
            new SVNSSHAuthentication("me", new File("./some1.key"), null, 23, false),
            new SVNSSHAuthentication("me", new File("./some2.key"), null, 23, false)));
        assertFalse(compareSVNAuthentications(
            new SVNSSHAuthentication("me", "key".toCharArray(), "phrase", 0, false),
            new SVNSSHAuthentication("yo", "key".toCharArray(), "phrase", 0, false)));

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
     * Loads a test Subversion repository into a temporary directory, and creates {@link org.eclipse.hudson.scm.subversion.SubversionSCM} for it.
     */
    private SubversionSCM loadSvnRepo() throws Exception {
        return new SubversionSCM(
            "file://" + new CopyExisting(getClass().getResource("svn-repo.zip")).allocate().toURI().toURL().getPath()
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

    private void _idem(SVNAuthentication a) {
        assertTrue(compareSVNAuthentications(a, a));
    }
}
