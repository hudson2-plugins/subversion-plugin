package hudson.scm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import hudson.model.AbstractBuild;
import hudson.scm.SubversionSCM.SvnInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SubversionTagActionTest {

	@Mock
	AbstractBuild mockBuild;
	@Mock
	Iterator<SvnInfo> mockIterator;
	@Mock
	Collection<SvnInfo> mockCollection;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(mockCollection.iterator()).thenReturn(mockIterator);
		when(mockIterator.next()).thenReturn(new SvnInfo("https://test", 12345), new SvnInfo("http://test", 67890));
		when(mockIterator.hasNext()).thenReturn(true, true, false);
	}
	
	
	@Test
	public void testVerifyTags() {
		SubversionTagAction tagAction = new SubversionTagAction(mockBuild, mockCollection);
		Map<SvnInfo, List<String>> tags = tagAction.getTags();
		
		assertTrue(tags.containsKey(new SvnInfo("https://test", 12345)));
		assertTrue(tags.containsKey(new SvnInfo("http://test", 67890)));
	}
	
	@Test
	public void testIsTagged() {
		SubversionTagAction tagAction = new SubversionTagAction(mockBuild, mockCollection);
		assertFalse(tagAction.isTagged());
	}
}
