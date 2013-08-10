package hudson.scm.subversion;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.scm.AbstractSubversionTest;
import hudson.scm.PerJobCredentialStore;
import hudson.scm.PerJobCredentialStoreTest;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.scm.SubversionSCM.DescriptorImpl.Credential;
import hudson.scm.subversion.UpdateWithRevertUpdater.TaskImpl;
import hudson.scm.subversion.WorkspaceUpdater.UpdateTask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNException;

public class UpdateWithRevertUpdaterTest extends AbstractSubversionTest {
	public void testSVNURLWithSpaces() throws Exception {
		FreeStyleProject proj = createFreeStyleProject("SVNUrlWithSpaces");
		SubversionSCM scm = new SubversionSCM("https://tsethudsonsvn.googlecode.com/svn/ branch");
		
		scm.setWorkspaceUpdater(new UpdateWithRevertUpdater());
		PerJobCredentialStore credentialStore = new PerJobCredentialStore(proj, null);
        SubversionSCM.DescriptorImpl.Credential credential = new SubversionSCM.DescriptorImpl.PasswordCredential(
            "test", "test");
        credentialStore.acknowledgeAuthentication("Test realm", credential);
		proj.setScm(scm);
	}
	
	public void testPreUpdateRevert() throws Exception {
		FreeStyleProject proj = createFreeStyleProject();
		
		SubversionSCM scm = new SubversionSCM("https://tsethudsonsvn.googlecode.com/svn/trunk/");
		scm.setWorkspaceUpdater(new UpdateWithRevertUpdater());
		proj.setScm(scm);
		
		Future<FreeStyleBuild> build = proj.scheduleBuild2(0);
		assertBuildStatusSuccess(build);
		build = proj.scheduleBuild2(0);
		assertBuildStatusSuccess(build);
		System.out.println(build.get().getLog());
		assertTrue(build.get().getLog().contains("Reverting"));
	}
}