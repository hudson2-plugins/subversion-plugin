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
 * Kohsuke Kawaguchi
 *
 *******************************************************************************/

package org.eclipse.hudson.scm;

import hudson.ClassicPluginStrategy;
import hudson.Launcher.LocalLauncher;
import hudson.Proc;
import org.eclipse.hudson.scm.SubversionSCM.DescriptorImpl;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.jvnet.hudson.test.HudsonHomeLoader.CopyExisting;
import org.jvnet.hudson.test.HudsonTestCase;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * Base class for Subversion related tests.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractSubversionTest extends HudsonTestCase {
    protected DescriptorImpl descriptor;
    protected String kind = ISVNAuthenticationManager.PASSWORD;


    @Override
    protected void setUp() throws Exception {
        //Enable classic plugin strategy, because some extensions are duplicated with default strategy
        System.setProperty("hudson.PluginStrategy", "hudson.ClassicPluginStrategy");
        super.setUp();
        descriptor = hudson.getDescriptorByType(DescriptorImpl.class);
    }

    protected Proc runSvnServe(URL zip) throws Exception {
        return runSvnServe(new CopyExisting(zip).allocate());
    }

    /**
     * Runs svnserve to serve the specified directory as a subversion repository.
     */
    protected Proc runSvnServe(File repo) throws Exception {
        LocalLauncher launcher = new LocalLauncher(new StreamTaskListener(System.out));
        try {
            launcher.launch().cmds("svnserve", "--help").start().join();
        } catch (IOException e) {
            // if we fail to launch svnserve, skip the test
            return null;
        }
        return launcher.launch().cmds(
            "svnserve", "-d", "--foreground", "-r", repo.getAbsolutePath()).pwd(repo).start();
    }

    protected ISVNAuthenticationManager createInMemoryManager() {
        ISVNAuthenticationManager m = SVNWCUtil.createDefaultAuthenticationManager(hudson.root, null, null, false);
        m.setAuthenticationProvider(descriptor.createAuthenticationProvider(null));
        return m;
    }

    static {
        ClassicPluginStrategy.useAntClassLoader = true;
    }
}
