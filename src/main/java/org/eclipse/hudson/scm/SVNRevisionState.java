/*******************************************************************************
 *
 * Copyright (c) 2004-2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi, Jean-Baptiste Quenot,
 * Seiji Sogabe, Alan Harder, Vojtech Habarta, Yahoo! Inc.
 *
 *******************************************************************************/
package org.eclipse.hudson.scm;

import hudson.scm.SCMRevisionState;
import java.io.Serializable;
import java.util.Map;

/**
 * {@link hudson.scm.SCMRevisionState} for {@link SubversionSCM}. {@link Serializable} since we compute
 * this remote.
 */
final class SVNRevisionState extends SCMRevisionState implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * All the remote locations that we checked out. This includes those that are specified
     * explicitly via {@link SubversionSCM#getLocations()} as well as those that
     * are implicitly pulled in via svn:externals, but it excludes those locations that
     * are added via svn:externals in a way that fixes revisions.
     */
    final Map<String,Long> revisions;

    SVNRevisionState(Map<String, Long> revisions) {
        this.revisions = revisions;
    }

}
