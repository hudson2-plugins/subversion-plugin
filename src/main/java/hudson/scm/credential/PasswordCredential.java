/*
 * The MIT License
 *
 * Copyright (c) 2011, Oracle Corporation, Nikita Levyankov
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
package hudson.scm.credential;

import hudson.scm.SubversionSCM;
import hudson.util.Scrambler;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNPasswordAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;

/**
 * Username/password based authentication.
 * <p/>
 * Date: 5/11/11
 *
 * @author Nikita Levyankov
 */
public class PasswordCredential extends SubversionSCM.DescriptorImpl.Credential {
    private final String userName;
    private final String password; // scrambled by base64

    public PasswordCredential(String userName, String password) {
        this.userName = userName;
        this.password = Scrambler.scramble(password);
    }

    @Override
    public SVNAuthentication createSVNAuthentication(String kind) {
        if (kind.equals(ISVNAuthenticationManager.SSH)) {
            return new SVNSSHAuthentication(userName, Scrambler.descramble(password), -1, false, null, false);
        }
        return new SVNPasswordAuthentication(userName, Scrambler.descramble(password), false, null, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PasswordCredential)) {
            return false;
        }
        PasswordCredential that = (PasswordCredential) o;
        return userName.equals(that.userName) && password.equals(that.password);
    }

    @Override
    public int hashCode() {
        int result = userName.hashCode();
        result = 31 * result + password.hashCode();
        return result;
    }
}
