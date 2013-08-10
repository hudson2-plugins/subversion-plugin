/**
 * 
 */
package hudson.scm.browsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.model.Hudson;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import hudson.scm.SubversionChangeLogSet.Path;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Email;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author schristou88
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Hudson.class, RepositoryBrowser.class})
public class FishEyeSVNTest {
	@Mock
	Hudson hudson;

	@Test
	public void testChangeSetLink() throws Exception {
		
		LogEntry mockLogEntry = mock(LogEntry.class);
		when(mockLogEntry.getRevision()).thenReturn(1);
		
		FishEyeSVN svn = new FishEyeSVN(new URL("https://test"), "foo/bar");
		URL returnURL = svn.getChangeSetLink(mockLogEntry);
		assertNotNull(returnURL);
		
		assertTrue("The url does not contain the revision number",
				returnURL.toString().contains(Integer.valueOf(mockLogEntry.getRevision()).toString()));
		
		assertEquals("Urls do not match", "https://test/../../changelog//?cs=1", returnURL.toString());
	}
	
	@Test
	@Email("http://dev.eclipse.org/mhonarc/lists/hudson-dev/msg00373.html")
	public void emptyrootModule() throws Exception{
		FishEyeSVN svn = new FishEyeSVN(new URL("https://test"), ""); // Validate no exception when no root module is provided.
	}
	
	
	@Test
	public void testCheckURL() throws Exception {
		// Mock hudson
		when(hudson.hasPermission(Hudson.ADMINISTER)).thenReturn(true);
		PowerMockito.mockStatic(Hudson.class);
		when(Hudson.getInstance()).thenReturn(hudson);
		
		
		FishEyeSVN.DescriptorImpl descriptor = new FishEyeSVN.DescriptorImpl();		
		
		// Verify Display Name doesn't change.
		assertEquals("FishEye", descriptor.getDisplayName());
		
		// First check for null value.
		assertEquals(FormValidation.ok(), descriptor.doCheckUrl(null));
		
		// Verify error thrown when invalid url. 
		assertEquals(Kind.ERROR, descriptor.doCheckUrl("/browse/foobar/").getKind());
		assertEquals(Kind.ERROR, descriptor.doCheckUrl("/browse/foobar").getKind());
				
		
		// First verify that someone without administrator permissions it does not try to connect.
		when(hudson.hasPermission(Hudson.ADMINISTER)).thenReturn(false);
		assertEquals(Kind.OK, descriptor.doCheckUrl("http://fisheye5.cenqua.com/browse/glassfish/").getKind());
		
		
		// Now verify when user has permissions to edit the url page.
		when(hudson.hasPermission(Hudson.ADMINISTER)).thenReturn(true);
		assertEquals(Kind.ERROR, descriptor.doCheckUrl("http://fisheye5.cenqua.com/browse/glassfish/").getKind());
		
		assertEquals(Kind.ERROR, descriptor.doCheckUrl("fisheye5.cenqua.com/browse/glassfish/").getKind());
	}
	
	@Test
	public void testRootModule() throws Exception {
		String expectedRootModule = "foo/bar";
		FishEyeSVN svn = new FishEyeSVN(new URL("https://test"), expectedRootModule);
		assertEquals(expectedRootModule, svn.getRootModule());
	}
	
	@Test
	public void testDifferentLink() throws Exception {
		FishEyeSVN svn = new FishEyeSVN(new URL("https://test"), "foo/bar");
		
		Path path = mock(Path.class);
		LogEntry logEntry = mock(LogEntry.class);
		// If edit type is add or delete, then we don't continue. Otherwise we obtain the link.
		when(path.getEditType()).thenReturn(EditType.ADD);
		assertNull(svn.getDiffLink(path));
		
		when(path.getEditType()).thenReturn(EditType.DELETE);
		assertNull(svn.getDiffLink(path));
				
		when(path.getEditType()).thenReturn(EditType.EDIT);
		when(path.getValue()).thenReturn("foo/bar/foo.c");
		when(path.getLogEntry()).thenReturn(logEntry);
		when(logEntry.getRevision()).thenReturn(Integer.valueOf(100));
		assertEquals("https://test/foo.c?r1=99&r2=100", svn.getDiffLink(path).toString());
	}
	
	@Test
	public void testFileLink() throws IOException {
		FishEyeSVN svn = new FishEyeSVN(new URL("https://test"), "/foo/bar/");
		
		Path path = mock(Path.class);
		when(path.getValue()).thenReturn("/test/trunk/foo.c");
		
		assertEquals("https://test/test/trunk/foo.c", svn.getFileLink(path).toString());
		
		when(path.getValue()).thenReturn("foo/bar/foo.c");
		assertEquals("https://test/foo.c", svn.getFileLink(path).toString());
	}
	
	// We do this one just because creating a new URL each time can slow down other unit tests.
	@Test
	public void testRealFishEyeRepository() throws Exception {
		when(hudson.hasPermission(Hudson.ADMINISTER)).thenReturn(true);
		PowerMockito.mockStatic(Hudson.class);
		when(Hudson.getInstance()).thenReturn(hudson);
		
		
		FishEyeSVN svn = new FishEyeSVN(new URL("https://fisheye.codehaus.org"), "/browse/mojo/");
		FishEyeSVN.DescriptorImpl descriptor = new FishEyeSVN.DescriptorImpl();
		assertEquals(FormValidation.ok(), descriptor.doCheckUrl("https://fisheye.codehaus.org/browse/mojo/"));
	}
}