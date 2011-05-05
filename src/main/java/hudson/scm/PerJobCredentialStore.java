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

import hudson.Extension;
import hudson.ExtensionList;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.remoting.Channel;
import hudson.scm.SubversionSCM.DescriptorImpl.Credential;
import hudson.scm.SubversionSCM.DescriptorImpl.RemotableSVNAuthenticationProvider;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;
import org.tmatesoft.svn.core.SVNURL;
import hudson.scm.subversion.Messages;

import static java.util.logging.Level.INFO;

/**
 * Persists the credential per job. This object is remotable.
 *
 * @author Kohsuke Kawaguchi
 */
final class PerJobCredentialStore implements Saveable, RemotableSVNAuthenticationProvider {
    private static final Logger LOGGER = Logger.getLogger(PerJobCredentialStore.class.getName());

    /**
     * Used to remember the context. If we are persisting, we don't want to persist a proxy,
     * even if that happens in the context of a remote call.
     */
    private static final ThreadLocal<Boolean> IS_SAVING = new ThreadLocal<Boolean>();

    private final transient AbstractProject<?,?> project;

    private static final String credentialsFileName = "subversion.credentials";

    private transient CredentialsSaveableListener saveableListener;

    /**
     * SVN authentication realm to its associated credentials, scoped to this project.
     */
    private final Map<String,Credential> credentials = new Hashtable<String,Credential>();
    
    PerJobCredentialStore(AbstractProject<?,?> project) {
        this.project = project;
        // read existing credential
        XmlFile xml = getXmlFile();
        try {
            if (xml.exists())
                xml.unmarshal(this);
        } catch (IOException e) {
            // ignore the failure to unmarshal, or else we'll never get through beyond this point.
            LOGGER.log(INFO, Messages.PerJobCredentialStore_readCredentials_error(xml), e);
        }
    }

    public synchronized Credential get(String realm) {
        return credentials.get(realm);
    }

    public Credential getCredential(SVNURL url, String realm) {
        return get(realm);
    }

    public void acknowledgeAuthentication(String realm, Credential cred) {
        try {
            acknowledge(realm, cred);
        } catch (IOException e) {
            LOGGER.log(INFO,Messages.PerJobCredentialStore_acknowledgeAuthentication_error(), e);
        }
    }

    public synchronized void acknowledge(String realm, Credential cred) throws IOException {
        Credential old = cred==null ? credentials.remove(realm) : credentials.put(realm, cred);
        // save only if there was a change
        if (old==null && cred==null)    return;
        if (old==null || cred==null || !old.equals(cred))
            save();
    }

    public synchronized void save() throws IOException {
        IS_SAVING.set(Boolean.TRUE);
        try {
            if(!credentials.isEmpty()) {
                XmlFile xmlFile = getXmlFile();
                xmlFile.write(this);
                SaveableListener.fireOnChange(this, xmlFile);
            }
        } finally {
            IS_SAVING.remove();
        }
    }

    private XmlFile getXmlFile() {
        return new XmlFile(new File(project.getRootDir(),credentialsFileName));
    }

    /*package*/ synchronized boolean isEmpty() {
        return credentials.isEmpty();
    }

    /**
     * When sent to the remote node, send a proxy.
     */
    private Object writeReplace() {
        if (IS_SAVING.get()!=null)  return this;
        
        Channel c = Channel.current();
        return c==null ? this : c.export(RemotableSVNAuthenticationProvider.class, this);
    }

    protected CredentialsSaveableListener getSaveableListener() {
        if(null == saveableListener) {
            ExtensionList<SaveableListener> extensionList = Hudson.getInstance().getExtensionList(
                SaveableListener.class);
            if(null != extensionList && !extensionList.isEmpty()) {
                for(SaveableListener listener : extensionList) {
                    if(listener instanceof CredentialsSaveableListener) {
                        saveableListener = (CredentialsSaveableListener) listener;
                        break;
                    }
                }
            }
        }
        return saveableListener;
    }

    @Extension
    public static class CredentialsSaveableListener extends SaveableListener {

        private boolean fileChanged = false;

        @Override
        public void onChange(Saveable o, XmlFile file) {
            if(o instanceof PerJobCredentialStore) {
                fileChanged = true;
            }
        }

        public boolean isFileChanged() {
            return fileChanged;
        }

        public void resetChangedStatus() {
            fileChanged = false;
        }
    }
}
