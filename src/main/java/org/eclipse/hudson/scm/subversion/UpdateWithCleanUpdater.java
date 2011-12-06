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
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import java.io.File;
import java.io.IOException;

/**
 * {@link WorkspaceUpdater} that removes all the untracked files before "svn update"
 * @author Kohsuke Kawaguchi
 */
public class UpdateWithCleanUpdater extends WorkspaceUpdater {
    @DataBoundConstructor
    public UpdateWithCleanUpdater() {}

    @Override
    public UpdateTask createTask() {
        return new TaskImpl();
    }

    // mostly "svn update" plus extra
    public static class TaskImpl extends UpdateUpdater.TaskImpl {
        @Override
        protected void preUpdate(ModuleLocation module, File local) throws SVNException, IOException {
            listener.getLogger().println("Cleaning up " + local);

            manager.getStatusClient().doStatus(local, null, SVNDepth.INFINITY, false, false, true, false, new ISVNStatusHandler() {
                public void handleStatus(SVNStatus status) throws SVNException {
                    SVNStatusType s = status.getContentsStatus();
                    if (s == SVNStatusType.STATUS_UNVERSIONED || s == SVNStatusType.STATUS_IGNORED || s == SVNStatusType.STATUS_MODIFIED) {
                        listener.getLogger().println("Deleting "+status.getFile());
                        try {
                            File f = status.getFile();
                            if (f.isDirectory())
                                FileUtils.deleteDirectory(f);
                            else
                                f.delete();
                        } catch (IOException e) {
                            throw new SVNException(SVNErrorMessage.UNKNOWN_ERROR_MESSAGE,e);
                        }
                    }
                }
            }, null);
        }
    }

    @Extension
    public static class DescriptorImpl extends WorkspaceUpdaterDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.UpdateWithCleanUpdater_DisplayName();
        }
    }
}

