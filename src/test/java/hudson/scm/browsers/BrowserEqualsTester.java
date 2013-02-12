/**
 * 
 */
package hudson.scm.browsers;

import java.net.MalformedURLException;
import java.net.URL;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

/**
 * @author schristou88
 *
 */
public class BrowserEqualsTester {
	@Test
	public void testViewSVNEqualsHashCode() throws MalformedURLException {
		EqualsVerifier.forClass(ViewSVN.class).debug()
		.withPrefabValues(URL.class, new URL("http://test"), new URL("https://test"))
		.verify();
	}
	
	@Test
	public void testFishEyeSVNEqualsHashCode() throws MalformedURLException {
		EqualsVerifier.forClass(FishEyeSVN.class)
			.withPrefabValues(URL.class, new URL("http://test"), new URL("https://test"))
			.verify();
	}
	
	@Test
	public void testCollabNetSVNEqualsHashCode() throws MalformedURLException {
		EqualsVerifier.forClass(CollabNetSVN.class)
			.withPrefabValues(URL.class, new URL("http://test"), new URL("https://test"))
			.verify();
	}
	
	@Test
	public void testSVNWebEqualsHashCode() throws MalformedURLException {
		EqualsVerifier.forClass(SVNWeb.class)
			.withPrefabValues(URL.class, new URL("http://test"), new URL("https://test"))
			.verify();
	}
	
	@Test
	public void testWebSVNEqualsHashCode() throws MalformedURLException {
		EqualsVerifier.forClass(WebSVN.class)
			.withPrefabValues(URL.class, new URL("http://test"), new URL("https://test"))
			.verify();
	}
	
	@Test
	public void testWebSVN2EqualsHashCode() throws MalformedURLException {
		EqualsVerifier.forClass(WebSVN2.class)
			.withPrefabValues(URL.class, new URL("http://test"), new URL("https://test"))
			.verify();
	}
}
