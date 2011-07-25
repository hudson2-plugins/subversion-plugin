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

import org.eclipse.hudson.scm.subversion.SubversionSCM;
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
            return new SVNSSHAuthentication(userName, Scrambler.descramble(password), -1, false);
        } else {
            return new SVNPasswordAuthentication(userName, Scrambler.descramble(password), false);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
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
