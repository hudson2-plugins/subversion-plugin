package hudson.scm.subversion;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNEventFactory;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
/**
 * Not really a useful test...shhh :).
 * 
 * @author schristou88
 *
 */
public class SVNEventTest {
	@Test
	public void testSVNEvent() {
		SVNEvent event = new SVNEvent(new File("/tmp"), SVNNodeKind.FILE, null, -1, 
				null, null, null, null, SVNEventAction.PROPERTY_ADD, SVNEventAction.PROPERTY_ADD, null, null, null, null, null);
		
		SVNExternal prev = new SVNExternal("", "https://test", SVNRevision.BASE, SVNRevision.HEAD, false, false, false);
		SVNExternal _new = new SVNExternal("", "https://test", SVNRevision.BASE, SVNRevision.HEAD, false, false, false);
		event = event.setExternalInfo(prev, _new);
		assertEquals(event.getPreviousExternalInfo().toString(), event.getExternalInfo().toString());
	}
}