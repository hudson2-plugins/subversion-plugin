/**
 * 
 */
package hudson.scm.credential;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.Future;

import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.Result;
import hudson.scm.AbstractSubversionTest;
import hudson.scm.UserProvidedCredential;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.External;
import hudson.util.Scrambler;
import hudson.util.XStream2;

import org.junit.Test;
import org.tmatesoft.svn.core.SVNCancelException;

import com.thoughtworks.xstream.XStream;

/**
 * DO NOT CHANGE JUNITS:
 * 
 * This junit is to maintain backward compatibility with previous version of the subversion plugin.
 * Changing this class in any way could break backward compatibility.
 * 
 * @author schristou88
 *
 */
public class SubversionStoringCredentialsTest extends AbstractSubversionTest {
	
	private String username = "testsvnplugin";
	private String password = "dGVzdHN2bnBsdWdpbg==";
	private String url = "https://svn.java.net/svn/hudson-test~svn-plugin-test";
	private String credentialsFileName = "subversion.credentials";
	
	@Test
	public void testPerJobCredentialStoring() throws Exception {
		FreeStyleProject proj = createFreeStyleProject();
		SubversionSCM scm = new SubversionSCM(url);
		proj.setScm(scm);
		
		// Should fail since there are no credentials to try.
		assertBuildStatus(Result.FAILURE, proj.scheduleBuild2(0).get());
		
		// Let's add credentials
        descriptor.postCredential(url, new UserProvidedCredential(username, Scrambler.descramble(password), null, proj), new PrintWriter(System.out));
        
        // Verify we can build.
        assertBuildStatusSuccess(proj.scheduleBuild2(0));
        
        // Verify subversion.credentials file was created with necessary content.
        File f = new File(proj.getRootDir(), credentialsFileName);
        assertTrue(f.exists());
	}
	
	// TODO: Write remaining test units
//	
//	@Test
//	public void testOverrideGlobalCredentials() {
//		
//	}
//	
//	@Test
//	public void testGlobalHudsonCredentialStoring() {
//		
//	}
//	
//	@Test
//	public void testSystemHudsonCredentialStoring() {
//		
//	}
	
	
	
    private static final XStream XSTREAM = new XStream2();

    static {
        XSTREAM.alias("external", External.class);
    }
}
