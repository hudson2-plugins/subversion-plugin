package hudson.scm;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import hudson.Proc;
import hudson.model.FreeStyleBuild;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.scm.SubversionSCM;
import hudson.triggers.SCMTrigger;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.util.SVNURLUtil;
import org.tmatesoft.svn.core.internal.wc.SVNCommitMediator;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.ISVNWorkspaceMediator;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.io.diff.SVNDiffWindow;

import sun.security.x509.DeltaCRLIndicatorExtension;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebConnection;
import com.gargoylesoftware.htmlunit.WebRequestSettings;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.sun.corba.se.impl.orbutil.closure.Future;

import antlr.ANTLRException;

public class SubversionPostCommitTest extends AbstractSubversionTest {
	private SVNRepository repository;

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		FileUtils.deleteQuietly(new File("/tmp/399165"));
	}
	
	@Test
	public void testPostCommitTrigger() throws Exception {
        // Disable crumbs because HTMLUnit refuses to mix request bodies with
        // request parameters
        hudson.setCrumbIssuer(null);
        
        FreeStyleProject p = createFreeStyleProject();
        String url = "https://tsethudsonsvn.googlecode.com/svn/trunk";
        SCMTrigger trigger = new SCMTrigger("0 */6 * * *");

        p.setScm(new SubversionSCM(url));
        p.addTrigger(trigger);
        trigger.start(p, true);

        String repoUUID = "b703df53-fdd9-0691-3d8c-58db40123d9f";

        WebClient wc = new WebClient();
        WebRequestSettings wr = new WebRequestSettings(new URL(getURL() + "subversion/" + repoUUID + "/notifyCommit"), HttpMethod.POST);
        wr.setRequestBody("A   trunk/testcommit.txt");
        wr.setAdditionalHeader("Content-Type", "text/plain;charset=UTF-8");

        wr.setAdditionalHeader("X-Hudson-Subversion-Revision", "16");
        
        WebConnection conn = wc.getWebConnection();
        WebResponse resp = conn.getResponse(wr);
        assertTrue(isGoodHttpStatus(resp.getStatusCode()));

        waitUntilNoActivity();
        FreeStyleBuild b = p.getLastBuild();
        assertNotNull(b);
        assertBuildStatus(Result.SUCCESS,b);

        SVNRevisionState revisionState = b.getAction(SVNRevisionState.class);

        assertNotNull("Failed to find revision", revisionState);
        
        assertNotNull("Failed to find revision", revisionState.revisions.get(url));
        
        assertEquals(16, revisionState.revisions.get(url).longValue());
	}
	
	/**
	 * This test unit is for testing a commit hook using the UUID
	 */
	@Bug(399165)
	@Test
	public void testPrebuiltCommitTrigger() throws Exception{
        hudson.setCrumbIssuer(null);

		// First create repository with 1 file and commit information
		SVNCommitInfo info = createSVNRepository();
		assertNull(info.getErrorMessage());
		assertEquals("Failed to create 1 revision.", 1, info.getNewRevision());
		
		// Create freestyle project with SVN SCM.
		FreeStyleProject project = createFreeStyleProject();
		project.setScm(new SubversionSCM("file:///tmp/399165"));
        SCMTrigger trigger = new SCMTrigger("0 */6 * * *");
        project.addTrigger(trigger);
        trigger.start(project, true);
		
		// Execute build (This is critical for fixing eclipse bug: 399165)
		assertBuildStatusSuccess(project.scheduleBuild2(0));
		
		// Commit a file again.
		info = createSecondCommit();
		assertNull(info.getErrorMessage());
		assertEquals("Failed to create second commit.", 2, info.getNewRevision());
		
		// Create post-commit hook
        WebClient wc = new WebClient();
        WebRequestSettings wr = new WebRequestSettings(new URL(getURL() + "subversion/" + repository.getRepositoryUUID(false) + "/notifyCommit"), HttpMethod.POST);
        wr.setRequestBody("A   dirB/file2.txt");
        wr.setAdditionalHeader("Content-Type", "text/plain;charset=UTF-8");

        wr.setAdditionalHeader("X-Hudson-Subversion-Revision", "2");
        
        WebConnection conn = wc.getWebConnection();
        System.out.println(wr);
        WebResponse resp = conn.getResponse(wr);
        assertTrue(isGoodHttpStatus(resp.getStatusCode()));
        
        waitUntilNoActivity();
        FreeStyleBuild b = project.getLastBuild();
        assertNotNull(b);
        
        assertBuildStatus(Result.SUCCESS,b);
        
        assertEquals("Failed to execute a buid.", 2, b.getNumber());
	}
	
	private SVNCommitInfo createSecondCommit() throws SVNException {
		String logMessage = "test second commit";
		ISVNWorkspaceMediator mediator = new PostCommitWorkspaceMediator();
		
		ISVNEditor editor = repository.getCommitEditor(logMessage, mediator);
		
		editor.openRoot(1);
		editor.addFile("dirB/file2.txt", null, -1);
		editor.applyTextDelta("dirB/file2.txt", null);
		
		OutputStream os = editor.textDeltaChunk("dirB/file2.txt", SVNDiffWindow.EMPTY);
		
		editor.textDeltaEnd("dirB/file2.txt");
		editor.closeFile("dirB/file2.txt", null);
		return editor.closeEdit();
	}
	
	/**
	 * I take no credit for the below code. The creation of the svn repository is provided by the svnkit library guys at:
	 * http://wiki.svnkit.com/Setting_Up_A_Subversion_Repository
	 * 
	 * @throws SVNException 
	 */
	private SVNCommitInfo createSVNRepository() throws Exception {
		FSRepositoryFactory.setup();
		String repoDir = "/tmp/399165/svn";
		SVNRepositoryFactoryImpl.setup();
		SVNURL repo = SVNRepositoryFactory.createLocalRepository(new File("/tmp/399165"), true, true);
		repository = SVNRepositoryFactory.create(repo);
		String logMessage = "test commit message";
		
		ISVNWorkspaceMediator mediator = new PostCommitWorkspaceMediator();
		
		ISVNEditor editor = repository.getCommitEditor(logMessage, mediator);
		
		editor.openRoot(-1);
		editor.addDir("dirB", null, -1);
		editor.addFile("dirB/file1.txt", null, -1);
		editor.applyTextDelta("dirB/file1.txt", null);
		
		OutputStream os = editor.textDeltaChunk("dirB/file1.txt", SVNDiffWindow.EMPTY);
		
		editor.textDeltaEnd("dirB/file1.txt");
		editor.closeFile("dirB/file1.txt", null);
		return editor.closeEdit();
	}
	
	
	public class PostCommitWorkspaceMediator implements ISVNWorkspaceMediator {

		@Override
		public SVNPropertyValue getWorkspaceProperty(String path,
				String name) throws SVNException {return null;}

		@Override
		public void setWorkspaceProperty(String path, String name,
				SVNPropertyValue value) throws SVNException {}
		
		public OutputStream createTemporaryLocation(String path, Object id) throws IOException {
			ByteArrayOutputStream tempStorageOS = new ByteArrayOutputStream();
			myTmpStorages.put(id, tempStorageOS);
			return tempStorageOS;
		}
		
		public InputStream getTemporaryLocation(Object id) throws IOException {
			return new ByteArrayInputStream(((ByteArrayOutputStream)myTmpStorages.get(id)).toByteArray());
		}
		
		public long getLength(Object id) throws IOException {
			ByteArrayOutputStream tempStorageOS = (ByteArrayOutputStream)myTmpStorages.get(id);
			return tempStorageOS != null ? tempStorageOS.size() : 0;
			
		}
		
		public void deleteTemporaryLocation(Object id) {
			myTmpStorages.remove(id);
		}
		
		private Map myTmpStorages = new HashMap();
	}
}