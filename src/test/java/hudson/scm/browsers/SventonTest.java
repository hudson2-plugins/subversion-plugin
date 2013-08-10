package hudson.scm.browsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.scm.EditType;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import hudson.scm.SubversionChangeLogSet.Path;
import hudson.scm.browsers.AbstractSventon.SventonUrlChecker;
import hudson.util.FormValidation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

public class SventonTest {
	@Mock
	Path mockPath;
	@Mock
	LogEntry mockLogEntry;
	@Mock
	AbstractProject mockProject;
	
	Sventon sventon;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		sventon = new Sventon(new URL("https://test"), "");
	}
	
	@Test
	public void testSventonDiffLink() throws IOException {
		when(mockPath.getEditType()).thenReturn(EditType.ADD);
		assertNull(sventon.getDiffLink(mockPath));
		when(mockPath.getEditType()).thenReturn(EditType.DELETE);
		assertNull(sventon.getDiffLink(mockPath));
		
		when(mockPath.getEditType()).thenReturn(EditType.EDIT);
		when(mockLogEntry.getRevision()).thenReturn(1);
		when(mockPath.getLogEntry()).thenReturn(mockLogEntry);
		when(mockPath.getValue()).thenReturn("https://sventon");
		assertEquals("https://test/diffprev.svn?name=&commitrev=1&committedRevision=1&revision=1&path=https%3A%2F%2Fsventon", sventon.getDiffLink(mockPath).toString());
	}
	
	@Test
	public void testFileLink() throws Exception {
		when(mockPath.getEditType()).thenReturn(EditType.DELETE);
		assertNull(sventon.getFileLink(mockPath));
		
		when(mockPath.getEditType()).thenReturn(EditType.EDIT);
		when(mockLogEntry.getRevision()).thenReturn(1);
		when(mockPath.getLogEntry()).thenReturn(mockLogEntry);
		when(mockPath.getValue()).thenReturn("https://sventon");

		assertEquals("https://test/goto.svn?name=&revision=1&path=https%3A%2F%2Fsventon", sventon.getFileLink(mockPath).toString());
	}
	
	@Test
	public void testChangeSetLink() throws IOException {
		when(mockLogEntry.getRevision()).thenReturn(1);
		
		assertEquals("https://test/revinfo.svn?name=&revision=1", sventon.getChangeSetLink(mockLogEntry).toString());
	}
	
	@Test
	public void testSventonDescriptor() throws Exception {
		Sventon.DescriptorImpl sventonDescriptor = new Sventon.DescriptorImpl();
		assertEquals("Sventon 1.x", sventonDescriptor.getDisplayName());
		
		when(mockProject.hasPermission(Item.CONFIGURE)).thenReturn(false); // Don't have permission so Ignore.
		assertEquals(FormValidation.ok(), sventonDescriptor.doCheckUrl(mockProject, null));
		
		when(mockProject.hasPermission(Item.CONFIGURE)).thenReturn(true); // Now we automagically got permissions :)
		assertEquals(FormValidation.ok(), sventonDescriptor.doCheckUrl(mockProject, null));
		
		assertEquals(FormValidation.Kind.ERROR,
				sventonDescriptor.doCheckUrl(mockProject, "http://svn.sventon.org/repos/gc/list/").getKind());
	}
}