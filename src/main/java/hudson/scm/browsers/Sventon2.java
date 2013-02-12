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
import java.io.UnsupportedEncodingException;
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
 * {@link RepositoryBrowser} for Sventon 2.x.
 *
 * @author Stephen Connolly
 */
public class Sventon2 extends AbstractSventon {
    @DataBoundConstructor
    public Sventon2(URL url, String repositoryInstance) throws MalformedURLException {
        super(url, repositoryInstance);
    }

    @Override
    public URL getDiffLink(Path path) throws IOException {
        if (path.getEditType() != EditType.EDIT) {
            return null;    // no diff if this is not an edit change
        }
        int r = path.getLogEntry().getRevision();
        return new URL(url, String.format("repos/%s/diff/%s?revision=%d",
            encodePath(repositoryInstance), encodePath(getPath(path)), Integer.valueOf(r)));
    }

    @Override
    public URL getFileLink(Path path) throws IOException {
        if (path.getEditType() == EditType.DELETE) {
            return null; // no file if it's gone
        }
        int r = path.getLogEntry().getRevision();
        return new URL(url, String.format("repos/%s/goto/%s?revision=%d",
            encodePath(repositoryInstance), encodePath(getPath(path)), Integer.valueOf(r)));
    }

    /**
     * Trims off the root module portion to compute the path within FishEye.
     */
    private String getPath(Path path) {
        String s = trimHeadSlash(path.getValue());
        return s;
    }

    private static String encodePath(String path)
        throws UnsupportedEncodingException {
        StringBuilder buf = new StringBuilder();
        if (path.startsWith("/")) {
            buf.append('/');
        }
        boolean first = true;
        for (String pathElement : path.split("/")) {
            if (first) {
                first = false;
            } else {
                buf.append('/');
            }
            buf.append(URLEncoder.encode(pathElement, URL_CHARSET));
        }
        if (path.endsWith("/")) {
            buf.append('/');
        }
        return buf.toString().replace("%20", "+");
    }

    @Override
    public URL getChangeSetLink(LogEntry changeSet) throws IOException {
        return new URL(url, String.format("repos/%s/info?revision=%d",
            encodePath(repositoryInstance), Integer.valueOf(changeSet.getRevision())));
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "Sventon 2.x";
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
            if (value == null) {// nothing entered yet
                return FormValidation.ok();
            }

            return new SventonUrlChecker(value, Integer.valueOf(2)).check();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Sventon2)) {
            return false;
        }

        Sventon2 that = (Sventon2) o;

        return new EqualsBuilder()
            .append(url, that.url)
            .append(repositoryInstance, that.repositoryInstance)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(url)
            .append(repositoryInstance)
            .toHashCode();
    }

    private static final long serialVersionUID = 1L;
}
