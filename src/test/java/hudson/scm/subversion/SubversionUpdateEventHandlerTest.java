package hudson.scm.subversion;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.scm.SubversionSCM.External;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class SubversionUpdateEventHandlerTest {
	@Mock
	SVNEvent mockEvent;
	@Mock
	SVNExternal mockSVNExternal;
	@Mock
	SVNURL mockSVNURL;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testHandleEvent() throws Exception {
		new File("/tmp/test.txt").createNewFile();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		
		File moduleDir = new File("");
		String modulePath = "";
		
		SubversionUpdateEventHandler updateEventHandler  = new SubversionUpdateEventHandler(out, 
																							new ArrayList<External>(),
																							moduleDir,
																							modulePath);
		double progress = 0.;
		
		// When we don't have any externals to update, just update regular.
		when(mockEvent.getFile()).thenReturn(new File("/tmp/tests.txt"));
		updateEventHandler.handleEvent(mockEvent, progress);
		
		
		when(mockEvent.getAction()).thenReturn(SVNEventAction.UPDATE_EXTERNAL);
		updateEventHandler.handleEvent(mockEvent, progress);
		assertFalse(baos.toString().isEmpty());
		assertTrue(baos.toString().contains("AssertionError: appears to be using unpatched svnkit at "));
		baos.reset();
		
		when(mockSVNExternal.getPath()).thenReturn("/tmp");
		when(mockSVNExternal.getRevision()).thenReturn(SVNRevision.HEAD);
		when(mockSVNExternal.getResolvedURL()).thenReturn(mockSVNURL);
		when(mockEvent.getExternalInfo()).thenReturn(mockSVNExternal);
		updateEventHandler.handleEvent(mockEvent, progress);
		assertFalse(baos.toString().isEmpty());
		assertTrue(baos.toString().contains("Fetching 'mockSVNURL' at -1 into '/tmp/tests.txt'"));
		baos.reset();
		
		when(mockEvent.getAction()).thenReturn(SVNEventAction.SKIP);
		updateEventHandler.handleEvent(mockEvent, progress);
		assertTrue(baos.toString().isEmpty());
		
		when(mockEvent.getExpectedAction()).thenReturn(SVNEventAction.UPDATE_EXTERNAL);
		updateEventHandler.handleEvent(mockEvent, progress);
		assertTrue(baos.toString().isEmpty());
		
		when(mockEvent.getNodeKind()).thenReturn(SVNNodeKind.FILE);
		updateEventHandler.handleEvent(mockEvent, progress);
		assertFalse(baos.toString().isEmpty());
		assertTrue(baos.toString().contains("svn:externals to a file requires Subversion 1.6 workspace support. Use the system configuration to enable that."));
	}
}
