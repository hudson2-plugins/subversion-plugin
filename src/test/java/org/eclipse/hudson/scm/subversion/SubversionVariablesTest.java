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
 * Kohsuke Kawaguchi
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import hudson.Proc;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.CaptureEnvironmentBuilder;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;

/**
 * @author Kohsuke Kawaguchi
 */
public class SubversionVariablesTest extends AbstractSubversionTest {
    private static final int LOG_LIMIT = 1000;
    private static final String GUEST_ACCESS_REPOSITORY_RESOURCE = "guest_access_svn.zip";
    private static final String realm = "<svn://localhost:3690>";
    private static final String SVN_URL = "svn://localhost/bob";
    private static final String GUEST_USER_LOGIN = "guest";
    private static final String GUEST_USER_PASSWORD = "guestpass";
    private static final String BOGUS_USER_LOGIN = "bogus";
    private static final String BOGUS_USER_PASSWORD = "boguspass";
    protected static final String SVN_URL1 = "http://svn.apache.org/repos/asf/subversion/trunk/doc";
    protected static final String SVN_URL2 = "http://svn.apache.org/repos/asf/subversion/trunk/packages";

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
            SVNPasswordAuthentication authentication = new SVNPasswordAuthentication(BOGUS_USER_LOGIN,
                BOGUS_USER_PASSWORD, true);
            m.acknowledgeAuthentication(false, kind, realm, null, authentication);

            authentication = new SVNPasswordAuthentication(GUEST_USER_LOGIN, GUEST_USER_PASSWORD, true);
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
        assertBuildStatus(Result.SUCCESS, b);
    }

    /**
     * Test parsing of @revision information from the tail of the URL
     */
    //TODO fix me
    public void ignore_testModuleLocationWithDepthIgnoreExternalsOption() throws Exception {
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

    private Long getActualRevision(FreeStyleBuild b, String url) throws Exception {
        SVNRevisionState revisionState = b.getAction(SVNRevisionState.class);
        if (revisionState == null) {
            throw new Exception("No revision found!");
        }

        return revisionState.revisions.get(url).longValue();

    }

    private void assertNullEquals(String left, String right) {
        if (left == null)
            left = "";
        if (right == null)
            right = "";
        assertEquals(left, right);
    }
}
