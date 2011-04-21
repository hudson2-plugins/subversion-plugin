package hudson.scm;

import java.io.Serializable;
import java.util.Map;

/**
 * {@link SCMRevisionState} for {@link SubversionSCM}. {@link Serializable} since we compute
 * this remote.
 */
final class SVNRevisionState extends SCMRevisionState implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * All the remote locations that we checked out. This includes those that are specified
     * explicitly via {@link SubversionSCM#getLocations()} as well as those that
     * are implicitly pulled in via svn:externals, but it excludes those locations that
     * are added via svn:externals in a way that fixes revisions.
     */
    final Map<String,Long> revisions;

    SVNRevisionState(Map<String, Long> revisions) {
        this.revisions = revisions;
    }

}
