package hudson.scm;

import hudson.Proc;
import hudson.model.FreeStyleProject;
import java.io.IOException;
import java.io.PrintWriter;
import org.jvnet.hudson.test.Bug;

/**
 * @author Kohsuke Kawaguchi
 */
public class PerJobCredentialStoreTest extends AbstractSubversionTest {
    private static final String testSvnUser = "user";
    private static final String testSvnPassword = "userpass";
    private static final String testSvnRealm = "Test realm";

    private PerJobCredentialStore credentialStore;

    public void testAcknowledgeNonEmptyCredentials() throws IOException {
        FreeStyleProject p = createFreeStyleProject();
        credentialStore = new PerJobCredentialStore(p);
        assertFalse(credentialStore.getSaveableListener().isFileChanged());
        SubversionSCM.DescriptorImpl.Credential credential = new SubversionSCM.DescriptorImpl.PasswordCredential(testSvnUser, testSvnPassword);
        credentialStore.acknowledge(testSvnRealm, credential);
        assertTrue(credentialStore.getSaveableListener().isFileChanged());
    }

    public void testAcknowledgeEmptyCredentials() throws IOException {
        FreeStyleProject p = createFreeStyleProject();
        credentialStore = new PerJobCredentialStore(p);
        SubversionSCM.DescriptorImpl.Credential credential = new SubversionSCM.DescriptorImpl.PasswordCredential(testSvnUser, testSvnPassword);
        //Store password credentials
        credentialStore.acknowledge(testSvnRealm, credential);
        //Reset file changed status flag in order to acknowledge null credentials
        credentialStore.getSaveableListener().resetChangedStatus();
        //Emulate call from slave.
        credentialStore.acknowledge(testSvnRealm, null);
        assertFalse(credentialStore.getSaveableListener().isFileChanged());
    }
    /**
     * There was a bug that credentials stored in the remote call context was serialized wrongly.
     */
    @Bug(8061)
    public void testRemoteBuild() throws Exception {
        Proc p = runSvnServe(SubversionSCMTest.class.getResource("HUDSON-1379.zip"));
        try {
            FreeStyleProject b = createFreeStyleProject();
            b.setScm(new SubversionSCM("svn://localhost/bob"));
            b.setAssignedNode(createSlave());

            descriptor.postCredential(b,"svn://localhost/bob","alice","alice",null,new PrintWriter(System.out));
            
            buildAndAssertSuccess(b);

            PerJobCredentialStore store = new PerJobCredentialStore(b);
            assertFalse(store.isEmpty());   // credential store should contain a valid entry
        } finally {
            p.kill();
        }
    }
}
