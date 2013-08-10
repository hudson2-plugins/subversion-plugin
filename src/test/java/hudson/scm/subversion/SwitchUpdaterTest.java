package hudson.scm.subversion;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.console.ConsoleNote;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.SCMedItem;
import hudson.model.TaskListener;
import hudson.scm.AbstractSubversionTest;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.scm.SubversionSCM.RevisionPolicy;
import hudson.scm.subversion.WorkspaceUpdater.UpdateTask;
import hudson.scm.util.RevisionUtil;
import hudson.triggers.SCMTrigger;
import hudson.util.LogTaskListener;
import hudson.util.StreamTaskListener;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hudson.class, RevisionUtil.class})
public class SwitchUpdaterTest {
	@Mock Hudson hudson;
	@Mock RevisionUtil revisionUtil;
	@Mock ISVNAuthenticationProvider mockSVNAuthenticationProvider;
	@Mock TaskListener mockTaskListener;
	@Mock SVNClientManager mockSVNClientManager;
	@Mock SVNUpdateClient mockSVNUpdateClient;
	@Mock SvnOperationFactory mockSvnOperationFactory;
	@Mock SVNWCClient mockSVNWCClient;
	@Mock SVNInfo mockSVNInfo;
	@Mock SVNURL mockSVNURL;
	@Mock SVNException mockSVNException;
	@Mock SVNErrorMessage mockSVNErrorMessage;
	
	UpdateTask updateTask;
	ModuleLocation[] moduleLocations;
	RevisionPolicy revisionPolicy;
	
	@Before
	public void setUp() {
		updateTask = new SwitchUpdater().createTask();
		updateTask.setAuthProvider(mockSVNAuthenticationProvider);
		updateTask.setListener(mockTaskListener); 
		updateTask.setManager(mockSVNClientManager);
	}
	
	@Test
	public void testSwitchUpdaterDescriptor() {
		assertEquals("Use 'svn switch' as much as possible", new SwitchUpdater.DescriptorImpl().getDisplayName());
	}

	@Test
	public void testCreateTask() throws Exception {
		// When no moduleLocations are provided the default return on a perform is to return an empty list.
		moduleLocations = new ModuleLocation[0];
		updateTask.setLocations(moduleLocations);
		assertTrue(updateTask.perform().isEmpty());

		moduleLocations = new ModuleLocation[1];
		moduleLocations[0] = new ModuleLocation("https://test", "");
		updateTask.setLocations(moduleLocations);
		revisionPolicy = RevisionPolicy.HEAD;
		updateTask.setRevisionPolicy(revisionPolicy);
		updateTask.setWs(new File("/tmp/test"));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		when(mockTaskListener.getLogger()).thenReturn(new PrintStream(baos));
		when(mockSVNClientManager.getUpdateClient()).thenReturn(mockSVNUpdateClient);
		when(mockSVNUpdateClient.getOperationsFactory()).thenReturn(mockSvnOperationFactory);
		FileUtils.deleteDirectory(new File("/tmp/test")); // When workspace does not exist.
		updateTask.perform();
		String[] output = baos.toString().split("\n");
		assertEquals("Checking out a fresh workspace because /tmp/test/test doesn't exist", output[0]);
		assertEquals("Cleaning workspace /tmp/test", output[1]);
		assertEquals("Checking out https://test revision: HEAD depth:infinity ignoreExternals: false", output[2]);
		baos.reset();
		
		new File("/tmp/test").mkdir();
		new File("/tmp/test/test").createNewFile();
		
		// now that the workspace exists let's perform the switch if possible.
		when(mockSVNClientManager.getWCClient()).thenReturn(mockSVNWCClient);
		when(mockSVNWCClient.doInfo(new File("/tmp/test/test"), SVNRevision.WORKING)).thenReturn(mockSVNInfo);
		when(mockSVNInfo.getURL()).thenReturn(mockSVNURL);
		when(mockSVNURL.toDecodedString()).thenReturn("https://test");
		when(mockSVNInfo.getCommittedRevision()).thenReturn(SVNRevision.BASE);
		updateTask.perform();
		output = baos.toString().split("\n");
		assertEquals("Workspace is https://test. Using 'svn switch' to perform update.", output[0]);
		assertEquals("Switching https://test revision: HEAD depth:infinity ignoreExternals: false", output[1]);
		baos.reset();
		
		revisionPolicy = null;
		PowerMockito.mockStatic(RevisionUtil.class);
		when(revisionUtil.getRevision(moduleLocations[0], null, null, null, null)).thenReturn(null);
		
		updateTask.setRevisionPolicy(revisionPolicy);
		updateTask.perform();
		output = baos.toString().split("\n");
		assertEquals("Workspace is https://test. Using 'svn switch' to perform update.", output[0]);
		assertEquals("Switching https://test revision: null depth:infinity ignoreExternals: false", output[1]);
		baos.reset();
		
		when(mockSVNUpdateClient.doSwitch(new File("/tmp/test/test"),
										  moduleLocations[0].getSVNURL(),
										  SVNRevision.HEAD,
										  null,
										  SVNDepth.INFINITY,
										  true,
										  false)).thenThrow(mockSVNException);
		
		when(mockSVNException.getMessage()).thenReturn(UpdateTask.SVN_CANCEL_EXCEPTION_MESSAGE);
		try {
			updateTask.perform();
			fail(); // should throw an exception.
		} catch (InterruptedException e) {
			assertNotNull(e);
			baos.reset();
		}
		
		// When workspace is locked, we just fail the build instead of performing a fresh checkout.
		when(mockSVNException.getMessage()).thenReturn("");
		when(mockSVNException.getErrorMessage()).thenReturn(mockSVNErrorMessage);
		when(mockSVNErrorMessage.getErrorCode()).thenReturn(SVNErrorCode.WC_LOCKED);
		try {
			updateTask.perform();
			fail(); // should throw an exception.
		} catch (InterruptedException e) {
			assertNotNull(e);
			output = baos.toString().split("\n");
			assertEquals("Workspace appear to be locked, so Failing the build", output[2]);
			baos.reset();
		}

		// Assert an unknown error, then return a null.
		when(mockSVNErrorMessage.getErrorCode()).thenReturn(SVNErrorCode.WC_BAD_PATH);
		assertNull(updateTask.perform());
		baos.reset();
		
		// This test is for verifying that when a switch fails, we perform a fresh checkout.
		when(mockSVNErrorMessage.getErrorCode()).thenReturn(SVNErrorCode.WC_OBSTRUCTED_UPDATE);
		updateTask.perform();
		output = baos.toString().split("\n");
		assertEquals("Switch failed due to local files. Getting a fresh workspace", output[3]);
		assertEquals("Cleaning workspace /tmp/test", output[4]);
		assertEquals("Checking out https://test revision: null depth:infinity ignoreExternals: false", output[5]);
		baos.reset();
	}
}