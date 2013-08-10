package hudson.scm.browsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.scm.EditType;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import hudson.scm.SubversionChangeLogSet.Path;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class WebSVNTest {
	@Mock
	Path mockPath;
	@Mock
	LogEntry mockLogEntry;
	
	WebSVN svn;

	@Before
	public void setUp() throws MalformedURLException {
		MockitoAnnotations.initMocks(this);
		svn = new WebSVN(new URL("https://test"));
	}

	@Test
	public void testGetDiffLinkPath() throws IOException {
		// Verify that we don't allow for diff link on anything other than edit type.
		when(mockPath.getEditType()).thenReturn(EditType.ADD);
		assertNull(svn.getDiffLink(mockPath));
		when(mockPath.getEditType()).thenReturn(EditType.DELETE);
		assertNull(svn.getDiffLink(mockPath));
		
		when(mockPath.getEditType()).thenReturn(EditType.EDIT);
		when(mockPath.getLogEntry()).thenReturn(mockLogEntry);
		when(mockLogEntry.getRevision()).thenReturn(1);
		when(mockPath.getValue()).thenReturn("https://test/");
		
		assertEquals("https://test/?op=diff&rev=1", svn.getDiffLink(mockPath).toString());
	}

	@Test
	public void testGetFileLinkPath() throws IOException {
		when(mockPath.getValue()).thenReturn("http://svn.apache.org");
		
		assertEquals("http://svn.apache.org", svn.getFileLink(mockPath).toString());
	}

	@Test
	public void testGetChangeSetLinkLogEntry() throws IOException {
		when(mockPath.getValue()).thenReturn("http://svn.apache.org");
		assertEquals("https://test/?rev=0&sc=1", svn.getChangeSetLink(mockLogEntry).toString());
	}

	@Test
	public void testDescriptor() {
		WebSVN.DescriptorImpl descriptor = new WebSVN.DescriptorImpl();
		assertEquals("WebSVN", descriptor.getDisplayName());
	}
}
