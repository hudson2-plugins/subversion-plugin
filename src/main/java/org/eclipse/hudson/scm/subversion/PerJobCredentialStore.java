/*******************************************************************************
 *
 * Copyright (c) 2004-2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.remoting.Channel;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;
import org.tmatesoft.svn.core.SVNURL;

import static java.util.logging.Level.INFO;

/**
 * Persists the credential per job. This object is remotable.
 *
 * @author Kohsuke Kawaguchi
 */
final class PerJobCredentialStore implements Saveable, SubversionSCM.DescriptorImpl.RemotableSVNAuthenticationProvider {
    private static final Logger LOGGER = Logger.getLogger(PerJobCredentialStore.class.getName());

    /**
     * Used to remember the context. If we are persisting, we don't want to persist a proxy,
     * even if that happens in the context of a remote call.
     */
    private static final ThreadLocal<Boolean> IS_SAVING = new ThreadLocal<Boolean>();

    private final transient AbstractProject<?, ?> project;
    private final transient String url;

    private static final String credentialsFileName = "subversion.credentials";

    private transient CredentialsSaveableListener saveableListener;

    /**
     * SVN authentication realm to its associated credentials, scoped to this project.
     */
    private final Map<String, SubversionSCM.DescriptorImpl.Credential> credentials = new Hashtable<String, SubversionSCM.DescriptorImpl.Credential>();

    PerJobCredentialStore(AbstractProject<?, ?> project, String url) {
        this.project = project;
        this.url = url;
        // read existing credential
        XmlFile xml = getXmlFile();
        try {
            if (xml.exists()) {
                xml.unmarshal(this);
            }
        } catch (IOException e) {
            // ignore the failure to unmarshal, or else we'll never get through beyond this point.
            LOGGER.log(INFO, Messages.PerJobCredentialStore_readCredentials_error(xml), e);
        }
    }

    private synchronized SubversionSCM.DescriptorImpl.Credential get(String key) {
        return credentials.get(key);
    }

    public SubversionSCM.DescriptorImpl.Credential getCredential(SVNURL url, String realm) {
        return null == url ? get(realm) : get(url.toDecodedString());
    }

    public void acknowledgeAuthentication(String realm, SubversionSCM.DescriptorImpl.Credential cred) {
        try {
            acknowledge(null == url? realm : url, cred);
        } catch (IOException e) {
            LOGGER.log(INFO, Messages.PerJobCredentialStore_acknowledgeAuthentication_error(), e);
        }
    }

    private synchronized void acknowledge(String key, SubversionSCM.DescriptorImpl.Credential cred) throws IOException {
        SubversionSCM.DescriptorImpl.Credential old = cred == null ? credentials.remove(key) : credentials.put(key, cred);
        // save only if there was a change
        if (old == null && cred == null) {
            return;
        }
        if (old == null || cred == null || !old.equals(cred)) {
            save();
        }
    }

    public synchronized void save() throws IOException {
        IS_SAVING.set(Boolean.TRUE);
        try {
            if (!credentials.isEmpty()) {
                XmlFile xmlFile = getXmlFile();
                xmlFile.write(this);
                SaveableListener.fireOnChange(this, xmlFile);
            }
        } finally {
            IS_SAVING.remove();
        }
    }

    private XmlFile getXmlFile() {
        return new XmlFile(new File(project.getRootDir(), credentialsFileName));
    }

    /*package*/
    synchronized boolean isEmpty() {
        return credentials.isEmpty();
    }

    /**
     * When sent to the remote node, send a proxy.
     */
    private Object writeReplace() {
        if (IS_SAVING.get() != null) {
            return this;
        }

        Channel c = Channel.current();
        return c == null ? this : c.export(SubversionSCM.DescriptorImpl.RemotableSVNAuthenticationProvider.class, this);
    }

    protected CredentialsSaveableListener getSaveableListener() {
        if (null == saveableListener) {
            ExtensionList<SaveableListener> extensionList = Hudson.getInstance().getExtensionList(
                SaveableListener.class);
            if (null != extensionList && !extensionList.isEmpty()) {
                for (SaveableListener listener : extensionList) {
                    if (listener instanceof CredentialsSaveableListener) {
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
            if (o instanceof PerJobCredentialStore) {
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
