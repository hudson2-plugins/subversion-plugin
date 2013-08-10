package hudson.scm.browsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.model.AbstractProject;
import hudson.scm.SubversionChangeLogSet;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import hudson.scm.SubversionChangeLogSet.Path;
import hudson.util.FormValidation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.Test;
import org.kohsuke.stapler.DataBoundConstructor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class WebSVN2Test {
	@Mock
	Path mockPath;
	@Mock
	LogEntry mockLogEntry;
	@Mock
	AbstractProject mockProject;
	
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testInvalidURL() throws IOException {
		WebSVN2 svn = new WebSVN2(new URL("https://server/websvn"));
		
		when(mockPath.getValue()).thenReturn("/test/trunk/foo.c");
		when(mockPath.getLogEntry()).thenReturn(mockLogEntry);
		when(mockLogEntry.getRevision()).thenReturn(1);
		assertEquals("https://server/diff.php? path=%2Ftest%2Ftrunk%2Ffoo.c&rev=  1",
				svn.getDiffLink(mockPath).toString());
	}
	
	@Test
	public void testDiffLink() throws IOException {
		WebSVN2 svn = new WebSVN2(new URL("https://server/websvn/comp.php?repname=rep&compare[]=/@2222&compare[]=/@2225"));
		
		when(mockPath.getValue()).thenReturn("/test/trunk/foo.c");
		when(mockPath.getLogEntry()).thenReturn(mockLogEntry);
		when(mockLogEntry.getRevision()).thenReturn(1);
		assertEquals("https://server/websvn/diff.php?repname=rep&path=%2Ftest%2Ftrunk%2Ffoo.c&rev=  1",
				svn.getDiffLink(mockPath).toString());
	}
	
	@Test
	public void testFileLink() throws IOException {
		WebSVN2 svn = new WebSVN2(new URL("https://server/websvn/comp.php?repname=rep&compare[]=/@2222&compare[]=/@2225"));
		
		when(mockPath.getValue()).thenReturn("/test/trunk/foo.c");
		when(mockPath.getLogEntry()).thenReturn(mockLogEntry);
		when(mockLogEntry.getRevision()).thenReturn(1);
		assertEquals("https://server/websvn/filedetails.php?repname=rep&path=%2Ftest%2Ftrunk%2Ffoo.c&rev=  1",
				svn.getFileLink(mockPath).toString());
	}
	
	@Test
	public void testChangeSetLink() throws IOException {
		WebSVN2 svn = new WebSVN2(new URL("https://server/websvn/comp.php?repname=rep&compare[]=/@2222&compare[]=/@2225"));
		
		when(mockLogEntry.getRevision()).thenReturn(1);
		assertEquals("https://server/websvn/revision.php?repname=rep&rev= 1",
				svn.getChangeSetLink(mockLogEntry).toString());
	}
	
	@Test
	public void testDescriptor() {
		WebSVN2.DescriptorImpl descriptor = new WebSVN2.DescriptorImpl();
		
		assertEquals("WebSVN2", descriptor.getDisplayName());
		
		assertEquals(FormValidation.ok(), descriptor.doCheckUrl(mockProject, null));
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckUrl(mockProject, "").kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrl(mockProject, "https://test").kind);
		assertEquals(FormValidation.ok(), descriptor.doCheckUrl(mockProject, "https://server/websvn/comp.php?repname=rep&compare[]=/@2222&compare[]=/@2225"));
		assertEquals(FormValidation.Kind.OK, descriptor.doCheckUrl(mockProject, "https://server/websvn/comp.php?&compare[]=/@2222&compare[]=/@2225").kind);
		assertEquals(FormValidation.Kind.ERROR, descriptor.doCheckUrl(mockProject, "server/websvn/comp.php?&compare[]=/@2222&compare[]=/@2225").kind);
	}
}
