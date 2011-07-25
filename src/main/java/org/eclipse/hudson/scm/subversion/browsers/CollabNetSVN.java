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
import org.eclipse.hudson.scm.subversion.SubversionRepositoryBrowser;
import java.io.IOException;
import java.net.URL;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * {@link RepositoryBrowser} implementation for CollabNet hosted Subversion repositories.
 * This enables Hudson to integrate with the repository browsers built-in to CollabNet-powered
 * sites such as Java.net and Tigris.org.
 *
 * @author Daniel Dyer
 */
public class CollabNetSVN extends SubversionRepositoryBrowser {
    @Extension
    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "CollabNet";
        }
    }


    public final URL url;


    /**
     * @param url The repository browser URL for the root of the project.
     * For example, a Java.net project called "myproject" would use
     * https://myproject.dev.java.net/source/browse/myproject
     */
    @DataBoundConstructor
    public CollabNetSVN(URL url) {
        this.url = normalizeToEndWithSlash(url);
    }


    /**
     * {@inheritDoc}
     */
    public URL getDiffLink(SubversionChangeLogSet.Path path) throws IOException {
        if (path.getEditType() != EditType.EDIT) {
            // No diff if the file is being added or deleted.
            return null;
        }

        int revision = path.getLogEntry().getRevision();
        QueryBuilder query = new QueryBuilder(null);
        query.add("r1=" + (revision - 1));
        query.add("r2=" + revision);
        return new URL(url, trimHeadSlash(path.getValue()) + query);
    }


    /**
     * {@inheritDoc}
     */
    public URL getFileLink(SubversionChangeLogSet.Path path) throws IOException {
        int revision = path.getLogEntry().getRevision();
        QueryBuilder query = new QueryBuilder(null);
        query.add("rev=" + revision);
        query.add("view=log");
        return new URL(url, trimHeadSlash(path.getValue()) + query);
    }


    /**
     * {@inheritDoc}
     */
    public URL getChangeSetLink(SubversionChangeLogSet.LogEntry changeSet) throws IOException {
        int revision = changeSet.getRevision();
        QueryBuilder query = new QueryBuilder(null);
        query.add("rev=" + revision);
        query.add("view=rev");
        return new URL(url, query.toString());
    }
}
