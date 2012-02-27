package hudson.scm;

import static java.util.logging.Level.INFO;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.XmlFile;
import hudson.matrix.MatrixConfiguration;
import hudson.model.ItemGroup;
import hudson.model.Saveable;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.listeners.SaveableListener;
import hudson.remoting.Channel;
import hudson.scm.SubversionSCM.DescriptorImpl.Credential;
import hudson.scm.SubversionSCM.DescriptorImpl.RemotableSVNAuthenticationProvider;
import hudson.scm.SubversionSCM.DescriptorImpl.SerializableSVNURL;
import hudson.scm.subversion.Messages;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import org.tmatesoft.svn.core.SVNException;

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

    private final transient AbstractProject<?, ?> project;
    private final transient String url;

    private static final String credentialsFileName = "subversion.credentials";

    private transient CredentialsSaveableListener saveableListener;

    /**
     * SVN authentication realm to its associated credentials, scoped to this project.
     */
    private final Map<String, Credential> credentials = new Hashtable<String, Credential>();

    PerJobCredentialStore(AbstractProject<?, ?> project, String url) {
        this.project = project;
        this.url = url;
        // read existing credential
        XmlFile xml = getXmlFile(project);
        try {
            if (xml.exists()) {
                xml.unmarshal(this);
            }
        } catch (IOException e) {
            // ignore the failure to unmarshal, or else we'll never get through beyond this point.
            LOGGER.log(INFO, Messages.PerJobCredentialStore_readCredentials_error(xml), e);
        }
    }

    private synchronized Credential get(String key) {
        return credentials.get(key);
    }

    public Credential getCredential(SerializableSVNURL serializableURL, String realm) throws SVNException {
        return get(getCredentialsKey(serializableURL.getSVNURL().toDecodedString(), realm));
    }
    
    
    

    public void acknowledgeAuthentication(String realm, Credential cred) {
        try {
            acknowledge(getCredentialsKey(url, realm), cred);
        } catch (IOException e) {
            LOGGER.log(INFO, Messages.PerJobCredentialStore_acknowledgeAuthentication_error(), e);
        }
    }

    /**
     * Method retuns credentials key based on realm and url. If url is not null and if it will be processed
     * without revision number, realm is used if url is null,
     *
     * @param url svn url
     * @param realm realm
     * @return credentials key.
     * @see SubversionSCM#getUrlWithoutRevision(String)
     */
    private String getCredentialsKey(String url, String realm) {
        return null == url ? realm : url.lastIndexOf("@") > 0 ? SubversionSCM.getUrlWithoutRevision(url) : url;
    }

    private synchronized void acknowledge(String key, Credential cred) throws IOException {
        Credential old = cred == null ? credentials.remove(key) : credentials.put(key, cred);
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
                XmlFile xmlFile = getXmlFile(project);
                xmlFile.write(this);
                SaveableListener.fireOnChange(this, xmlFile);
            }
        } finally {
            IS_SAVING.remove();
        }
    }

    XmlFile getXmlFile(Job prj) {
        //default behaviour
        File rootDir = prj.getRootDir();
        File credentialFile = new File(rootDir, credentialsFileName);
        if (credentialFile.exists()) {
            return new XmlFile(credentialFile);
        }
        //matrix configuration project
        if (prj instanceof MatrixConfiguration && prj.getParent() != null) {
            ItemGroup parent = prj.getParent();
            if (parent instanceof Job){
                return getXmlFile((Job)parent);
            }
        }
        if (prj.hasCascadingProject()) {
            return getXmlFile(prj.getCascadingProject());
        }
        return new XmlFile(new File(rootDir, credentialsFileName));
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
        return c == null ? this : c.export(RemotableSVNAuthenticationProvider.class, this);
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
