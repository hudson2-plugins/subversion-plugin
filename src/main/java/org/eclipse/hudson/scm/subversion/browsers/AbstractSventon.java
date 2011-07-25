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

import org.eclipse.hudson.scm.subversion.SubversionRepositoryBrowser;
import hudson.util.FormValidation;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletException;

/**
 * Common part of {@link Sventon} and {@link Sventon2}
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractSventon extends SubversionRepositoryBrowser {
    /**
     * The URL of the Sventon 2.x repository.
     * <p/>
     * This is normally like <tt>http://somehost.com/svn/</tt>
     * Normalized to have '/' at the tail.
     */
    public final URL url;

    /**
     * Repository instance. Cannot be empty
     */
    protected final String repositoryInstance;

    /**
     * The charset to use when encoding paths in an URI (specified in RFC 3986).
     */
    protected static final String URL_CHARSET = "UTF-8";

    public AbstractSventon(URL url, String repositoryInstance) throws MalformedURLException {
        this.url = normalizeToEndWithSlash(url);

        // normalize
        repositoryInstance = repositoryInstance.trim();

        this.repositoryInstance = repositoryInstance == null ? "" : repositoryInstance;
    }

    public String getRepositoryInstance() {
        return repositoryInstance;
    }

    protected static class SventonUrlChecker extends FormValidation.URLCheck {

        private String url;

        private Integer version;

        public SventonUrlChecker(String url, Integer version) {
            this.url = url;
            this.version = version;
        }

        protected FormValidation check() throws IOException, ServletException {
            String v = url;
            if (!v.endsWith("/")) {
                v += '/';
            }

            try {
                if (findText(open(new URL(v)), "sventon " + version)) {
                    return FormValidation.ok();
                } else if (findText(open(new URL(v)), "sventon")) {
                    return FormValidation.error("This is a valid Sventon URL but it doesn't look like Sventon "
                        + version + ".x");
                } else {
                    return FormValidation.error("This is a valid URL but it doesn't look like Sventon");
                }
            } catch (IOException e) {
                return handleIOException(v, e);
            }
        }
    }
}
