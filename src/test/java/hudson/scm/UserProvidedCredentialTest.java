/**
 * 
 */
package hudson.scm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletRequest;

import hudson.model.FileParameterValue.FileItemImpl;
import hudson.model.Hudson;
import hudson.security.csrf.CrumbIssuer;
import hudson.util.MultipartFormDataParser;

import org.apache.commons.fileupload.FileItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author schristou88
 *
 */
@RunWith(PowerMockRunner.class)
public class UserProvidedCredentialTest {	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
	@After
	public void tearDown() {
		new File("/tmp/PuTTYFile.ppk").delete();
	}
	
	@PrepareOnlyThisForTest(Hudson.class)
	@Test
	public void testUPCCreationFromForm() throws Exception {
		PowerMockito.mockStatic(Hudson.class);
		when(Hudson.getInstance()).thenReturn(hudson);
		CrumbIssuerProxy cip = new CrumbIssuerProxy();
		when(hudson.getCrumbIssuer()).thenReturn(cip);
		
		UserProvidedCredential upc = null;
		try {
			upc = UserProvidedCredential.fromForm(mockStaplerRequest, mockMultipartFormDataParser);
			fail("Should have thrown a No crumb found, or forbidden exception");
		} catch (Exception e) {
			assertEquals("java.io.IOException: No crumb found", e.getMessage());
		}
		
		cip.vcrumb = true;
		upc = UserProvidedCredential.fromForm(mockStaplerRequest, mockMultipartFormDataParser);
		when(hudson.getCrumbIssuer()).thenReturn(null);
		upc = UserProvidedCredential.fromForm(mockStaplerRequest, mockMultipartFormDataParser);
		assertNotNull (upc);
		
		when(mockMultipartFormDataParser.get("kind")).thenReturn("publickey");
		createPuTTYFile();
		when(mockMultipartFormDataParser.getFileItem("privateKey")).thenReturn(new FileItemImpl(new File("/tmp/PuTTYFile.ppk")));
		upc = upc.fromForm(mockStaplerRequest, mockMultipartFormDataParser);
		
		// verify throwing exception when write fails to occur.
		when(mockMultipartFormDataParser.getFileItem("privateKey")).thenReturn(new FileItemImplProxy());
		try {
			upc.fromForm(mockStaplerRequest, mockMultipartFormDataParser);
			fail("Should have thrown an exception when writting to the file.");
		} catch(Exception e) {
			assertEquals(e.getMessage(), "Mock exception has occurred");
		}
		upc.close();
	}
	
	@Mock StaplerRequest mockStaplerRequest;
	@Mock MultipartFormDataParser mockMultipartFormDataParser;
	@Mock Hudson hudson;
		
	public void createPuTTYFile() {
		String samplePuTTYFile = "PuTTY-User-Key-File-2: ssh-rsa\n" + 
						   "Encryption: none\n" + 
						   "Comment: rsa-key-20080514\n" + 
						   "Public-Lines: 4\n" + 
						   "AAAAB3NzaC1yc2EAAAABJQAAAIEAiPVUpONjGeVrwgRPOqy3Ym6kF/f8bltnmjA2\n" + 
						   "BMdAtaOpiD8A2ooqtLS5zWYuc0xkW0ogoKvORN+RF4JI+uNUlkxWxnzJM9JLpnvA\n" + 
						   "HrMoVFaQ0cgDMIHtE1Ob1cGAhlNInPCRnGNJpBNcJ/OJye3yt7WqHP4SPCCLb6nL\n" + 
						   "nmBUrLM=\n" + 
						   "Private-Lines: 8\n" + 
						   "AAAAgGtYgJzpktzyFjBIkSAmgeVdozVhgKmF6WsDMUID9HKwtU8cn83h6h7ug8qA\n" + 
						   "hUWcvVxO201/vViTjWVz9ALph3uMnpJiuQaaNYIGztGJBRsBwmQW9738pUXcsUXZ\n" + 
						   "79KJP01oHn6Wkrgk26DIOsz04QOBI6C8RumBO4+F1WdfueM9AAAAQQDmA4hcK8Bx\n" + 
						   "nVtEpcF310mKD3nsbJqARdw5NV9kCxPnEsmy7Sy1L4Ob/nTIrynbc3MA9HQVJkUz\n" + 
						   "7V0va5Pjm/T7AAAAQQCYbnG0UEekwk0LG1Hkxh1OrKMxCw2KWMN8ac3L0LVBg/Tk\n" + 
						   "8EnB2oT45GGeJaw7KzdoOMFZz0iXLsVLNUjNn2mpAAAAQQCN6SEfWqiNzyc/w5n/\n" + 
						   "lFVDHExfVUJp0wXv+kzZzylnw4fs00lC3k4PZDSsb+jYCMesnfJjhDgkUA0XPyo8\n" + 
						   "Emdk\n" + 
						   "Private-MAC: 50c45751d18d74c00fca395deb7b7695e3ed6f77\n";
		
		BufferedOutputStream boss = null; // Like a boss!
		try {
			new File("/tmp/PuTTYFile.ppk").createNewFile();
			boss = new BufferedOutputStream(new FileOutputStream(new File("/tmp/PuTTYFile.ppk")));
			boss.write(samplePuTTYFile.getBytes());
		} catch (IOException e) {
			fail(e.getMessage());
		} finally {
			try {
				if (boss != null) {
					boss.flush();
					boss.close();
				}
			} catch (IOException e) {
				fail(e.getMessage());
			}
		}
		
	}
	
	/* A simple mock CrumbIssuer to force scenario of invalid crumb. */
	public class CrumbIssuerProxy extends CrumbIssuer {
		public boolean vcrumb = false; 
		@Override
		public boolean validateCrumb(ServletRequest request, MultipartFormDataParser parser) {return vcrumb;}
		protected String issueCrumb(ServletRequest request, String salt) {return null;}
		@Override
		public boolean validateCrumb(ServletRequest request, String salt, String crumb) {return vcrumb;}
	}
	
	public class FileItemImplProxy implements FileItem {
		public InputStream getInputStream() throws IOException { return null; }
		public String getContentType() { return null; }
		public String getName() { return null; }
		public boolean isInMemory() { return false;	}
		public long getSize() { return 0; }
		public byte[] get() { return null; }
		public String getString(String encoding) throws UnsupportedEncodingException { return null; }
		public String getString() { return null; }
		public void write(File file) throws Exception {throw new Exception ("Mock exception has occurred"); }
		public void delete() {}
		public String getFieldName() { return null; }
		public void setFieldName(String name) {}
		public boolean isFormField() { return false; }
		public void setFormField(boolean state) {}
		public OutputStream getOutputStream() throws IOException { return null; }
	}
}
