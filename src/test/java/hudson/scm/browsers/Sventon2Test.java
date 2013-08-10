package hudson.scm.browsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import hudson.scm.SubversionChangeLogSet.Path;
import hudson.scm.browsers.AbstractSventon.SventonUrlChecker;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class Sventon2Test {
	@Mock
	Path mockPath;
	@Mock
	LogEntry mockLogEntry;
	@Mock
	AbstractProject mockProject;
	
	Sventon2 sventon2;
	Sventon2.DescriptorImpl sventon2Descriptor;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		sventon2 = new Sventon2(new URL("https://test"), "/trunk/");
		sventon2Descriptor = new Sventon2.DescriptorImpl();
	}
	
	@Test
	public void testCreation() throws Exception {
		assertEquals("/trunk/", sventon2.getRepositoryInstance());
	}
	
	@Test
	public void testGetDiffLinkPath() throws Exception {
		// We only care about when the the path is editable, so we need to ignore everything else.
		when(mockPath.getEditType()).thenReturn(EditType.ADD);
		assertNull(sventon2.getDiffLink(mockPath));
		when(mockPath.getEditType()).thenReturn(EditType.DELETE);
		assertNull(sventon2.getDiffLink(mockPath));
		
		when(mockPath.getEditType()).thenReturn(EditType.EDIT);
		when(mockPath.getLogEntry()).thenReturn(mockLogEntry);
		when(mockLogEntry.getRevision()).thenReturn(1);
		when(mockPath.getValue()).thenReturn("/test/trunk/foo.c");
		
		assertEquals("https://test/repos///trunk//diff/test/trunk/foo.c?revision=1", sventon2.getDiffLink(mockPath).toString());
	}
	
	@Test
	public void testGetFileLinkPath() throws Exception {
		when(mockPath.getEditType()).thenReturn(EditType.DELETE);
		assertNull("No file if it's already gone.", sventon2.getFileLink(mockPath));
		
		when(mockPath.getEditType()).thenReturn(EditType.EDIT);
		when(mockPath.getValue()).thenReturn("/test/trunk/foo.c");
		when(mockPath.getLogEntry()).thenReturn(mockLogEntry);
		when(mockLogEntry.getRevision()).thenReturn(1);
		
		assertEquals("https://test/repos///trunk//goto/test/trunk/foo.c?revision=1", sventon2.getFileLink(mockPath).toString());
	}
	
	
	@Test
	public void testGetChangeSetLink() throws Exception {
		when(mockLogEntry.getRevision()).thenReturn(1);
		assertEquals("https://test/repos///trunk//info?revision=1", sventon2.getChangeSetLink(mockLogEntry).toString());
	}
	
	@Test
	public void testDescriptor() throws Exception {
		assertEquals("Sventon 2.x", sventon2Descriptor.getDisplayName()); // Verify we are using sventon 2. Kind useless....shutup!
		
		when(mockProject.hasPermission(Item.CONFIGURE)).thenReturn(false); // When user dosen't have permission to go to configure.
		assertEquals("When user doesn't have permission we can't check.", FormValidation.ok(), sventon2Descriptor.doCheckUrl(mockProject, ""));
		
		when(mockProject.hasPermission(Item.CONFIGURE)).thenReturn(true); // Now we get permissions.
		assertEquals("Nothing entered, so should be OK", FormValidation.ok(), sventon2Descriptor.doCheckUrl(mockProject, null));
		
		// The only time this will ever fail is if the sventon repository upgrades to a version beyond 2, or an issue connecting to the internet.
		assertEquals(FormValidation.ok(), sventon2Descriptor.doCheckUrl(mockProject, "http://svn.sventon.org/repos/gc/list/"));
		
		// Let's test when invalid url type (no protocol).
		assertEquals(FormValidation.Kind.ERROR, sventon2Descriptor.doCheckUrl(mockProject, "sventon.sventon.org/repos/gc/list").kind);
		assertEquals(FormValidation.Kind.ERROR, sventon2Descriptor.doCheckUrl(mockProject, "https://server").kind);
		
	}
}
