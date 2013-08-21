/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Oracle, Steven Christou(schristou88).
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.scm.subversion;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.Email;
import org.jvnet.hudson.test.For;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.wc.SVNRevision;

import com.thoughtworks.xstream.XStream;

import hudson.XmlFile;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.scm.AbstractSubversionTest;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.External;
import hudson.util.XStream2;

/**
 * @author schristou88
 *
 */
public class SVNExternalsTest extends AbstractSubversionTest {
	/**
	 * This test is designed for verifying that the svnexternals.txt file gets written with the correct information.
	 * @throws Exception 
	 * 
	 */
	@Bug(390648)
	@Test
	public void testSVNExternalsFile() throws Exception {
        FreeStyleProject p = hudson.createProject(FreeStyleProject.class, "svnExternal");
        SubversionSCM scm = new SubversionSCM("https://tsethudsonsvn.googlecode.com/svn/trunk");
        p.setScm(scm);
        FreeStyleBuild build = assertBuildStatusSuccess(p.scheduleBuild2(0));
        
        assertEquals("Build should have succeeded, but did not.", build.getResult(), Result.SUCCESS);
        
        File file = new File(p.getRootDir() + "/svnexternals.txt");
        assertNotNull("Error with file: svnexternals.txt", file);
        assertTrue("File does not exist.", file.exists());
        
        @SuppressWarnings("unchecked")
        List<SubversionSCM.External> externals = (List<External>) new XmlFile(XSTREAM, file).read();
        assertTrue("Externals file is empty.", !externals.isEmpty());
        
        for (SubversionSCM.External external : externals) {
        	assertNotNull("External PATH is null.", external.path);
        	assertNotNull("External URL is null.", external.url);
        	assertNotNull("External REVISION is null", external.revision);
        }
        
	}
	
	@Email("http://www.eclipse.org/forums/index.php?t=rview&goto=1028723#msg_1028723")
	@Test
	public void testSVNUpdateStrategy() throws Exception {
		FreeStyleProject p = hudson.createProject(FreeStyleProject.class, "svnExternal");
		
		SubversionSCM scm = new SubversionSCM("https://tsethudsonsvn.googlecode.com/svn/trunk");
//		UpdateWithRevertUpdater
		
		
	}
	
	
    private static final XStream XSTREAM = new XStream2();

    static {
        XSTREAM.alias("external", External.class);
    }
}
