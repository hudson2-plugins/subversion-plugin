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
package org.eclipse.hudson.scm.subversion.util;

import java.util.Date;
import org.eclipse.hudson.scm.subversion.RevisionParameterAction;
import org.eclipse.hudson.scm.subversion.SubversionSCM;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Class contains help methods to process subversion revisions.
 * <p/>
 * Date: 4/19/11
 *
 * @author Anton Kozak
 */
public final class RevisionUtil {

    public static final char AT_SYMBOL = '@';

    /**
     * Private constructor.
     */
    private RevisionUtil() {
    }

    /**
     * Returns {@link org.tmatesoft.svn.core.wc.SVNRevision} based on the following logic:
     * <li>if repository URL is {@code url@rev} then this method returns specified revision.
     * <li>if the build is parameterized and it contains {RevisionParameterAction} mapped to specified repository URL then
     * {@link SvnInfo.revision}  will be used
     * <li>if {@code QUEUE_TIME} policy was chosen then revision created base on build scheduled time will be used
     * <li>if {@code BUILD_TIME} policy was chosen then revision created base on build run time will be used
     * <li>if {@code HEAD} policy was chosen then HEAD revision will be used
     * <p/>
     *
     * @param location module location.
     * @param revisionPolicy global revision policy.
     * @param queueTime when the build is scheduled.
     * @param buildTime when the build is started.
     * @param revisionParameterAction revisionParameterAction.
     * @return {@link org.tmatesoft.svn.core.wc.SVNRevision}.
     */
    public static SVNRevision getRevision(SubversionSCM.ModuleLocation location,
                                          RevisionParameterAction revisionParameterAction,
                                          SubversionSCM.RevisionPolicy revisionPolicy,
                                          Date queueTime, Date buildTime) {
        int idx = location.getOriginRemote().lastIndexOf(AT_SYMBOL);
        if(idx > 0) {
            String n = location.getOriginRemote().substring(idx + 1);
            return SVNRevision.parse(n);
        }
        if(revisionParameterAction != null) {
            SVNRevision revision = revisionParameterAction.getRevision(location.getURL());
            if(revision != null) {
                return revisionParameterAction.getRevision(location.getURL());
            }
        }
        switch(revisionPolicy) {
            case QUEUE_TIME:
                return SVNRevision.create(queueTime);
            case BUILD_TIME:
                return SVNRevision.create(buildTime);
            case HEAD:
                return SVNRevision.HEAD;
            default:
                throw new IllegalArgumentException("Unknown revision policy value:" + revisionPolicy.name());
        }
    }
}
