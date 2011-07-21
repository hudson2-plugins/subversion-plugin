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
 * Kohsuke Kawaguchi, CloudBees, Inc.
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.Hudson;

/**
 * {@link Descriptor} for {@link WorkspaceUpdater}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class WorkspaceUpdaterDescriptor extends Descriptor<WorkspaceUpdater> {

    public static DescriptorExtensionList<WorkspaceUpdater,WorkspaceUpdaterDescriptor> all() {
        return Hudson.getInstance().<WorkspaceUpdater,WorkspaceUpdaterDescriptor>getDescriptorList(WorkspaceUpdater.class);
    }
}
