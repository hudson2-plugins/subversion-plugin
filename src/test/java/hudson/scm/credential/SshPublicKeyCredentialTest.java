package hudson.scm.credential;

import java.io.File;
import java.io.IOException;

import hudson.model.Hudson;
import hudson.remoting.Channel;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.taskdefs.Chmod;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hudson.class, Messages.class, Channel.class})
public class SshPublicKeyCredentialTest {
	@Mock
	Hudson hudson;
	
	/**
	 * Method used to set up a mock hudson instance.
	 */
	public void mockHudson() {
		when(hudson.getRootDir()).thenReturn(new File("./src/test/resources"));
		PowerMockito.mockStatic(Hudson.class);
		when(Hudson.getInstance()).thenReturn(hudson);
	}
	
	/*
	 * Method used to create an invalid root directory (DNE directory).
	 */
	public void mockInvalidHudson() {
		when(hudson.getRootDir()).thenReturn(new File("./asdf"));
		PowerMockito.mockStatic(Hudson.class);
		when(Hudson.getInstance()).thenReturn(hudson);
	}
	
	@After
	public void tearDown() {
			File f = new File("/tmp/.hudsonrsa");
			FileUtils.deleteQuietly(f);
			f = new File("./asdf");
			FileUtils.deleteQuietly(f);
			f = new File("./src/test/resources/subversion-credentials");
			FileUtils.deleteQuietly(f);
	}
	
	
	// Create a simple test case for a valid source/destination for hudson ssh keys.
	@Test
	public void testWorkingConstruction() throws SVNException, IOException {
		mockHudson();
		File f = new File("/tmp/.hudsonrsa");
		if (!f.exists())
			f.createNewFile();
		
		SshPublicKeyCredential sshKey = new SshPublicKeyCredential("username", "password", f);
	}
	
	// This tests an invalid construction where the directory does not exist.
	@Test (expected = SVNException.class)
	public void testInvalidConstruction() throws SVNException {
		mockHudson();
		File f = new File("/tmp/.hudsonrsa");
		if (f.exists())
			f.delete();
		
		SshPublicKeyCredential sshKey = new SshPublicKeyCredential("username", "password", f);
	}
	
	
	// Test ability to create directory when folder does not exist for private key.
	@Test
	public void testBadHudsonRootDirectory() throws SVNException, IOException {
		mockInvalidHudson();
		File f = new File("/tmp/.hudsonrsa");
		if (!f.exists())
			f.createNewFile();
		
		SshPublicKeyCredential sshKey = new SshPublicKeyCredential("username", "password", f);
	}
	
	// Test ability to setup CHMOD 600 permissions for folder.
	// TODO: Mock Chmod class to throw an exception. We need to verify if it fails to create permissions.
	// Should be back on next release.
	@Ignore
	@Test
	public void testBadPermissions() throws Exception {
		mockInvalidHudson();
		
		PowerMockito.mockStatic(Messages.class);
		when(Messages.SshPublicKeyCredential_private_key_permissions()).thenReturn("aewfgrea"); // Dirty Dirty Dirty! I feel bad doing this.
		when(Messages.SshPublicKeyCredential_private_key()).thenReturn("subversion-credentials");
		
		File f = new File("/tmp/.hudsonrsa");
		if (!f.exists())
			f.createNewFile();
		// This test requires that the hudson root does not exist.
		f = new File("./asdf");
		if (!f.exists())
			f.mkdir();
		
		SshPublicKeyCredential sshKey = new SshPublicKeyCredential("username", "password", f);
	}
	
	@Test
	public void testAuthenticationCreation() throws Exception {
		mockHudson();
		File f = new File("/tmp/.hudsonrsa");
		if (!f.exists())
			f.createNewFile();
		SshPublicKeyCredential sshKey = new SshPublicKeyCredential("username", "password", f);
		SVNSSHAuthentication authent = sshKey.createSVNAuthentication(ISVNAuthenticationManager.SSH);
		assertNotNull(authent);
	}
	
	@Test
	public void testInvalidAuthenticationType() throws Exception {
		mockHudson();
		File f = new File("/tmp/.hudsonrsa");
		if (!f.exists())
			f.createNewFile();
		SshPublicKeyCredential sshKey = new SshPublicKeyCredential("username", "password", f);
		assertNull(sshKey.createSVNAuthentication(ISVNAuthenticationManager.SSL));
	}	
}