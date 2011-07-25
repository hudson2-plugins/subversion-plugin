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
package org.eclipse.hudson.scm.subversion.browsers;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import org.eclipse.hudson.scm.subversion.SubversionChangeLogSet.LogEntry;
import org.eclipse.hudson.scm.subversion.SubversionChangeLogSet.Path;
import hudson.util.FormValidation;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import javax.servlet.ServletException;
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
            repositoryInstance, r, r, r, URLEncoder.encode(getPath(path), URL_CHARSET)));
    }

    @Override
    public URL getFileLink(Path path) throws IOException {
        if (path.getEditType() == EditType.DELETE) {
            return null; // no file if it's gone
        }
        int r = path.getLogEntry().getRevision();
        return new URL(url, String.format("goto.svn?name=%s&revision=%d&path=%s",
            repositoryInstance, r, URLEncoder.encode(getPath(path), URL_CHARSET)));
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
            repositoryInstance, changeSet.getRevision()));
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

            return new SventonUrlChecker(value, 1).check();
        }
    }

    private static final long serialVersionUID = 1L;
}
