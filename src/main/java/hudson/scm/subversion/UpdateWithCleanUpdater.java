/*
 * The MIT License
 *
 * Copyright (c) 2010, CloudBees, Inc.
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

import hudson.Extension;
import hudson.scm.SubversionSCM.ModuleLocation;
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
        protected void preUpdate(ModuleLocation module, File local) throws SVNException {
            listener.getLogger().println("Cleaning up " + local);

            manager.getStatusClient().doStatus(local, null, SVNDepth.INFINITY, false, false, true, false, new ISVNStatusHandler() {
                public void handleStatus(SVNStatus status) throws SVNException {
                    SVNStatusType s = status.getContentsStatus();
                    /*
                     * Perform a delete on the file/directory if any of the following are meet:
                     * 1. The status of the file is unversioned.
                     * 2. The status of the file is ignored.
                     * 3. The status of the file is modified.
                     * 4. Unable to obtain the status of a specific file.
                     * 
                     */
                    if (s == SVNStatusType.STATUS_UNVERSIONED ||
                    	s == SVNStatusType.STATUS_IGNORED ||
                    	s == SVNStatusType.STATUS_MODIFIED ||
                    	s == SVNStatusType.STATUS_NONE) {
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

