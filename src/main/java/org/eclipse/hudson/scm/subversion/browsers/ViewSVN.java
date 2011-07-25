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
 *    Kohsuke Kawaguchi
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion.browsers;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.scm.EditType;
import hudson.scm.RepositoryBrowser;
import hudson.scm.browsers.QueryBuilder;
import org.eclipse.hudson.scm.subversion.SubversionChangeLogSet;
import org.eclipse.hudson.scm.subversion.SubversionChangeLogSet.Path;
import org.eclipse.hudson.scm.subversion.SubversionRepositoryBrowser;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * {@link RepositoryBrowser} for Subversion.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.90
 */
// See http://viewvc.tigris.org/source/browse/*checkout*/viewvc/trunk/docs/url-reference.html
public class ViewSVN extends SubversionRepositoryBrowser {
    /**
     * The URL of the top of the site.
     * <p/>
     * Normalized to ends with '/', like <tt>http://svn.apache.org/viewvc/</tt>
     * It may contain a query parameter like <tt>?root=foobar</tt>, so relative URL
     * construction needs to be done with care.
     */
    public final URL url;

    @DataBoundConstructor
    public ViewSVN(URL url) throws MalformedURLException {
        this.url = normalizeToEndWithSlash(url);
    }

    @Override
    public URL getDiffLink(Path path) throws IOException {
        if (path.getEditType() != EditType.EDIT) {
            return null;    // no diff if this is not an edit change
        }
        int r = path.getLogEntry().getRevision();
        return new URL(url, trimHeadSlash(path.getValue()) + param().add("r1=" + (r - 1)).add("r2=" + r));
    }

    @Override
    public URL getFileLink(Path path) throws IOException {
        return new URL(url, trimHeadSlash(path.getValue()) + param());
    }

    @Override
    public URL getChangeSetLink(SubversionChangeLogSet.LogEntry changeSet) throws IOException {
        return new URL(url, "." + param().add("view=rev").add("rev=" + changeSet.getRevision()));
    }

    private QueryBuilder param() {
        return new QueryBuilder(url.getQuery());
    }

    private static final long serialVersionUID = 1L;

    @Extension
    public static final class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "ViewSVN";
        }
    }
}
