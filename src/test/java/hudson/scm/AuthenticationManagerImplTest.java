/**
 * 
 */
package hudson.scm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import hudson.model.TaskListener;
import hudson.model.Hudson;
import hudson.scm.UserProvidedCredential.AuthenticationManagerImpl;
import hudson.scm.credential.PasswordCredential;
import hudson.util.LogTaskListener;
import hudson.util.StreamTaskListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

/**
 * @author schristou88
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Hudson.class})
public class AuthenticationManagerImplTest {
	UserProvidedCredential upc;
	StreamTaskListener streamTaskListener;
	ByteArrayOutputStream baos;
	@Mock Hudson hudson;
	@Mock SVNErrorMessage mockSVNErrorMessage;
	@Mock SVNAuthentication mockSVNAuthentication;
	
	@Before
	public void setUp() {
		upc = new UserProvidedCredential("testUsername", "testPassword", new File("/tmp"));
		// Mock hudson first.
		PowerMockito.mockStatic(Hudson.class);
		when(Hudson.getInstance()).thenReturn(hudson);
		when(hudson.getRootDir()).thenReturn(new File("/tmp"));
		baos = new ByteArrayOutputStream();
		streamTaskListener = new StreamTaskListener(baos);
		MockitoAnnotations.initMocks(this);
	}
	
	@After
	public void tearDown() {
		new File("/tmp/test.ppk").delete();
	}
	
	@Test
	public void testGetFirstAuthenticationWithPassword() throws Exception {
		// Given
		AuthenticationManagerImpl ami = upc.new AuthenticationManagerImpl(streamTaskListener);
		
		// When
		// When we're dealing with svn+ssh or file:/// authentication which means there's only a username which performs
		// the authentication.
		SVNAuthentication svnAuthentication = ami.getFirstAuthentication(ISVNAuthenticationManager.USERNAME, null, null);
		
		// Then
		assertNotNull(svnAuthentication);
		assertTrue("When specifying a username only authentication we did not get back username authentication",
				svnAuthentication instanceof SVNUserNameAuthentication);
	}
	
	@Test
	public void testGetFirstAuthenticationWithUserPasswordCombo() throws Exception {
		// Given
		AuthenticationManagerImpl ami = upc.new AuthenticationManagerImpl(streamTaskListener);
		
		// When
		SVNAuthentication svnAuthentication = ami.getFirstAuthentication(ISVNAuthenticationManager.PASSWORD, "", null);
		
		// Then
		assertNotNull(svnAuthentication);
		assertTrue("When specifying a username/password combo, we did not get back the correct authentication type",
				ami.getCredential() instanceof hudson.scm.SubversionSCM.DescriptorImpl.PasswordCredential);
		assertEquals("Passing user name testUsername and password you entered", baos.toString().trim());
	}
	
	@Test
	public void testGetFirstAuthenticationWithSSHWithKeyFile() throws Exception {
		// Given
		new File("/tmp/test.ppk").createNewFile();
		upc = new UserProvidedCredential("testUsername", "testPassword", new File("/tmp/test.ppk"));
		AuthenticationManagerImpl ami = upc. new AuthenticationManagerImpl(streamTaskListener);
		
		// When
		SVNAuthentication svnAuthentication = ami.getFirstAuthentication(ISVNAuthenticationManager.SSH, "", null);
		
		// Then
		assertNotNull(svnAuthentication);
		assertEquals("Attempting a public key authentication with username testUsername",baos.toString().trim());
	}
	
	@Test
	public void testGetFirstAuthenticationWithSSHWithoutKeyFile() throws Exception {
		// Given
		upc = new UserProvidedCredential("testUsername", "testPassword", null);
		AuthenticationManagerImpl ami = upc. new AuthenticationManagerImpl(streamTaskListener);
		
		// When
		SVNAuthentication svnAuthentication = ami.getFirstAuthentication(ISVNAuthenticationManager.SSH, "", null);
		
		// Then
		assertNotNull(svnAuthentication);
		assertEquals("Passing user name testUsername and password you entered to SSH", baos.toString().trim());
	}
	
	@Test 
	public void testGetFirstAuthenticationWithSSLCertificateWithFolder() throws Exception {
		// Given
		AuthenticationManagerImpl ami = upc.new AuthenticationManagerImpl(streamTaskListener);

		// When
		SVNAuthentication svnAuthentication = ami.getFirstAuthentication(ISVNAuthenticationManager.SSL, "", null);
		
		// Then
		assertNull("Should have thrown an exception when certificate is a folder not a file.",
					svnAuthentication);
		assertEquals("Attempting an SSL client certificate authentcation", baos.toString().split("\n")[0].trim());
	}

	@Test 
	public void testGetFirstAuthenticationWithSSLCertificateWithoutFolder() throws Exception {
		// Given
		new File("/tmp/test.ppk").createNewFile();
		upc = new UserProvidedCredential("testUsername", "testPassword", new File("/tmp/test.ppk"));
		AuthenticationManagerImpl ami = upc.new AuthenticationManagerImpl(streamTaskListener);

		// When
		SVNAuthentication svnAuthentication = ami.getFirstAuthentication(ISVNAuthenticationManager.SSL, "", null);
		
		// Then
		assertNotNull(svnAuthentication);
		assertEquals("Attempting an SSL client certificate authentcation", baos.toString().trim());
	}
	
	@Test
	public void testGetFirstAuthenticationWithUnknownAuthenticationMethod() throws Exception {
		// Given
		AuthenticationManagerImpl ami = upc.new AuthenticationManagerImpl(streamTaskListener);
		
		// When
		SVNAuthentication svnAuthentication = ami.getFirstAuthentication("svn.invalid", "", null);
		
		// Then
		assertNull(svnAuthentication);
		assertEquals("Unknown authentication method: svn.invalid", baos.toString().trim());
	}

	@Test (expected = SVNAuthenticationException.class)
	public void testGetNextAuthentication() throws Exception {
		// Given
		AuthenticationManagerImpl ami = upc.new AuthenticationManagerImpl(streamTaskListener);
		
		// Should throw exception because if first authentication failed, second should also.
		ami.getNextAuthentication("", "", null);
	}
	
	@Test
	public void testAcknowledgeAuthenticationAndAccepted() throws Exception {
		// Given
		AuthenticationManagerImpl ami = upc. new AuthenticationManagerImpl(streamTaskListener);
		
		// When
		ami.acknowledgeAuthentication(true, "", "", mockSVNErrorMessage, mockSVNAuthentication);
		
		// Then
		assertTrue("Something got printed when it shouldn't have been", baos.toString().length() == 0); 
	}
	
	@Test
	public void testAcknowledgeAuthenticationAndAccepteds() throws Exception {
		// Given
		AuthenticationManagerImpl ami = upc. new AuthenticationManagerImpl(streamTaskListener);
		ami.getFirstAuthentication(ISVNAuthenticationManager.PASSWORD, "", null);
		
		// When
		ami.acknowledgeAuthentication(true, "", "", mockSVNErrorMessage, mockSVNAuthentication);
		
		// Then
		assertEquals("Passing user name testUsername and password you entered", baos.toString().trim());
	}

	@Test
	public void testAcknowledgeAuthenticationAcceptedWithValidErrorMessage() throws Exception {
		// Given
		AuthenticationManagerImpl ami = upc.new AuthenticationManagerImpl(streamTaskListener);
		when(mockSVNErrorMessage.getCause()).thenReturn(new IOException());
		
		// When
		ami.acknowledgeAuthentication(false, "", "", mockSVNErrorMessage, mockSVNAuthentication);
		
		// Then
		assertEquals("Failed to authenticate: mockSVNErrorMessage", baos.toString().split("\n")[0].trim());
	}
	
	@Test
	public void testAcknowledgeAuthenticationAcceptedwithNullCause() throws Exception {
		// Given
		AuthenticationManagerImpl ami = upc.new AuthenticationManagerImpl(streamTaskListener);
		when(mockSVNErrorMessage.getCause()).thenReturn(null);
		
		// When
		ami.acknowledgeAuthentication(false, "", "", mockSVNErrorMessage, mockSVNAuthentication);
		
		// Then
		assertEquals("Failed to authenticate: mockSVNErrorMessage", baos.toString().split("\n")[0].trim());
	}	
	
	@Test (expected = SVNCancelException.class)
	public void testCheckIfProtocolCompletedWithNoAuthenticationAttempt() throws SVNCancelException {
		AuthenticationManagerImpl ami = upc. new AuthenticationManagerImpl(streamTaskListener);
		
		// This should fail when we try to check when there was no attempt at authentication.
		ami.checkIfProtocolCompleted();
	}

	@Test
	public void testCheckIfProtocolCompletedWithAuthenticationAttempt() throws SVNException {
		AuthenticationManagerImpl ami = upc. new AuthenticationManagerImpl(streamTaskListener);
		
		ami.getFirstAuthentication("svn.invalid", "", null);
		ami.checkIfProtocolCompleted();
	}
}
