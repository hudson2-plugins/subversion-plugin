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
package org.eclipse.hudson.scm.subversion;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.TaskListener;
import org.eclipse.hudson.scm.subversion.SubversionSCM.External;
import org.eclipse.hudson.scm.subversion.SubversionSCM.ModuleLocation;
import org.eclipse.hudson.scm.subversion.util.RevisionUtil;
import org.kohsuke.stapler.export.ExportedBean;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Encapsulates the logic of how files are obtained from a subversion repository.
 *
 * <p>
 * {@link WorkspaceUpdater} serves as a {@link Describable}, created from the UI via databinding and
 * encapsulates whatever configuration parameter. The checkout logic is in {@link UpdateTask}.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.23
 */
@ExportedBean
public abstract class WorkspaceUpdater extends AbstractDescribableImpl<WorkspaceUpdater> implements ExtensionPoint, Serializable {
    /**
     * Creates the {@link UpdateTask} instance, which performs the actual check out / update.
     */
    public abstract UpdateTask createTask();

    @Override
    public WorkspaceUpdaterDescriptor getDescriptor() {
        return (WorkspaceUpdaterDescriptor)super.getDescriptor();
    }

    /**
     * This object gets instantiated on the master and then sent to the slave via remoting,
     * then used to {@linkplain #perform() perform the actual checkout activity}.
     *
     * <p>
     * A number of contextual objects are defined as fields, to be used by the {@link #perform()} method.
     * These fields are set by {@link SubversionSCM} before the invocation.
     */
    public static abstract class UpdateTask implements Serializable {
        private static final long serialVersionUID = 1L;

        // fields that are set by the caller as context for the perform method

        /**
         * Factory for various subversion commands.
         */
        protected SVNClientManager manager;

        /**
         * Encapusulates the authentication. Connected back to Hudson master. Never null.
         */
        protected ISVNAuthenticationProvider authProvider;

        /**
         * When the build was scheduled.
         */
        protected Date queueTime;

        /**
         * When the build was started.
         */
        protected Date buildTime;

        /**
         * Connected to build console. Never null.
         */
        protected TaskListener listener;

        /**
         * Modules to check out. Never null.
         */
        protected ModuleLocation[] locations;

        /**
         * Build workspace. Never null.
         */
        protected File ws;

        /**
         * If the build parameter is specified with specific version numbers, this field captures that. Can be null.
         */
        protected RevisionParameterAction revisionParameterAction;

        /**
         * Global defined revision policy.
         */
        protected SubversionSCM.RevisionPolicy revisionPolicy;

        /**
         * Performs the checkout/update.
         *
         * <p>
         * Use the fields defined in this class that defines the parameters of the check out.
         *
         * @return
         *      Where svn:external mounting happened. Can be empty but never null.
         */
        public abstract List<External> perform() throws IOException, InterruptedException;

        protected List<External> delegateTo(UpdateTask t) throws IOException, InterruptedException {
            t.manager = this.manager;
            t.authProvider = this.authProvider;
            t.queueTime = this.queueTime;
            t.buildTime = this.buildTime;
            t.listener = this.listener;
            t.locations = this.locations;
            t.revisionParameterAction = this.revisionParameterAction;
            t.ws = this.ws;
            t.revisionPolicy = this.revisionPolicy;

            return t.perform();
        }

        /**
         * Delegates the execution to another updater. This is most often useful to fall back to the fresh check out
         * by using {@link CheckoutUpdater}.
         */
        protected final List<External> delegateTo(WorkspaceUpdater wu) throws IOException, InterruptedException {
            return delegateTo(wu.createTask());
        }

        /**
         * Determines the revision to check out for the given location.
         */
        protected SVNRevision getRevision(ModuleLocation location) {
            return RevisionUtil.getRevision(location, revisionParameterAction, revisionPolicy, queueTime, buildTime);
        }

        /**
         * Returns {@link org.tmatesoft.svn.core.SVNDepth} by string value.
         *
         * @return {@link org.tmatesoft.svn.core.SVNDepth} value.
         */
        protected static SVNDepth getSvnDepth(String name) {
            return SVNDepth.fromString(name);
        }

        /**
         * Sets svn manager. For the tests only.
         *
         * @param manager manager.
         */
        void setManager(SVNClientManager manager) {
            this.manager = manager;
        }

        /**
         * Sets auth provider.  For the tests only.
         *
         * @param authProvider auth provider.
         */
        void setAuthProvider(ISVNAuthenticationProvider authProvider) {
            this.authProvider = authProvider;
        }

        /**
         * Sets svn locations.  For the tests only.
         *
         * @param locations svn locations.
         */
        void setLocations(ModuleLocation[] locations) {
            this.locations = locations;
        }

        /**
         * Sets workspace. For the tests only.
         *
         * @param ws workspace.
         */
        void setWs(File ws) {
            this.ws = ws;
        }

        /**
         * Sets revision policy. For the tests only.
         *
         * @param revisionPolicy revision policy.
         */
        void setRevisionPolicy(SubversionSCM.RevisionPolicy revisionPolicy) {
            this.revisionPolicy = revisionPolicy;
        }

        /**
         * Sets listener.  For the tests only.
         *
         * @param listener listener.
         */
        void setListener(TaskListener listener) {
            this.listener = listener;
        }
    }
}
