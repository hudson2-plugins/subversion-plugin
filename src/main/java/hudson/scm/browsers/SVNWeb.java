package hudson.scm.browsers;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SubversionChangeLogSet;
import hudson.scm.SubversionChangeLogSet.Path;
import hudson.scm.SubversionRepositoryBrowser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * SVN::Web {@link RepositoryBrowser} for Subversion.
 */
public final class SVNWeb extends SubversionRepositoryBrowser {

    @Extension
    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "SVN::Web";
        }
    }

    private static final long serialVersionUID = 1L;

    /**
     * The URL of the top of the site.
     * <p/>
     * <p>Normalized to ends with '/', like <tt>http://svn.apache.org/wsvn/</tt>
     * It may contain a query parameter like <tt>?root=foobar</tt>, so relative
     * URL construction needs to be done with care.</p>
     */
    public final URL url;

    /**
     * Creates a new SVNWeb object.
     */
    @DataBoundConstructor
    public SVNWeb(URL url) throws MalformedURLException {
        this.url = normalizeToEndWithSlash(url);
    }

    /**
     * Returns the diff link value.
     *
     * @param path the given path value.
     * @return the diff link value.
     * @throws IOException DOCUMENT ME!
     */
    @Override
    public URL getDiffLink(Path path) throws IOException {
        if (path.getEditType() != EditType.EDIT) {
            return null; // no diff if this is not an edit change
        }

        int r = path.getLogEntry().getRevision();
        return new URL(
                url,
                "diff/" + trimHeadSlash(path.getValue()) +
                param().add("rev1=" + (r-1) + ";rev2=" + r)
        );
    }

    /**
     * Returns the file link value.
     *
     * @param path the given path value.
     * @return the file link value.
     * @throws IOException DOCUMENT ME!
     */
    @Override
    public URL getFileLink(Path path) throws IOException {
        final int r = path.getLogEntry().getRevision();
        return new URL(
                url,
                "view/" + trimHeadSlash(path.getValue()) +
                param().add("rev=" + r)
        );
    }

    /**
     * Returns the change set link value.
     *
     * @param changeSet the given changeSet value.
     * @return the change set link value.
     * @throws IOException DOCUMENT ME!
     */
    @Override
    public URL getChangeSetLink(SubversionChangeLogSet.LogEntry changeSet)
            throws IOException {
        final int r = changeSet.getRevision();
        return new URL(
                url,
                "revision/" +
                param().add("rev=" + r)
        );
    }

    private QueryBuilder param() {
        return new QueryBuilder(url.getQuery());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SVNWeb)) {
            return false;
        }

        SVNWeb other = (SVNWeb) o;

        return new EqualsBuilder()
            .append(url, other.url)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(url)
            .toHashCode();
    }

}
