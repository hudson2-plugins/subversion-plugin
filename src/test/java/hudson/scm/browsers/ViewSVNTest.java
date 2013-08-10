package hudson.scm.browsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.scm.EditType;
import hudson.scm.SubversionChangeLogSet;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import hudson.scm.SubversionChangeLogSet.Path;

import java.io.IOException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ViewSVNTest {
	@Mock
	Path mockPath;
	@Mock
	LogEntry mockLogEntry;
	
	ViewSVN svn;
	
	@Before
	public void setUp() throws IOException{
		MockitoAnnotations.initMocks(this);
		svn = new ViewSVN(new URL("https://server/repo"));
	}
	
	@Test
	public void testDiffLink() throws IOException {
		when(mockPath.getEditType()).thenReturn(EditType.ADD);
		assertNull(svn.getDiffLink(mockPath));
		when(mockPath.getEditType()).thenReturn(EditType.DELETE);
		assertNull(svn.getDiffLink(mockPath));
		
		when(mockPath.getEditType()).thenReturn(EditType.EDIT);
		when(mockPath.getLogEntry()).thenReturn(mockLogEntry);
		when(mockLogEntry.getRevision()).thenReturn(1);
		when(mockPath.getValue()).thenReturn("/test/trunk/foo.c");
		assertEquals("https://server/repo/test/trunk/foo.c?r1=0&r2=1", svn.getDiffLink(mockPath).toString());
	}
	
	@Test
	public void testFileLink() throws IOException {
		when(mockPath.getValue()).thenReturn("/test/trunk/foo.c");
		assertEquals("https://server/repo/test/trunk/foo.c", svn.getFileLink(mockPath).toString());
	}
	
	@Test
	public void testChangeSetLink() throws IOException {
		when(mockLogEntry.getRevision()).thenReturn(1);
		assertEquals("https://server/repo/?view=rev&rev=1", svn.getChangeSetLink(mockLogEntry).toString());
	}
	
	@Test
	public void testDescriptor() {
		ViewSVN.DescriptorImpl descriptor = new ViewSVN.DescriptorImpl();
		assertEquals("ViewSVN", descriptor.getDisplayName());
	}	
}
