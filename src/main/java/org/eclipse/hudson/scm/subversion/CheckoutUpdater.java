/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi, Fulvio Cavarretta, Jean-Baptiste Quenot,
 * Luca Domenico Milanesio, Renaud Bruyeron, Stephen Connolly,
 * Tom Huybrechts, Yahoo! Inc., Manufacture Francaise des Pneumatiques Michelin,
 * Romain Seguy, Anton Kozak
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import hudson.Extension;
import hudson.Util;
import org.eclipse.hudson.scm.subversion.SubversionSCM.External;
import org.eclipse.hudson.scm.subversion.SubversionSCM.ModuleLocation;
import hudson.util.IOException2;
import hudson.util.StreamCopyThread;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


/**
 * {@link WorkspaceUpdater} that cleans workspace and then performs checkout.
 *
 * @author Kohsuke Kawaguchi
 */
public class CheckoutUpdater extends WorkspaceUpdater {
    @DataBoundConstructor
    public CheckoutUpdater() {
    }

    public UpdateTask createTask() {
        return new UpdateTaskImpl();
    }

    @Extension
    public static class DescriptorImpl extends WorkspaceUpdaterDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.CheckoutUpdater_DisplayName();
        }
    }

    protected static class UpdateTaskImpl extends UpdateTask {
        public List<External> perform() throws IOException, InterruptedException {
            final SVNUpdateClient svnuc = manager.getUpdateClient();
            final List<External> externals = new ArrayList<External>(); // store discovered externals to here

            cleanupBeforeCheckout();

            // buffer the output by a separate thread so that the update operation
            // won't be blocked by the remoting of the data
            PipedOutputStream pos = new PipedOutputStream();
            StreamCopyThread sct = null;
            if (listener != null) {
                sct = new StreamCopyThread("svn log copier", new PipedInputStream(pos),
                        listener.getLogger());
                sct.start();
            }
            ModuleLocation location = null;
            try {
                for (final ModuleLocation l : locations) {
                    location = l;
                    SVNDepth svnDepth = getSvnDepth(l.getDepthOption());
                    SVNRevision revision = getRevision(l);
                    if (listener != null) {
                        listener.getLogger().println("Checking out " + l.remote + " revision: " +
                                (revision != null ? revision.toString() : "null") + " depth:" + svnDepth +
                                " ignoreExternals: " + l.isIgnoreExternalsOption());
                    }
                    File local = new File(ws, l.getLocalDir());
                    svnuc.setIgnoreExternals(l.isIgnoreExternalsOption());
                    svnuc.setEventHandler(
                            new SubversionUpdateEventHandler(new PrintStream(pos), externals, local, l.getLocalDir()));
                    svnuc.doCheckout(l.getSVNURL(), local.getCanonicalFile(), SVNRevision.HEAD, revision,
                            svnDepth, true);
                }
            } catch (SVNCancelException e) {
                listener.error("Svn command was canceled");
                throw (InterruptedException) new InterruptedException().initCause(e);
            } catch (SVNException e) {
                e.printStackTrace(listener.error("Failed to check out " + location.remote));
                return null;
            } finally {
                try {
                    pos.close();
                } finally {
                    try {
                        if (sct != null) {
                            sct.join(); // wait for all data to be piped.
                        }
                    } catch (InterruptedException e) {
                        throw new IOException2("interrupted", e);
                    }
                }
            }

            return externals;
        }

        /**
         * Cleans workspace.
         *
         * @throws IOException IOException
         */
        protected void cleanupBeforeCheckout() throws IOException {
            if (listener != null && listener.getLogger() != null) {
                listener.getLogger().println("Cleaning workspace " + ws.getCanonicalPath());
            }
            Util.deleteContentsRecursive(ws);
        }
    }
}
