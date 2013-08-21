/**
 * 
 */
package hudson.scm.listtagsparameter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.util.VariableResolver;

import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author schristou88
 *
 */
public class ListSubversionTagsParameterValueTest {
	@Test
	public void testConstruction() {
		String expectedName = "Test";
		String expectedTagsDir = "/tmp";
		String expectedTag = "TEST";
		
		ListSubversionTagsParameterValue svnTagParamVal = new ListSubversionTagsParameterValue(expectedName, "/usr", "asdf");
		svnTagParamVal.setTag(expectedTag);
		svnTagParamVal.setTagsDir(expectedTagsDir);
		
		assertEquals(expectedName, svnTagParamVal.getName());
		assertEquals(expectedTagsDir, svnTagParamVal.getTagsDir());
		assertEquals(expectedTag, svnTagParamVal.getTag());
	}
	
	
	@Test
	public void testBuildEnv() {
		String expectedName = "Test";
		String expectedTagsDir = "/tmp";
		String expectedTag = "TEST";
		
		AbstractBuild mockBuild = mock(AbstractBuild.class);
		
		EnvVars env = new EnvVars();
		ListSubversionTagsParameterValue svnTagParamVal = new ListSubversionTagsParameterValue(expectedName, expectedTagsDir, expectedTag);
		svnTagParamVal.buildEnvVars(mockBuild, env);
		
		assertTrue(env.containsKey(expectedName));
		assertTrue(env.containsValue(expectedTag));
	}
	
	@Test
	public void testCreateVariableResolver() {
		String expectedName = "Test";
		String expectedTagsDir = "/tmp";
		String expectedTag = "TEST";
		
		ListSubversionTagsParameterValue svnTagParamVal = new ListSubversionTagsParameterValue(expectedName, expectedTagsDir, expectedTag);
		
		AbstractBuild mockBuild = mock(AbstractBuild.class);
		assertEquals(expectedTag, svnTagParamVal.createVariableResolver(mockBuild).resolve(expectedName));
		assertNull(svnTagParamVal.createVariableResolver(mockBuild).resolve(expectedTag));
	}
}