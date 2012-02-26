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
