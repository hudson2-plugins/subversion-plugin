package hudson.scm.credential;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import hudson.scm.AbstractSubversionTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.net.ssl.SSLEngine;

import org.apache.commons.io.FileUtils;
import org.hudsonci.utils.io.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;

import com.trilead.ssh2.crypto.Base64;

public class SslClientCertificateCredentialTest extends AbstractSubversionTest {
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		// No this is NOT my certificate so don't ask.
		StringBuilder sslCertificateString = new StringBuilder().append("-----BEGIN CERTIFICATE-----")
															  .append("MIIDrTCCAxagAwIBAgIBADANBgkqhkiG9w0BAQQFADCBnDEbMBkGA1UEChMSVGhl")
															  .append("IFNhbXBsZSBDb21wYW55MRQwEgYDVQQLEwtDQSBEaXZpc2lvbjEcMBoGCSqGSIb3")
															  .append("DQEJARYNY2FAc2FtcGxlLmNvbTETMBEGA1UEBxMKTWV0cm9wb2xpczERMA8GA1UE")
															  .append("CBMITmV3IFlvcmsxCzAJBgNVBAYTAlVTMRQwEgYDVQQDEwtUU0MgUm9vdCBDQTAe")
															  .append("Fw0wMTEyMDgwNDI3MDVaFw0wMjEyMDgwNDI3MDVaMIGcMRswGQYDVQQKExJUaGUg")
															  .append("U2FtcGxlIENvbXBhbnkxFDASBgNVBAsTC0NBIERpdmlzaW9uMRwwGgYJKoZIhvcN")
															  .append("AQkBFg1jYUBzYW1wbGUuY29tMRMwEQYDVQQHEwpNZXRyb3BvbGlzMREwDwYDVQQI")
															  .append("EwhOZXcgWW9yazELMAkGA1UEBhMCVVMxFDASBgNVBAMTC1RTQyBSb290IENBMIGf")
															  .append("MA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDaiAwfKB6ZBtnTRTIo6ddomt0S9ec0")
															  .append("NcuvtJogt0s9dXpHowh98FCDjnLtCi8du6LDTZluhlOtTFARPlV/LVnpsbyMCXMs")
															  .append("G2qpdjJop+XIBdvoCz2HpGXjUmym8WLqt+coWwJqUSwiEba74JG93v7TU+Xcvc00")
															  .append("5MWnxmKZzD/R3QIDAQABo4H8MIH5MAwGA1UdEwQFMAMBAf8wHQYDVR0OBBYEFG/v")
															  .append("yytrBtEquMX2dreysix/MlPMMIHJBgNVHSMEgcEwgb6AFG/vyytrBtEquMX2drey")
															  .append("six/MlPMoYGipIGfMIGcMRswGQYDVQQKExJUaGUgU2FtcGxlIENvbXBhbnkxFDAS")
															  .append("BgNVBAsTC0NBIERpdmlzaW9uMRwwGgYJKoZIhvcNAQkBFg1jYUBzYW1wbGUuY29t")
															  .append("MRMwEQYDVQQHEwpNZXRyb3BvbGlzMREwDwYDVQQIEwhOZXcgWW9yazELMAkGA1UE")
															  .append("BhMCVVMxFDASBgNVBAMTC1RTQyBSb290IENBggEAMA0GCSqGSIb3DQEBBAUAA4GB")
															  .append("ABclymJfsPOUazNQO8aIaxwVbXWS+8AFEkMMRx6O68ICAMubQBvs8Buz3ALXhqYe")
															  .append("FS5G13pW2ZnAlSdTkSTKkE5wGZ1RYSfyiEKXb+uOKhDN9LnajDzaMPkNDU2NDXDz")
															  .append("SqHk9ZiE1boQaMzjNLu+KabTLpmL9uXvFA/i+gdenFHv")
															  .append("-----END CERTIFICATE-----");
		
		File f = new File("/tmp/certificate.perm");
		
		try {
			f.createNewFile();
			FileUtils.writeStringToFile(f, sslCertificateString.toString());
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		FileUtils.deleteQuietly(new File("/tmp/certificate.perm"));
	}
		
	@Test
	public void testSSLCertificateConstruction() throws IOException {
		SslClientCertificateCredential ssl = new SslClientCertificateCredential(new File("/tmp/certificate.perm"), "test");
		
		// Invalid authentication type.
		assertNull(ssl.createSVNAuthentication(ISVNAuthenticationManager.SSH));
		
		assertNotNull(ssl.createSVNAuthentication(ISVNAuthenticationManager.SSL));
	}
	
}
