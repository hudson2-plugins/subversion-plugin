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

import com.trilead.ssh2.crypto.Base64;
import hudson.scm.SubversionSCM;
import hudson.scm.auth.ISVNAuthenticationManager;
import hudson.util.Scrambler;
import hudson.util.Secret;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.auth.SVNAuthentication;

/**
 * SSL client certificate based authentication.
 * <p/>
 * Date: 5/11/11
 *
 * @author Nikita Levyankov
 */
public class SslClientCertificateCredential extends SubversionSCM.DescriptorImpl.Credential {
    private final Secret certificate;
    private final String password; // scrambled by base64

    public SslClientCertificateCredential(File certificate, String password) throws IOException {
        this.password = Scrambler.scramble(password);
        this.certificate = Secret.fromString(
            new String(Base64.encode(FileUtils.readFileToByteArray(certificate))));
    }

    @Override
    public SVNAuthentication createSVNAuthentication(String kind) {
        if (kind.equals(ISVNAuthenticationManager.SSL)) {
            try {
                return new SVNSSLAuthentication(
                    Base64.decode(certificate.getPlainText().toCharArray()),
                    Scrambler.descramble(password), false);
            } catch (IOException e) {
                throw new Error(e); // can't happen
            }
        }
        return null; // unexpected authentication type
    }
}
