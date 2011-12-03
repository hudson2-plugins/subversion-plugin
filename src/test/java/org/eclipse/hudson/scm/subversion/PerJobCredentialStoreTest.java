/*******************************************************************************
 *
<<<<<<< HEAD:src/test/java/org/eclipse/hudson/scm/subversion/PerJobCredentialStoreTest.java
 * Copyright (c) 2011 Oracle Corporation.
=======
 * Copyright (c) 2011, Oracle Corporation, Nikita Levyankov, Anton Kozak
>>>>>>> refs/heads/master:src/test/java/hudson/scm/PerJobCredentialStoreTest.java
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi,Nikita Levyankov, Anton Kozak
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import hudson.Proc;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.matrix.TextAxis;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import org.jvnet.hudson.test.Bug;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

import static org.eclipse.hudson.scm.subversion.SubversionSCM.compareSVNAuthentications;

/**
 * @author Kohsuke Kawaguchi
 * @author Nikita Levyankov
 */
public class PerJobCredentialStoreTest extends AbstractSubversionTest {
    private static final String testSvnUser = "user";
    private static final String testSvnPassword = "userpass";
    private static final String testSvnRealm = "Test realm";
    private static final String GUEST_ACCESS_REPOSITORY_RESOURCE = "guest_access_svn.zip";
    private static final String realm = "<svn://localhost:3690>";
    private static final String SVN_URL = "svn://localhost/bob";
    private static final String GUEST_USER_LOGIN = "guest";
    private static final String GUEST_USER_PASSWORD = "guestpass";
    private static final String BOGUS_USER_LOGIN = "bogus";
    private static final String BOGUS_USER_PASSWORD = "boguspass";

    private PerJobCredentialStore credentialStore;

    public void testAcknowledgeNonEmptyCredentials() throws IOException {
        FreeStyleProject p = createFreeStyleProject();
        credentialStore = new PerJobCredentialStore(p, null);
        assertFalse(credentialStore.getSaveableListener().isFileChanged());
        SubversionSCM.DescriptorImpl.Credential credential = new SubversionSCM.DescriptorImpl.PasswordCredential(
            testSvnUser, testSvnPassword);
        credentialStore.acknowledgeAuthentication(testSvnRealm, credential);
        assertTrue(credentialStore.getSaveableListener().isFileChanged());
    }

    public void testAcknowledgeEmptyCredentials() throws IOException {
        FreeStyleProject p = createFreeStyleProject();
        credentialStore = new PerJobCredentialStore(p, null);
        SubversionSCM.DescriptorImpl.Credential credential = new SubversionSCM.DescriptorImpl.PasswordCredential(
            testSvnUser, testSvnPassword);
        //Store password credentials
        credentialStore.acknowledgeAuthentication(testSvnRealm, credential);
        //Reset file changed status flag in order to acknowledge null credentials
        credentialStore.getSaveableListener().resetChangedStatus();
        //Emulate call from slave.
        credentialStore.acknowledgeAuthentication(testSvnRealm, null);
        assertFalse(credentialStore.getSaveableListener().isFileChanged());
    }

    @Bug(3)
    public void testMatrixConfigurationCredentialsFileNamePath() throws IOException {
        MatrixProject p = createMatrixProject("matrix");
        p.setScm(new SubversionSCM("https://datacard.googlecode.com/svn/trunk"));
        Collection<MatrixConfiguration> configurations =  p.getItems();
        assertEquals(configurations.size(), 2);

        MatrixConfiguration configuration = configurations.iterator().next();
        SubversionSCM scm = (SubversionSCM)configuration.getScm();

        SubversionSCM.DescriptorImpl.SVNAuthenticationProviderImpl provider =
            (SubversionSCM.DescriptorImpl.SVNAuthenticationProviderImpl)scm.getDescriptor().
                createAuthenticationProvider(configuration);
        PerJobCredentialStore perJobCredentialStore = (PerJobCredentialStore)provider.getLocal();
        String correctPath = "matrix" + File.separator + "subversion.credentials";
        assertTrue(perJobCredentialStore.getXmlFile().getFile().getCanonicalPath().endsWith(correctPath));
    }

