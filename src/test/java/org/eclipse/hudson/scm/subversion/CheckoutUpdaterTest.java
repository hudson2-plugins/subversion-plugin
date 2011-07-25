/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Anton Kozak
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import org.junit.Test;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import java.io.File;
import java.io.IOException;

import static org.easymock.EasyMock.*;

/**
 * Test for {@link org.eclipse.hudson.scm.subversion.CheckoutUpdater}
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
        expect(svnuc.doCheckout(location.getSVNURL(), new File(ws, location.getLocalDir()).getCanonicalFile(),
                SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true)).andReturn(100L);
        replay(manager, svnuc);
        task.perform();
        verify(manager, svnuc);
    }
}
