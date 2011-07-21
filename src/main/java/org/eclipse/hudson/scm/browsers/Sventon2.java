/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Kohsuke Kawaguchi, Stephen Connolly
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.browsers;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import org.eclipse.hudson.scm.SubversionChangeLogSet.LogEntry;
import org.eclipse.hudson.scm.SubversionChangeLogSet.Path;
import hudson.util.FormValidation;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import javax.servlet.ServletException;
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
            encodePath(repositoryInstance), encodePath(getPath(path)), r));
    }

    @Override
    public URL getFileLink(Path path) throws IOException {
        if (path.getEditType() == EditType.DELETE) {
            return null; // no file if it's gone
        }
        int r = path.getLogEntry().getRevision();
        return new URL(url, String.format("repos/%s/goto/%s?revision=%d",
            encodePath(repositoryInstance), encodePath(getPath(path)), r));
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
            encodePath(repositoryInstance), changeSet.getRevision()));
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

            return new SventonUrlChecker(value, 2).check();
        }
    }

    private static final long serialVersionUID = 1L;
}