    /**
     * There was a bug that credentials stored in the remote call context was serialized wrongly.
     */
    @Bug(8061)
    public void testRemoteBuild() throws Exception {
        Proc p = null;
        try {
            p = runSvnServe(getClass().getResource("HUDSON-1379.zip"));
            FreeStyleProject b = createFreeStyleProject();
            b.setScm(new SubversionSCM(SVN_URL));
            b.setAssignedNode(createSlave());

            descriptor.postCredential(b, SVN_URL, "alice", "alice", null, new PrintWriter(System.out));

            buildAndAssertSuccess(b);

            PerJobCredentialStore store = new PerJobCredentialStore(b, SVN_URL);
            assertFalse(store.isEmpty());   // credential store should contain a valid entry
        } finally {
            if (p != null) {
                p.kill();
            }
        }
    }

    /**
     * Even if the default providers remember bogus passwords, Hudson should still attempt what it knows.
     */
    @Bug(3936)
    public void test3936() throws Exception {
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

            // teach a bogus credential and have SVNKit store it.
            SVNPasswordAuthentication bogus = new SVNPasswordAuthentication(BOGUS_USER_LOGIN, BOGUS_USER_PASSWORD,
                true);
            m.acknowledgeAuthentication(true, kind, realm, null, bogus);
            assertTrue(compareSVNAuthentications(m.getFirstAuthentication(kind, realm, repo), bogus));
            try {
                attemptAccess(repo, m);
                fail("SVNKit shouldn't yet know how to access");
            } catch (SVNCancelException e) {
            }

            // make sure the failure didn't clean up the cache,
            // since what we want to test here is Hudson trying to supply its credential, despite the failed cache
            assertTrue(compareSVNAuthentications(m.getFirstAuthentication(kind, realm, repo), bogus));

            // now let Hudson have the real credential
            // can we now access the repo?
            descriptor.postCredential(null, repo.toDecodedString(), GUEST_USER_LOGIN, GUEST_USER_PASSWORD, null,
                new PrintWriter(System.out));
            attemptAccess(repo, m);
        } finally {
            server.kill();
        }
    }

    @Bug(1379)
    public void testMultipleCredentialsPerRepo() throws Exception {
        Proc p = runSvnServe(getClass().getResource("HUDSON-1379.zip"));
        try {
            FreeStyleProject b = createFreeStyleProject();
            b.setScm(new SubversionSCM(SVN_URL));

            FreeStyleProject c = createFreeStyleProject();
            c.setScm(new SubversionSCM("svn://localhost/charlie"));

            // should fail without a credential
            assertBuildStatus(Result.FAILURE, b.scheduleBuild2(0).get());
            descriptor.postCredential(b, SVN_URL, "bob", "bob", null, new PrintWriter(System.out));
            buildAndAssertSuccess(b);

            assertBuildStatus(Result.FAILURE, c.scheduleBuild2(0).get());
            descriptor.postCredential(c, "svn://localhost/charlie", "charlie", "charlie", null,
                new PrintWriter(System.out));
            buildAndAssertSuccess(c);

            // b should still build fine.
            buildAndAssertSuccess(b);
        } finally {
            p.kill();
        }
    }


    @Bug(1379)
    public void testSuperUserForAllRepos() throws Exception {
        Proc p = runSvnServe(getClass().getResource("HUDSON-1379.zip"));
        try {
            FreeStyleProject b = createFreeStyleProject();
            b.setScm(new SubversionSCM(SVN_URL));

            FreeStyleProject c = createFreeStyleProject();
            c.setScm(new SubversionSCM("svn://localhost/charlie"));

            // should fail without a credential
            assertBuildStatus(Result.FAILURE, b.scheduleBuild2(0).get());
            assertBuildStatus(Result.FAILURE, c.scheduleBuild2(0).get());

            // but with the super user credential both should work now
            descriptor.postCredential(b, SVN_URL, "alice", "alice", null, new PrintWriter(System.out));
            buildAndAssertSuccess(b);
            buildAndAssertSuccess(c);
        } finally {
            p.kill();
        }
    }

    private void attemptAccess(SVNURL repo, ISVNAuthenticationManager m) throws SVNException {
        SVNRepository repository = SVNRepositoryFactory.create(repo);
        repository.setAuthenticationManager(m);
        repository.testConnection();
    }

    @Override
    protected MatrixProject createMatrixProject(String name) throws IOException {
        MatrixProject p = super.createMatrixProject(name);

        // set up 2x2 matrix
        AxisList axes = new AxisList();
        axes.add(new TextAxis("db","mysql","oracle"));
        p.setAxes(axes);

        return p;
    }
}
