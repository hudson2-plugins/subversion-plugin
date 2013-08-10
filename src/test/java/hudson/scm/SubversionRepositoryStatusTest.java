package hudson.scm;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Test;

public class SubversionRepositoryStatusTest {

	@Test
	public void testRepositoryStatusUUID() {
		UUID expectedUUID = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6");

		SubversionRepositoryStatus repoStatus = new SubversionRepositoryStatus(expectedUUID);
		assertEquals(expectedUUID.toString(), repoStatus.getDisplayName());
		assertEquals(expectedUUID.toString(), repoStatus.getSearchUrl());
	}
	
	
	@Test
	public void testNotifyCommit() {
		
	}
}
