/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Nikita Levyankov
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion.credential;

import com.trilead.ssh2.crypto.Base64;
import org.eclipse.hudson.scm.subversion.SubversionSCM;
import hudson.util.Scrambler;
import hudson.util.Secret;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNSSLAuthentication;

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
        } else {
            return null; // unexpected authentication type
        }
    }
}
