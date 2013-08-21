/**
 * 
 */
package hudson.scm;

import hudson.scm.auth.ISVNAuthenticationOutcomeListener;

import java.io.File;

import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;

/**
 * @author schristou88
 *
 */
public class BasicAuthenticationManager extends org.tmatesoft.svn.core.auth.BasicAuthenticationManager {

	public BasicAuthenticationManager(String userName, File keyFile,
			String passphrase, int portNumber) {
		super(userName, keyFile, passphrase, portNumber);
	}
	
	public static void acknowledgeAuthentication(boolean accepted, String kind, String realm, SVNErrorMessage errorMessage, SVNAuthentication authentication, SVNURL accessedURL, ISVNAuthenticationManager authManager) throws SVNException {
		if (outcomeListener != null)
			outcomeListener.acknowledgeAuthentication(accepted, kind, realm, errorMessage, authentication);
	}
	
	public void setAuthenticationOutcomeListener(ISVNAuthenticationOutcomeListener listener) {
		outcomeListener = listener;
	}
	
	private static ISVNAuthenticationOutcomeListener outcomeListener;
}
