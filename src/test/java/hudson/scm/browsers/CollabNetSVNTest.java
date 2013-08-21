package hudson.scm.browsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.scm.EditType;
import hudson.scm.SubversionChangeLogSet;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import hudson.scm.SubversionChangeLogSet.Path;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CollabNetSVNTest {
	@Mock
	Path mockPath;
	@Mock
	LogEntry mockLogEntry;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	/*
	 * 
	 * 
	    public URL getDiffLink(SubversionChangeLogSet.Path path) throws IOException {
        if (path.getEditType() != EditType.EDIT) {
            // No diff if the file is being added or deleted.
            return null;
        }

        int revision = path.getLogEntry().getRevision();
        QueryBuilder query = new QueryBuilder(null);
        query.add("r1=" + (revision - 1));
        query.add("r2=" + revision);
        return new URL(url, trimHeadSlash(path.getValue()) + query);
    }
	 * 
	 */
	@Test
	public void testGetDiffLink() throws IOException {
		CollabNetSVN svn = new CollabNetSVN(new URL("https://server/"));
		
		when(mockPath.getEditType()).thenReturn(EditType.ADD);
		assertNull(svn.getDiffLink(mockPath));
		when(mockPath.getEditType()).thenReturn(EditType.DELETE);
		assertNull(svn.getDiffLink(mockPath));
		
		when(mockPath.getEditType()).thenReturn(EditType.EDIT);
		when(mockPath.getLogEntry()).thenReturn(mockLogEntry);
		when(mockLogEntry.getRevision()).thenReturn(1);
		when(mockPath.getValue()).thenReturn("/test/trunk/foo.c");
		assertEquals("https://server/test/trunk/foo.c?r1=0&r2=1", svn.getDiffLink(mockPath).toString());
	}

	@Test
	public void testGetFileLink() throws IOException {
		CollabNetSVN svn = new CollabNetSVN(new URL("https://server/"));

		when(mockPath.getLogEntry()).thenReturn(mockLogEntry);
		when(mockLogEntry.getRevision()).thenReturn(1);
		when(mockPath.getValue()).thenReturn("/test/trunk/foo.c");
		assertEquals("https://server/test/trunk/foo.c?rev=1&view=log", svn.getFileLink(mockPath).toString());
	}

	@Test
	public void testGetChangeSetLinkLogEntry() throws IOException {
		CollabNetSVN svn = new CollabNetSVN(new URL("https://server/"));

		when(mockLogEntry.getRevision()).thenReturn(1);
		assertEquals("https://server/?rev=1&view=rev", svn.getChangeSetLink(mockLogEntry).toString());
	}
	
	@Test
	public void testDescriptor() {
		CollabNetSVN.DescriptorImpl descriptor = new CollabNetSVN.DescriptorImpl();
		assertEquals("CollabNet", descriptor.getDisplayName());
	}
	
}
