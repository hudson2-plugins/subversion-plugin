/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi, Fulvio Cavarretta, Jean-Baptiste Quenot, Luca Domenico Milanesio,
 * Renaud Bruyeron, Stephen Connolly, Tom Huybrechts, Yahoo! Inc., Manufacture Francaise des Pneumatiques Michelin,
 * Romain Seguy
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import hudson.Extension;
import hudson.model.Hudson;
import org.eclipse.hudson.scm.subversion.SubversionSCM.External;
import org.eclipse.hudson.scm.subversion.SubversionSCM.ModuleLocation;
import org.eclipse.hudson.scm.subversion.SubversionSCM.SvnInfo;
import hudson.triggers.SCMTrigger;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link WorkspaceUpdater} that uses "svn update" as much as possible.
 * 
 * @author Kohsuke Kawaguchi
 */
public class UpdateUpdater extends WorkspaceUpdater {
    @DataBoundConstructor
    public UpdateUpdater() {
    }

    @Override
    public UpdateTask createTask() {
        return new TaskImpl();
    }

    public static class TaskImpl extends UpdateTask {
        /**
         * Returns true if we can use "svn update" instead of "svn checkout"
         */
        protected boolean isUpdatable() throws IOException {
            for (ModuleLocation l : locations) {
                String moduleName = l.getLocalDir();
                File module = new File(ws,moduleName).getCanonicalFile(); // canonicalize to remove ".." and ".". See #474

                if(!module.exists()) {
                    listener.getLogger().println("Checking out a fresh workspace because "+module+" doesn't exist");
                    return false;
                }

                try {
                    SVNInfo svnkitInfo = parseSvnInfo(module);
                    SvnInfo svnInfo = new SvnInfo(svnkitInfo);

                    String url = l.getURL();
                    if(!svnInfo.url.equals(url)) {
                        listener.getLogger().println("Checking out a fresh workspace because the workspace is not "+url);
                        return false;
                    }
                } catch (SVNException e) {
                    if (e.getErrorMessage().getErrorCode()==SVNErrorCode.WC_NOT_DIRECTORY) {
                        listener.getLogger().println("Checking out a fresh workspace because there's no workspace at "+module);
                    } else {
                        listener.getLogger().println("Checking out a fresh workspace because Hudson failed to detect the current workspace "+module);
                        e.printStackTrace(listener.error(e.getMessage()));
                    }
                    return false;
                }
            }
            return true;
        }

        /**
         * Gets the SVN metadata for the given local workspace.
         *
         * @param workspace
         *      The target to run "svn info".
         */
        private SVNInfo parseSvnInfo(File workspace) throws SVNException {
            final SVNWCClient svnWc = manager.getWCClient();
            return svnWc.doInfo(workspace,SVNRevision.WORKING);
        }

        @Override
        public List<External> perform() throws IOException, InterruptedException {
            if (!isUpdatable())
                return delegateTo(new CheckoutUpdater());


            final SVNUpdateClient svnuc = manager.getUpdateClient();
            final List<External> externals = new ArrayList<External>(); // store discovered externals to here

            for (final ModuleLocation l : locations) {
                try {
                    File local = new File(ws, l.getLocalDir());
                    svnuc.setEventHandler(new SubversionUpdateEventHandler(listener.getLogger(), externals, local, l.getLocalDir()));

                    svnuc.setIgnoreExternals(l.isIgnoreExternalsOption());
                    preUpdate(l, local);

                    SVNDepth svnDepth = getSvnDepth(l.getDepthOption());
                    SVNRevision revision = getRevision(l);

                    listener.getLogger().println("Updating " + l.remote + " revision: " +
                        (revision != null ? revision.toString() : "null") + " depth:" + svnDepth +
                        " ignoreExternals: " + l.isIgnoreExternalsOption());
                    svnuc.doUpdate(local.getCanonicalFile(), revision, svnDepth, true, false);
                } catch (SVNCancelException e) {
                    listener.error("Svn command was aborted");
                    throw (InterruptedException) new InterruptedException().initCause(e);
                } catch (final SVNException e) {
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_LOCKED) {
                        // work space locked. try fresh check out
                        listener.getLogger().println("Workspace appear to be locked, so getting a fresh workspace");
                        return delegateTo(new CheckoutUpdater());
                    }
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_OBSTRUCTED_UPDATE) {
                        // HUDSON-1882. If existence of local files cause an update to fail,
                        // revert to fresh check out
                        listener.getLogger().println(e.getMessage()); // show why this happened. Sometimes this is caused by having a build artifact in the repository.
                        listener.getLogger().println("Updated failed due to local files. Getting a fresh workspace");
                        return delegateTo(new CheckoutUpdater());
                    }

                    e.printStackTrace(listener.error("Failed to update " + l.remote));
                    // trouble-shooting probe for #591
                    if (e.getErrorMessage().getErrorCode() == SVNErrorCode.WC_NOT_LOCKED) {
                        listener.getLogger().println("Polled jobs are " + Hudson.getInstance().getDescriptorByType(SCMTrigger.DescriptorImpl.class).getItemsBeingPolled());
                    }
                    return null;
                }
            }

            return externals;
        }

        /**
         * Hook for subtype to perform some cleanup activity before "svn update" takes place.
         *
         * @param module
         *      Remote repository that corresponds to the workspace.
         * @param local
         *      Local directory that gets the update from the module.
         */
        protected void preUpdate(ModuleLocation module, File local) throws SVNException, IOException {
            // noop by default
        }
    }

    @Extension(ordinal=100) // this is the default, so given a higher ordinal
    public static class DescriptorImpl extends WorkspaceUpdaterDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.UpdateUpdater_DisplayName();
        }
    }
}
