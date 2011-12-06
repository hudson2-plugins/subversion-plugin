/*******************************************************************************
 *
 * Copyright (c) 2010, CloudBees, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import hudson.Extension;
import org.eclipse.hudson.scm.subversion.SubversionSCM.ModuleLocation;
import java.io.File;
import java.io.IOException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNWCClient;

/**
 * {@link WorkspaceUpdater} that performs "svn revert" + "svn update"
 *
 * @author Kohsuke Kawaguchi
 */
public class UpdateWithRevertUpdater extends WorkspaceUpdater {
    @DataBoundConstructor
    public UpdateWithRevertUpdater() {}

    @Override
    public UpdateTask createTask() {
        return new TaskImpl();
    }

    // mostly "svn update" plus extra
    public static class TaskImpl extends UpdateUpdater.TaskImpl {
        @Override
        protected void preUpdate(ModuleLocation module, File local) throws SVNException, IOException {
            listener.getLogger().println("Reverting " + local + " ignoreExternals: " + module.isIgnoreExternalsOption());
            final SVNWCClient svnwc = manager.getWCClient();
            svnwc.setIgnoreExternals(module.isIgnoreExternalsOption());
            svnwc.doRevert(new File[]{local.getCanonicalFile()}, getSvnDepth(module.getDepthOption()), null);
        }
    }

    @Extension
    public static class DescriptorImpl extends WorkspaceUpdaterDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.UpdateWithRevertUpdater_DisplayName();
        }
    }
}
