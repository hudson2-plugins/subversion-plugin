/*
  * The MIT License
  *
  * Copyright (c) 2011, Oracle Corporation, Anton Kozak
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

import hudson.scm.SubversionSCM;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc2.SvnWcGeneration;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;

import java.io.File;
import java.io.IOException;

import static org.easymock.EasyMock.*;

/**
 * Test for {@link CheckoutUpdater}
 * <p/>
 * Date: 5/13/11
 *
 * @author Anton Kozak
 */
public class CheckoutUpdaterTest {

    @Test
    public void testUpdateTask() throws IOException, InterruptedException, SVNException {
        WorkspaceUpdater.UpdateTask task = new CheckoutUpdater().createTask();
        SVNClientManager manager = createMock(SVNClientManager.class);
        task.setManager(manager);
        SVNUpdateClient svnuc = createMock(SVNUpdateClient.class);
        expect(manager.getUpdateClient()).andReturn(svnuc);
        SubversionSCM.ModuleLocation location = new SubversionSCM.ModuleLocation("http://localhost/test1", "test1",
                SVNDepth.INFINITY.getName(), false);
        SubversionSCM.ModuleLocation[] locations = new SubversionSCM.ModuleLocation[]{location};
        task.setLocations(locations);
        File ws = new File("./target/workspace");
        task.setWs(ws);
        task.setRevisionPolicy(SubversionSCM.RevisionPolicy.HEAD);
        svnuc.setIgnoreExternals(false);
        svnuc.setEventHandler((ISVNEventHandler) anyObject());
        expect(svnuc.getOperationsFactory()).andReturn(new SvnOperationFactory());
        expect(Long.valueOf(svnuc.doCheckout(location.getSVNURL(), new File(ws, location.getLocalDir()).getCanonicalFile(),
                SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true))).andReturn(Long.valueOf(100));
        replay(manager, svnuc);
        task.perform();
        verify(manager, svnuc);
    }
}
