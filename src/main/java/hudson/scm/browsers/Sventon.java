/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Stephen Connolly
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.scm.browsers;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SubversionChangeLogSet.LogEntry;
import hudson.scm.SubversionChangeLogSet.Path;
import hudson.util.FormValidation;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import javax.servlet.ServletException;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * {@link RepositoryBrowser} for Sventon 1.x.
 *
 * @author Stephen Connolly
 */
public class Sventon extends AbstractSventon {
    @DataBoundConstructor
    public Sventon(URL url, String repositoryInstance) throws MalformedURLException {
        super(url, repositoryInstance);
    }

    @Override
    public URL getDiffLink(Path path) throws IOException {
        if (path.getEditType() != EditType.EDIT) {
            return null;    // no diff if this is not an edit change
        }
        int r = path.getLogEntry().getRevision();
        return new URL(url, String.format("diffprev.svn?name=%s&commitrev=%d&committedRevision=%d&revision=%d&path=%s",
            repositoryInstance, Integer.valueOf(r), Integer.valueOf(r), Integer.valueOf(r), URLEncoder.encode(getPath(path), URL_CHARSET)));
    }

    @Override
    public URL getFileLink(Path path) throws IOException {
        if (path.getEditType() == EditType.DELETE) {
            return null; // no file if it's gone
        }
        int r = path.getLogEntry().getRevision();
        return new URL(url, String.format("goto.svn?name=%s&revision=%d&path=%s",
            repositoryInstance, Integer.valueOf(r), URLEncoder.encode(getPath(path), URL_CHARSET)));
    }

    /**
     * Trims off the root module portion to compute the path within FishEye.
     */
    private String getPath(Path path) {
        String s = trimHeadSlash(path.getValue());
        if (s.startsWith(repositoryInstance)) // this should be always true, but be defensive
        {
            s = trimHeadSlash(s.substring(repositoryInstance.length()));
        }
        return s;
    }

    @Override
    public URL getChangeSetLink(LogEntry changeSet) throws IOException {
        return new URL(url, String.format("revinfo.svn?name=%s&revision=%d",
            repositoryInstance, Integer.valueOf(changeSet.getRevision())));
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "Sventon 1.x";
        }

        /**
         * Performs on-the-fly validation of the URL.
         */
        public FormValidation doCheckUrl(@AncestorInPath AbstractProject project,
                                         @QueryParameter(fixEmpty = true) final String value)
            throws IOException, ServletException {
            if (!project.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok(); // can't check
            }
            if (value == null) { // nothing entered yet
                return FormValidation.ok();
            }

            return new SventonUrlChecker(value, Integer.valueOf(1)).check();
        }
    }

    private static final long serialVersionUID = 1L;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
    	if (!(o instanceof Sventon)) {
            return false;
        }

        Sventon that = (Sventon) o;

        return new EqualsBuilder()
        	.appendSuper(super.equals(that))
            .append(url, that.url)
            .append(repositoryInstance, that.repositoryInstance)
            .isEquals();
    }

    @Override
    public final int hashCode() {
        return new HashCodeBuilder()
            .append(url)
            .append(repositoryInstance)
            .toHashCode();
    }
}
