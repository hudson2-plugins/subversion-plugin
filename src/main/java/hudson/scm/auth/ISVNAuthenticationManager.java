/**
 * 
 */
package hudson.scm.auth;

/**
 * @author Steven
 *
 */
public interface ISVNAuthenticationManager extends
		org.tmatesoft.svn.core.auth.ISVNAuthenticationManager {

	void setAuthenticationOutcomeListener(
			ISVNAuthenticationOutcomeListener listener);

}