/**
 * 
 */
package hudson.scm.browsers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.model.Hudson;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;

import java.io.File;
import java.io.IOException;
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
@PrepareForTest({Hudson.class})
public class FishEyeSVNTest {
	@Mock
	Hudson hudson;
	
	@Test
	public void testChangeSetLink() throws IOException {
		
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
	@Email("http://dev.eclipse.org/mhonarc/lists/hudson-dev/msg00374.html")
	public void emptyrootModule() throws Exception{
		FishEyeSVN svn = new FishEyeSVN(new URL("https://test"), ""); // Validate no exception when no root module is provided.
	}
	
	
	@Test
	public void testDescriptorImplWithPermissions() throws Exception {
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
		assertTrue(Kind.ERROR.equals(descriptor.doCheckUrl("/browse/foobar/").getKind()));
		assertTrue(Kind.ERROR.equals(descriptor.doCheckUrl("/browse/foobar").getKind()));
		
		
		assertTrue(Kind.ERROR.equals(descriptor.doCheckUrl("http://fisheye5.cenqua.com/browse/glassfish/").getKind()));
	}
}
