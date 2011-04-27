/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc.
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

import hudson.scm.SubversionRepositoryBrowser;
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
