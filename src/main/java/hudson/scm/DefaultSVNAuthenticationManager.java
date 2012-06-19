/**
 * 
 */
package hudson.scm;

import java.io.File;

import org.tmatesoft.svn.core.wc.SVNWCUtil;

import hudson.scm.auth.ISVNAuthenticationManager;
import hudson.scm.auth.ISVNAuthenticationOutcomeListener;

/**
 * @author Steven
 *
 */
public class DefaultSVNAuthenticationManager extends org.tmatesoft.svn.core.internal.wc.DefaultSVNAuthenticationManager
		implements ISVNAuthenticationManager {

	public DefaultSVNAuthenticationManager(File configDirectory,
			boolean storeAuth, String userName, String password) {
		super(configDirectory, storeAuth, userName, password);
	}

	public DefaultSVNAuthenticationManager(
			org.tmatesoft.svn.core.auth.ISVNAuthenticationManager createDefaultAuthenticationManager) {
		super(SVNWCUtil.getDefaultConfigurationDirectory(), createDefaultAuthenticationManager.isAuthenticationForced(), null, null);
	}

	
	/**
	 * File configDirectory, boolean storeAuth, String userName, String password, File privateKey, String passphrase)
	 * @param subversionConfigDir
	 * @param b
	 * @param username
	 * @param password
	 * @param keyFile
	 * @param password2
	 */
	public DefaultSVNAuthenticationManager(File subversionConfigDir, boolean b,
			String username, String password, File keyFile, String password2) {
		super(subversionConfigDir, b, username, password, keyFile, password2);
	}

	/* (non-Javadoc)
	 * @see hudson.scm.auth.ISVNAuthenticationManager#getAuthenticationManager()
	 */
	@Override
	public org.tmatesoft.svn.core.auth.ISVNAuthenticationManager getAuthenticationManager() {
		return (org.tmatesoft.svn.core.auth.ISVNAuthenticationManager)this;
	}

	/* (non-Javadoc)
	 * @see hudson.scm.auth.ISVNAuthenticationManager#setAuthenticationOutcomeListener(hudson.scm.auth.ISVNAuthenticationOutcomeListener)
	 */
	@Override
	public void setAuthenticationOutcomeListener(
			ISVNAuthenticationOutcomeListener listener) {
//		DO NOTHING
	}
}
