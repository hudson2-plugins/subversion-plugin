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
package org.eclipse.hudson.scm.util;

import org.eclipse.hudson.scm.RevisionParameterAction;
import org.eclipse.hudson.scm.SubversionSCM;
import java.util.Calendar;
import java.util.Date;
import junit.framework.TestCase;
import org.eclipse.hudson.scm.util.RevisionUtil;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Test for {@link RevisionUtil}
 * <p/>
 * Date: 4/19/11
 *
 * @author Anton Kozak
 */
public class RevisionUtilTest extends TestCase {

    private static final int REVISION_NUMBER = 13000;
    private static final String SVN_URL = "https://svn.java.net/svn/hudson~svn/trunk/hudson/test-projects/trivial-ant";
    private static final String HEAD_REVISION = "HEAD";
    private Date currDate = Calendar.getInstance().getTime();

    protected void setUp() throws Exception {
    }

    public void testGetRevisionFromRemoteWithNumericRevision(){
        SubversionSCM.ModuleLocation config = new SubversionSCM.ModuleLocation(SVN_URL + RevisionUtil.AT_SYMBOL + REVISION_NUMBER,
            null, null, false);
        SVNRevision revision = RevisionUtil.getRevision(config, null, null, null, null);
        assertTrue(revision.isValid());
        assertEquals(revision.getNumber(), REVISION_NUMBER);
        assertEquals(config.getURL(), SVN_URL);
    }

    public void testGetRevisionFromRemoteWithHeadRevision(){
        SubversionSCM.ModuleLocation config = new SubversionSCM.ModuleLocation(SVN_URL + RevisionUtil.AT_SYMBOL + HEAD_REVISION,
            null, null, false);
        SVNRevision revision = RevisionUtil.getRevision(config, null, null, null, null);
        assertTrue(revision.isValid());
        assertEquals(revision, SVNRevision.HEAD);
        assertEquals(config.getURL(), SVN_URL);
    }

    public void testGetRevisionFromRemoteWithIncorrectRevision(){
        SubversionSCM.ModuleLocation config = new SubversionSCM.ModuleLocation(SVN_URL + RevisionUtil.AT_SYMBOL + "FAKE",
            null, null, false);
        SVNRevision revision = RevisionUtil.getRevision(config, null, null, null, null);
        assertFalse(revision.isValid());
        assertEquals(config.getURL(), SVN_URL+"@FAKE");
    }

    public void testGetRevisionQueueTimePolicy(){
        SubversionSCM.ModuleLocation config = new SubversionSCM.ModuleLocation(SVN_URL, null, null, false);
        SVNRevision revision = RevisionUtil.getRevision(config, null, SubversionSCM.RevisionPolicy.QUEUE_TIME,
            currDate, null);
        assertTrue(revision.isValid());
        assertEquals(revision.getDate(), currDate);
        assertEquals(config.getURL(), SVN_URL);

    }

    public void testGetRevisionBuildTimePolicy(){
        SubversionSCM.ModuleLocation config = new SubversionSCM.ModuleLocation(SVN_URL, null, null, false);
        SVNRevision revision = RevisionUtil.getRevision(config, null, SubversionSCM.RevisionPolicy.BUILD_TIME,
            null, currDate);
        assertTrue(revision.isValid());
        assertEquals(revision.getDate(), currDate);
        assertEquals(config.getURL(), SVN_URL);
    }

    public void testGetRevisionHeadPolicy(){
        SubversionSCM.ModuleLocation config = new SubversionSCM.ModuleLocation(SVN_URL, null, null, false);
        SVNRevision revision = RevisionUtil.getRevision(config, null, SubversionSCM.RevisionPolicy.HEAD,
            null, currDate);
        assertTrue(revision.isValid());
        assertEquals(revision, SVNRevision.HEAD);
        assertEquals(config.getURL(), SVN_URL);
    }

    public void testGetRevisionRevisionParameterAction(){
        SubversionSCM.ModuleLocation config = new SubversionSCM.ModuleLocation(SVN_URL, null, null, false);
        SubversionSCM.SvnInfo svnInfo = new SubversionSCM.SvnInfo(SVN_URL, REVISION_NUMBER);
        RevisionParameterAction action = new RevisionParameterAction(svnInfo);
        SVNRevision revision = RevisionUtil.getRevision(config, action, SubversionSCM.RevisionPolicy.HEAD,
            null, currDate);
        assertTrue(revision.isValid());
        assertEquals(revision.getNumber(), REVISION_NUMBER);
        assertEquals(config.getURL(), SVN_URL);
    }

}
