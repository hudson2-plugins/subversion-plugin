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
 * Kohsuke Kawaguchi, Bruce Chapman, Yahoo! Inc., Anton Kozak
 *
 *******************************************************************************/
package hudson.model;

import hudson.Proc;
import hudson.util.StreamTaskListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.eclipse.hudson.scm.subversion.AbstractSubversionTest;
import org.eclipse.hudson.scm.subversion.CheckoutUpdater;
import org.eclipse.hudson.scm.subversion.SubversionSCM;
import org.eclipse.hudson.scm.subversion.UpdateUpdater;
import org.eclipse.hudson.scm.subversion.UpdateWithRevertUpdater;
import org.eclipse.hudson.scm.subversion.WorkspaceUpdater;
import org.jvnet.hudson.test.Bug;


/**
 * @author Kohsuke Kawaguchi
 */
public class LegacyConfigTest extends AbstractSubversionTest {

    /**
     * Ensures that the introduction of {@link org.eclipse.hudson.scm.subversion.WorkspaceUpdater} maintains backward compatibility with
     * existing data.
     */
    public void testWorkspaceUpdaterCompatibility() throws Exception {
        Proc p = runSvnServe(getClass().getResource("small.zip"));
        SubversionSCM.initialize();
        try {
            verifyCompatibility("legacy-update.xml", UpdateUpdater.class);
            verifyCompatibility("legacy-checkout.xml", CheckoutUpdater.class);
            verifyCompatibility("legacy-revert.xml", UpdateWithRevertUpdater.class);
        } finally {
            p.kill();
        }
    }

    /**
     * Used for experimenting the memory leak problem.
     * This test by itself doesn't detect that, but I'm leaving it in anyway.
     */
    @Bug(8061)
    public void testPollingLeak() throws Exception {
        Proc p = runSvnServe(getClass().getResource("small.zip"));
        try {
            FreeStyleProject b = createFreeStyleProject();
            b.setScm(new SubversionSCM("svn://localhost/"));
            b.setAssignedNode(createSlave());

            assertBuildStatusSuccess(b.scheduleBuild2(0));

            b.poll(new StreamTaskListener(System.out, Charset.defaultCharset()));
        } finally {
            p.kill();
        }
    }

    private void verifyCompatibility(String resourceName, Class<? extends WorkspaceUpdater> expected)
        throws IOException {
        InputStream io = null;
        AbstractProject job = null;
        try {
            io = getClass().getResourceAsStream(resourceName);
            job = (AbstractProject) hudson.createProjectFromXML("update", io);
            job.initProjectProperties();
            job.convertScmProperty();

        } finally {
            IOUtils.closeQuietly(io);
        }
        assertEquals(expected, ((SubversionSCM) job.getScm()).getWorkspaceUpdater().getClass());
    }
}
