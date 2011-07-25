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

import hudson.Extension;
import hudson.Util;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;

/**
 * {@link WorkspaceUpdater} that cleans checkout folders and then performs checkout.
 * <p/>
 * <p/>
 * Copyright (C) 2011 Hudson-CI.org
 * <p/>
 * Date: 5/12/11
 *
 * @author Anton Kozak
 */
public class CheckoutWithLocationFoldersCleanupUpdater extends CheckoutUpdater {
    /**
     * Constructor.
     */
    @DataBoundConstructor
    public CheckoutWithLocationFoldersCleanupUpdater() {
    }

    /**
     * Creates update task.
     *
     * @return {@link UpdateTask}
     */
    public UpdateTask createTask() {
        return new UpdateTaskImpl();
    }

    /**
     * Describes extention.
     */
    @Extension
    public static class DescriptorImpl extends WorkspaceUpdaterDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.CheckoutWithLocationFolderCleanupUpdater_DisplayName();
        }
    }

    /**
     * Update task is the same as {@link CheckoutUpdater.UpdateTaskImpl} except workspace cleaning.
     */
    protected static class UpdateTaskImpl extends CheckoutUpdater.UpdateTaskImpl {
        /**
         * Cleans workspace.
         *
         * @throws java.io.IOException IOException
         */
        protected void cleanupBeforeCheckout() throws IOException {
            for (final SubversionSCM.ModuleLocation location : locations) {
                File local = new File(ws, location.getLocalDir());
                if (listener != null && listener.getLogger() != null) {
                    listener.getLogger().println("Cleaning checkout folder " + local.getCanonicalPath());
                }
                Util.deleteContentsRecursive(local);
            }
        }
    }
}
