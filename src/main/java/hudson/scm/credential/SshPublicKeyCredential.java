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

import hudson.model.Hudson;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.scm.SubversionSCM;
import hudson.util.Scrambler;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Chmod;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNSSHAuthentication;

/**
 * Public key authentication for Subversion over SSH.
 * <p/>
 * Date: 5/11/11
 *
 * @author Nikita Levyankov
 */
public class SshPublicKeyCredential extends SubversionSCM.DescriptorImpl.Credential {
    private static final Logger LOGGER = Logger.getLogger(SubversionSCM.class.getName());
    private final String userName;
    private final String passphrase; // scrambled by base64
    private final String id;

    /**
     * @param keyFile stores SSH private key. The file will be copied.
     */
    public SshPublicKeyCredential(String userName, String passphrase, File keyFile) throws SVNException {
        this.userName = userName;
        this.passphrase = Scrambler.scramble(passphrase);

        SecureRandom r = new SecureRandom();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            buf.append(Integer.toHexString(r.nextInt(16)));
        }
        this.id = buf.toString();

        try {
            File savedKeyFile = getKeyFile();
            FileUtils.copyFile(keyFile, savedKeyFile);
            setFilePermissions(savedKeyFile, "600");
        } catch (IOException e) {
            throw new SVNException(
                SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE,
                    Messages.SshPublicKeyCredential_private_key_save_error()), e);
        }
    }

    /**
     * Gets the location where the private key will be permanently stored.
     */
    private File getKeyFile() {
        File dir = new File(Hudson.getInstance().getRootDir(),
            Messages.SshPublicKeyCredential_private_key());
        if (dir.mkdirs()) {
            // make sure the directory exists. if we created it, try to set the permission to 600
            // since this is sensitive information
            setFilePermissions(dir,
                Messages.SshPublicKeyCredential_private_key_permissions());
        }
        return new File(dir, id);
    }

    /**
     * Set the file permissions
     */
    private boolean setFilePermissions(File file, String perms) {
        try {
            Chmod chmod = new Chmod();
            chmod.setProject(new Project());
            chmod.setFile(file);
            chmod.setPerm(perms);
            chmod.execute();
        } catch (BuildException e) {
            // if we failed to set the permission, that's fine.
            LOGGER.log(Level.WARNING,
                Messages.SshPublicKeyCredential_private_key_set_permissions_error(file), e);
            return false;
        }

        return true;
    }


    @Override
    public SVNSSHAuthentication createSVNAuthentication(String kind) throws SVNException {
        if (kind.equals(ISVNAuthenticationManager.SSH)) {
            try {
                Channel channel = Channel.current();
                String privateKey;
                if (channel != null) {
                    // remote
                    privateKey = channel.call(new Callable<String, IOException>() {
                        public String call() throws IOException {
                            return FileUtils.readFileToString(getKeyFile(),
                                Messages.SshPublicKeyCredential_private_key_encoding());
                        }
                    });
                } else {
                    privateKey = FileUtils.readFileToString(getKeyFile(),
                        Messages.SshPublicKeyCredential_private_key_encoding());
                }
                return new SVNSSHAuthentication(userName, privateKey.toCharArray(),
                    Scrambler.descramble(passphrase), -1, false, null, false);
            } catch (IOException e) {
                throw new SVNException(
                    SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE,
                        Messages.SshPublicKeyCredential_private_key_load_error()), e);
            } catch (InterruptedException e) {
                throw new SVNException(
                    SVNErrorMessage.create(SVNErrorCode.AUTHN_CREDS_UNAVAILABLE,
                        Messages.SshPublicKeyCredential_private_key_load_error()), e);
            }
        }
        return null; // unknown
    }
}
