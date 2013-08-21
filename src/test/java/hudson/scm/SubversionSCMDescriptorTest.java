package hudson.scm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import hudson.EnvVars;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.scm.SubversionSCM.DescriptorImpl;
import hudson.scm.subversion.WorkspaceUpdater;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;

import java.util.List;

import net.sf.json.JSONObject;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;

/**
 * This is the integration testing portion for the SubversionSCM class.
 * 
 * Please note these tests will take longer to execute however they do real
 * world scenario testing.
 * 
 * @author schristou88
 *
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(Hudson.class)

public class SubversionSCMDescriptorTest {
	SubversionSCM scm;
	String excludedRegions;
	String excludedUsers;
	String excludedRevprop;
	String excludedCommitMessages;
	String includedRegions;
	@Mock Hudson hudson;
	@Mock StaplerRequest mockStaplerRequest;
	@Mock SubversionSCM mockSubversionSCM;
	@Mock SubversionSCM mockSubversionSCM2;
	
	@Before
	public void setUp() {
		PowerMockito.mockStatic(Hudson.class);
		when(Hudson.getInstance()).thenReturn(hudson);
		
	}
	
	@Test
	public void testGetDefaultWorkspaceFormat() throws Exception {
		SubversionSCM.DescriptorImpl descriptorImpl = new DescriptorImpl();

		assertEquals("Default workspace format has changed.", 8, descriptorImpl.getWorkspaceFormat());
	}
	
	@Test
	public void testGetInvalidWorkspaceFormat() throws Exception {
		SubversionSCM.DescriptorImpl descriptorImpl = new DescriptorImpl();
		JSONObject mockJSONObject = new JSONObject();
		
		when(mockStaplerRequest.getParameter("svn.workspaceFormat")).thenReturn("0");
		try {
			descriptorImpl.configure(mockStaplerRequest, mockJSONObject);
		} catch (Exception e) {
			// We don't care about exception being created
			// We only care that workspaceFormat is invalid
		}
		
		assertEquals(SVNAdminAreaFactory.WC_FORMAT_14, descriptorImpl.getWorkspaceFormat());
	}
	
	@Test
	public void testIsBrowserReusable() {
		when(mockSubversionSCM.getLocations()).thenReturn(new ModuleLocation[] {new ModuleLocation("", "")});
		when(mockSubversionSCM2.getLocations()).thenReturn(new ModuleLocation[] {new ModuleLocation("", "")});
		
		SubversionSCM.DescriptorImpl descriptorImpl = new DescriptorImpl();
		assertTrue("URLS are the same it should be reuseable.",
				   descriptorImpl.isBrowserReusable(mockSubversionSCM, mockSubversionSCM2));
	}
	
//	@Test
//	public void testConfigure() {
//		
//		
//		
//	}
//	
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
//	@Test public void test() {}
}
