/*
 * The MIT License
 *
 * Copyright (c) 2011, Oracle Corporation, Winston Prakash, Nikita Levyankov, Anton Kozak
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
package hudson.scm.util;

import hudson.scm.RevisionParameterAction;
import hudson.scm.SubversionSCM;
import java.util.Date;
import org.tmatesoft.svn.core.wc.SVNRevision;

public final class RevisionUtil {

    static final char AT_SYMBOL = '@';

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
