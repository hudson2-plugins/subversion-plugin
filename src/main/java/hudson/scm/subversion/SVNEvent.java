/**
 * 
 */
package hudson.scm.subversion;

import java.io.File;

import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNLock;
import org.tmatesoft.svn.core.SVNMergeRange;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatusType;

/**
 * @author Steven
 *
 */
public class SVNEvent extends org.tmatesoft.svn.core.wc.SVNEvent {

    private SVNExternal myPreviousExternalInfo;
    private SVNExternal myExternalInfo;

    public SVNEvent(File file, SVNNodeKind kind, String mimetype,
            long revision, SVNStatusType cstatus, SVNStatusType pstatus,
            SVNStatusType lstatus, SVNLock lock, SVNEventAction action,
            SVNEventAction expected, SVNErrorMessage error,
            SVNMergeRange range, String changelistName, SVNProperties revisionProperties, String propertyName) {
        super(file, kind, mimetype, revision, cstatus, pstatus, lstatus, lock, action,
                expected, error, range, changelistName, revisionProperties, propertyName);
    }

    public SVNExternal getExternalInfo() {
        return myExternalInfo;
    }

    public SVNExternal getPreviousExternalInfo() {
        return myPreviousExternalInfo;
    }

    public SVNEvent setExternalInfo(SVNExternal prev, SVNExternal _new) {
        this.myPreviousExternalInfo = prev;
        this.myExternalInfo = _new;
        return this;
    }
}
