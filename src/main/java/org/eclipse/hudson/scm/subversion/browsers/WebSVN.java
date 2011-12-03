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
 *    Kohsuke Kawaguchi, Daniel Dyer
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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.kohsuke.stapler.DataBoundConstructor;


/**
 * {@link RepositoryBrowser} for Subversion.  Assumes that WebSVN is
 * configured with Multiviews enabled.
 *
 * @author jasonchaffee at dev.java.net
 * @since 1.139
 */
public class WebSVN extends SubversionRepositoryBrowser {

    @Extension
    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "WebSVN";
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
     * Creates a new WebSVN object.
     *
     * @param url DOCUMENT ME!
     * @throws MalformedURLException DOCUMENT ME!
     */
    @DataBoundConstructor
    public WebSVN(URL url) throws MalformedURLException {
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

        return new URL(url,
            trimHeadSlash(path.getValue()) +
                param().add("op=diff").add("rev=" + r));
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
        return new URL(url, trimHeadSlash(path.getValue()) + param());
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
        return new URL(url,
            "." +
                param().add("rev=" + changeSet.getRevision()).add("sc=1"));
    }

    private QueryBuilder param() {
        return new QueryBuilder(url.getQuery());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        WebSVN that = (WebSVN) o;

        return new EqualsBuilder()
            .append(url, that.url)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(url)
            .toHashCode();
    }
}
