/*
 * The MIT License
 *
 * Copyright (c) 2011, Oracle Corporation, Winston Prakash, Nikita Levyankov, Anton Kozak
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
package hudson.scm;

import org.tmatesoft.svn.core.auth.*;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.io.SVNRepository;

import javax.net.ssl.TrustManager;

/**
 * {@link ISVNAuthenticationManager} filter. Useful for customizing the behavior by delegation.
 * @author Kohsuke Kawaguchi
 */
public class FilterSVNAuthenticationManager implements ISVNAuthenticationManager {
    protected ISVNAuthenticationManager core;

    public FilterSVNAuthenticationManager(ISVNAuthenticationManager core) {
        this.core = core;
    }

    public void setAuthenticationProvider(ISVNAuthenticationProvider provider) {
        core.setAuthenticationProvider(provider);
    }

    public ISVNProxyManager getProxyManager(SVNURL url) throws SVNException {
        return core.getProxyManager(url);
    }

    public TrustManager getTrustManager(SVNURL url) throws SVNException {
        return core.getTrustManager(url);
    }

    public SVNAuthentication getFirstAuthentication(String kind, String realm, SVNURL url) throws SVNException {
        return core.getFirstAuthentication(kind, realm, url);
    }

    public SVNAuthentication getNextAuthentication(String kind, String realm, SVNURL url) throws SVNException {
        return core.getNextAuthentication(kind, realm, url);
    }

    public void acknowledgeAuthentication(boolean accepted, String kind, String realm, SVNErrorMessage errorMessage, SVNAuthentication authentication) throws SVNException {
        core.acknowledgeAuthentication(accepted, kind, realm, errorMessage, authentication);
    }

    public void acknowledgeTrustManager(TrustManager manager) {
        core.acknowledgeTrustManager(manager);
    }

    public boolean isAuthenticationForced() {
        return core.isAuthenticationForced();
    }

    public int getReadTimeout(SVNRepository repository) {
        return core.getReadTimeout(repository);
    }

    public int getConnectTimeout(SVNRepository repository) {
        return core.getConnectTimeout(repository);
    }

    public void setAuthenticationOutcomeListener(ISVNAuthenticationOutcomeListener listener) {
        core.setAuthenticationOutcomeListener(listener);
    }
}
