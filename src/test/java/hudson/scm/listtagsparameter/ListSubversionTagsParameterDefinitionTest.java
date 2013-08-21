package hudson.scm.listtagsparameter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.scm.AbstractSubversionTest;
import hudson.scm.SubversionSCM;

import net.sf.json.JSONObject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Hudson.class)
public class ListSubversionTagsParameterDefinitionTest {
	@Test
	public void testInvalidUUID() {
		new ListSubversionTagsParameterDefinition("", "", null);		
	}
	
	@Test
	public void testInvalidUUID2() {
		assertEquals(new ListSubversionTagsParameterDefinition("", "", "").compareTo(
				new ListSubversionTagsParameterDefinition("", "", "")), -1);
	}

	@Test
	public void testConstruction() {
		String expectedName = "";
		String expectedTagsDir = "/tmp";
		String expectedUUID = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
		ListSubversionTagsParameterDefinition svnDef = new ListSubversionTagsParameterDefinition(expectedName, expectedTagsDir, expectedUUID);
		ListSubversionTagsParameterDefinition svnDef2 = new ListSubversionTagsParameterDefinition(expectedName, expectedTagsDir, expectedUUID);
		assertEquals(svnDef.compareTo(svnDef2), 0);
	}


	@Test
	public void testCreateValue() {
		String expectedName = "Test";
		String expectedTagsDir = "/tmp";
		String expectedUUID = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
		
		StaplerRequest mockReq = mock(StaplerRequest.class);
		
		// When perfect
		when(mockReq.getParameterValues(expectedName)).thenReturn(new String[]{expectedUUID});
		
		ListSubversionTagsParameterDefinition svnDef = new ListSubversionTagsParameterDefinition(expectedName, expectedTagsDir, expectedUUID);
		assertEquals(expectedUUID, ((ListSubversionTagsParameterValue)svnDef.createValue(mockReq)).getTag());
		
		
		// When invalid or no parameter values
		when(mockReq.getParameterValues(anyString())).thenReturn(null);
		assertNull(svnDef.createValue(mockReq));
		
		when(mockReq.getParameterValues(anyString())).thenReturn(new String[]{expectedName, expectedTagsDir, expectedUUID});
		assertNull(svnDef.createValue(mockReq));
	}
	
	@Test
	public void testCreateValueFromBuildButton() {
		String expectedName = "Test";
		String expectedTagsDir = "/tmp";
		String expectedTag = "TEST";
		String expectedUUID = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
		
		StaplerRequest req = mock(StaplerRequest.class);
		when(req.bindJSON(ListSubversionTagsParameterValue.class, null)).thenReturn(new ListSubversionTagsParameterValue(expectedName, expectedTagsDir, expectedTag));
		
		ListSubversionTagsParameterDefinition svnDef = new ListSubversionTagsParameterDefinition(expectedName, expectedTagsDir, expectedUUID);
		
		ListSubversionTagsParameterValue param = (ListSubversionTagsParameterValue) svnDef.createValue(req, null); 
		assertEquals(expectedName, param.getName());
		assertEquals(expectedTagsDir, param.getTagsDir());
		assertEquals(expectedTag, param.getTag());
	}

	@Test
	public void testGetTags() {
		String expectedName = "Test";
		String expectedTagsDir = "http://tsethudsonsvn.googlecode.com/svn/tags";
		String expectedTag = "TEST";
		String expectedUUID = "f81d4fae-7dec-11d0-a765-00a0c91e6bf6";
		String unExpectedUUID = "6ad36170-9044-11e2-9e96-0800200c9a66";
		
		ListSubversionTagsParameterDefinition svnDef = new ListSubversionTagsParameterDefinition(expectedName, expectedTagsDir, expectedUUID);
		when(hudson.getItems(AbstractProject.class)).thenReturn(new ArrayList<AbstractProject>());
		when(hudson.getRootDir()).thenReturn(new File("/tmp"));
		when(hudson.getDescriptor(SubversionSCM.class)).thenReturn(mockDescriptor);
		PowerMockito.mockStatic(Hudson.class);
		when(Hudson.getInstance()).thenReturn(hudson);
		
		when(svnDef.getDescriptor()).thenReturn(new ListSubversionTagsParameterDefinition.DescriptorImpl());
		
		List<String> tags = svnDef.getTags();
		assertFalse(tags.isEmpty());
		
		ArrayList<AbstractProject> mockProjects = new ArrayList<AbstractProject>();
		when(hudson.getItems(AbstractProject.class)).thenReturn(mockProjects);
		
		// When the project does not contain any parameters (should just ignore it) 
		mockProjects.add(mockProject);
		when(mockProject.getProperty(ParametersDefinitionProperty.class)).thenReturn(null);
		tags = svnDef.getTags(); // Should throw an exception when there are no projects available.
		assertFalse(tags.isEmpty());
		
		// Weird issue where there's a null parameter definition or an invalid one.		
		mockProjects = new ArrayList<AbstractProject>();
		mockProjects.add(mockProject);
		when(mockProject.getProperty(ParametersDefinitionProperty.class)).thenReturn(mockParametersDefinitionProperty);
		when(mockParametersDefinitionProperty.getParameterDefinitions()).thenReturn(null);
		tags = svnDef.getTags();
		assertFalse(tags.isEmpty());
		
		
		// Now let's add some parameter definitions to the project.
		ArrayList mockProperties = new ArrayList();
		mockProperties.add(new ParameterDefinitionProxy("test"));
		when(mockParametersDefinitionProperty.getParameterDefinitions()).thenReturn(mockProperties);
		
		tags = svnDef.getTags();
		assertFalse(tags.isEmpty());
		
		
		// A perfect instance.
		mockProperties = new ArrayList();
		mockProperties.add(new ParameterDefinitionProxy("test"));
		mockProperties.add(new ListSubversionTagsParameterDefinition("expectedName", "expectedTagsDir", unExpectedUUID));
		mockProperties.add(svnDef);
		when(mockParametersDefinitionProperty.getParameterDefinitions()).thenReturn(mockProperties);
		tags = svnDef.getTags();
		assertFalse(tags.isEmpty());
		
		svnDef = new ListSubversionTagsParameterDefinition(expectedName, "/tmp", expectedUUID);
		tags = svnDef.getTags();
		assertEquals("&lt;An SVN exception occurred while listing the directory entries.&gt;", tags.get(0));
	}
	
	@Mock
	Hudson hudson;
	@Mock
	SubversionSCM.DescriptorImpl mockDescriptor;
	@Mock
	FreeStyleProject mockProject;
	@Mock
	ParametersDefinitionProperty mockParametersDefinitionProperty;
	
	public class ParameterDefinitionProxy extends ParameterDefinition {
		public ParameterDefinitionProxy(String name) {super(name);}
		public ParameterValue createValue(StaplerRequest req, JSONObject jo) {return null;}
		public ParameterValue createValue(StaplerRequest req) {return null;}
	}
}