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

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.slaves.NodeProperty;
import hudson.slaves.DumbSlave;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Future;

import javax.servlet.ServletException;

import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.Email;
import org.jvnet.hudson.test.HudsonHomeLoader.CopyExisting;
import org.jvnet.hudson.test.Url;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * @author Kohsuke Kawaguchi
 */
public class SubversionCheckoutTest extends AbstractSubversionTest {

    private static final int LOG_LIMIT = 1000;

    String kind = ISVNAuthenticationManager.PASSWORD;

    @Email("http://www.nabble.com/Hudson-1.266-and-1.267%3A-Subversion-authentication-broken--td21156950.html")
    public void testHttpsCheckOut() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.setScm(new SubversionSCM("https://datacard.googlecode.com/svn/trunk"));

        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0, new Cause.UserCause()).get());
        assertTrue(b.getWorkspace().child("README.txt").exists());
    }

    @Email("http://hudson.361315.n4.nabble.com/Hudson-1-266-and-1-267-Subversion-authentication-broken-td375737.html")
    public void testHttpCheckOut() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.setScm(new SubversionSCM("http://svn.codehaus.org/sxc/tags/sxc-0.5/sxc-core/src/test/java/com/envoisolutions/sxc/builder/"));

        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0, new Cause.UserCause()).get());
        assertTrue(b.getWorkspace().child("Node.java").exists());
    }

    @Url("http://hudson.pastebin.com/m3ea34eea")
    public void testRemoteCheckOut() throws Exception {
        DumbSlave s = createSlave();
        FreeStyleProject p = createFreeStyleProject();
        p.setAssignedLabel(s.getSelfLabel());
        p.setScm(new SubversionSCM("http://svn.apache.org/repos/asf/subversion/trunk/doc"));

        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0, new Cause.UserCause()).get());
        assertTrue(b.getWorkspace().child("README").exists());
        assertNotNull(assertBuildStatusSuccess(p.scheduleBuild2(0).get()));
    }

    /**
     * Tests the "URL@REV" format in SVN URL.
     */
    @Bug(262)
    public void testRevisionedCheckout() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.setScm(new SubversionSCM("http://svn.apache.org/repos/asf/subversion/trunk/doc@1244918"));

        FreeStyleBuild b = p.scheduleBuild2(0, new Cause.UserCause()).get();
        System.out.println(b.getLog(LOG_LIMIT));
        assertTrue(b.getLog(LOG_LIMIT).contains("At revision 1244918"));
        assertBuildStatus(Result.SUCCESS, b);
    }

    /**
     * Tests the "URL@HEAD" format in the SVN URL
     */
    public void testHeadRevisionCheckout() throws Exception {
        File testRepo = new CopyExisting(getClass().getResource("two-revisions.zip")).allocate();
        SubversionSCM scm = new SubversionSCM("file:///" + testRepo.getPath() + "@HEAD");

        FreeStyleProject p = createFreeStyleProject();
        p.setScm(scm);

        FreeStyleBuild b = p.scheduleBuild2(0, new Cause.UserCause()).get();
        System.out.println(b.getLog(LOG_LIMIT));
        assertTrue(b.getLog(LOG_LIMIT).contains("At revision 2"));
        assertBuildStatus(Result.SUCCESS, b);
    }

    /**
     * Test parsing of @revision information from the tail of the URL
     */
    public void testModuleLocationRevisions() {
        SubversionSCM.ModuleLocation m = new SubversionSCM.ModuleLocation("https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/trivial-ant@13000", null);
        SVNRevision r = m.getRevision(null);
        assertTrue(r.isValid());
        assertEquals(13000, r.getNumber());
        assertEquals("https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/trivial-ant", m.getURL());

        m = new SubversionSCM.ModuleLocation("https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/trivial-ant@HEAD", null);
        r = m.getRevision(null);
        assertTrue(r.isValid());
        assertTrue(r == SVNRevision.HEAD);
        assertEquals("https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/trivial-ant", m.getURL());

        m = new SubversionSCM.ModuleLocation("https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/trivial-ant@FAKE", null);
        r = m.getRevision(null);
        assertFalse(r.isValid());
        assertEquals("https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/trivial-ant@FAKE", m.getURL());
    }

    /**
     * Test parsing of @revision information from the tail of the URL
     */
    public void testModuleLocationWithDepthIgnoreExternalsOption() throws Exception {
        FreeStyleProject p = createFreeStyleProject();

        SubversionSCM scm = new SubversionSCM(
                Arrays.asList(
                        new SubversionSCM.ModuleLocation("https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/testSubversionExclusion", "c", "infinity", true),
                        new SubversionSCM.ModuleLocation("https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/testSubversionExclusion", "d", "files", false)),
               false, false, null, null, null, null, null, null);
        p.setScm(scm);
        submit(new WebClient().getPage(p, "configure").getFormByName("config"));
        verify(scm, (SubversionSCM) p.getScm());
    }

    /**
     * Tests a checkout with RevisionParameterAction
     */
    public void testRevisionParameter() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        String url = "http://svn.apache.org/repos/asf/subversion/trunk/doc";
        p.setScm(new SubversionSCM(url));

        FreeStyleBuild b = p.scheduleBuild2(0, new Cause.UserCause(),
                new RevisionParameterAction(new SubversionSCM.SvnInfo(url, 1162787))).get();
        System.out.println(b.getLog(LOG_LIMIT));
        assertTrue(b.getLog(LOG_LIMIT).contains("At revision 1162787"));
        assertBuildStatus(Result.SUCCESS, b);
    }

    public void testRevisionParameterFolding() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        String url = "http://svn.apache.org/repos/asf/subversion/trunk/doc";
	p.setScm(new SubversionSCM(url));

	// Schedule build of a specific revision with a quiet period
        Future<FreeStyleBuild> f = p.scheduleBuild2(60, new Cause.UserCause(),
        		new RevisionParameterAction(new SubversionSCM.SvnInfo(url, 1162786)));

	// Schedule another build at a more recent revision
	p.scheduleBuild2(0, new Cause.UserCause(),
        		new RevisionParameterAction(new SubversionSCM.SvnInfo(url, 1162787)));

        FreeStyleBuild b = f.get();

        System.out.println(b.getLog(LOG_LIMIT));
        assertTrue(b.getLog(LOG_LIMIT).contains("At revision 1162787"));
        assertBuildStatus(Result.SUCCESS,b);
    }

    @Bug(5684)
    public void testDoCheckExcludedUsers() throws IOException, ServletException {
        String[] validUsernames = new String[]{
                "DOMAIN\\user",
                "user",
                "us_er",
                "user123",
                "User",
                "", // this one is ignored
                "DOmain12\\User34",
                "DOMAIN.user",
                "continuous-habbo"};

        for (String validUsername : validUsernames) {
            assertEquals(
                    "User " + validUsername + " isn't OK (but it's valid).",
                    FormValidation.Kind.OK,
                    new SubversionSCM.DescriptorImpl().doCheckExcludedUsers(validUsername).kind);
        }

        String[] invalidUsernames = new String[]{
                "\\user",
                "DOMAIN\\",
                "DOMAIN@user"};

        for (String invalidUsername : invalidUsernames) {
            assertEquals(
                    "User " + invalidUsername + " isn't ERROR (but it's not valid).",
                    FormValidation.Kind.ERROR,
                    new SubversionSCM.DescriptorImpl().doCheckExcludedUsers(invalidUsername).kind);
        }

    }
    
    @Test
    @Bug(397860)
    public void testGlobalVariableInURL() throws IOException {
    	String expectedURL = "https://TEST/asdf";
    	String inputURL = "https://${TEST}/asdf";
    	
    	EnvironmentVariablesNodeProperty.Entry e = new EnvironmentVariablesNodeProperty.Entry("TEST", "TEST");
    	NodeProperty<?> np = new EnvironmentVariablesNodeProperty(e);
    	
    	hudson.getInstance().getGlobalNodeProperties().add(np);
    	
    	assertNotNull(hudson.getInstance().getGlobalNodeProperties().get(0)); // Verify it was inserted successfully.
    	
    	ModuleLocation loc = new ModuleLocation(inputURL, "");
    	
    	assertEquals(expectedURL, loc.getURL());
    }
    
    @Test
    @Bug(397860)
    public void testMultipleGlobalVariablesInURL() throws IOException {
    	String expectedURL = "https://TEST/asdf";
    	String inputURL = "https://${REPOURL}/${BRANCH}";
    	
    	EnvironmentVariablesNodeProperty.Entry e = new EnvironmentVariablesNodeProperty.Entry("REPOURL", "TEST");
    	EnvironmentVariablesNodeProperty.Entry e1 = new EnvironmentVariablesNodeProperty.Entry("SVNURL", "TESTs");
    	EnvironmentVariablesNodeProperty.Entry e2 = new EnvironmentVariablesNodeProperty.Entry("BRANCH", "asdf");
    	NodeProperty<?> np = new EnvironmentVariablesNodeProperty(e, e1, e2);
    	
    	hudson.getInstance().getGlobalNodeProperties().add(np);
    	
    	assertNotNull(hudson.getInstance().getGlobalNodeProperties().get(0)); // Verify it was inserted successfully.
    	
    	ModuleLocation loc = new ModuleLocation(inputURL, "");
    	
    	assertEquals(expectedURL, loc.getURL());
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
        if (left == null)
            left = "";
        if (right == null)
            right = "";
        assertEquals(left, right);
    }
}
