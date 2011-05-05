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
package hudson.scm.subversion;

import hudson.Extension;
import hudson.Util;
import hudson.scm.SubversionSCM.External;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.util.IOException2;
import hudson.util.StreamCopyThread;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.kohsuke.stapler.DataBoundConstructor;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

/**
 * {@link WorkspaceUpdater} that does a fresh check out.
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

            listener.getLogger().println("Cleaning workspace " + ws.getCanonicalPath());
            Util.deleteContentsRecursive(ws);

            // buffer the output by a separate thread so that the update operation
            // won't be blocked by the remoting of the data
            PipedOutputStream pos = new PipedOutputStream();
            StreamCopyThread sct = new StreamCopyThread("svn log copier", new PipedInputStream(pos),
                listener.getLogger());
            sct.start();

            ModuleLocation location = null;
            try {
                for (final ModuleLocation l : locations) {
                    location = l;
                    SVNDepth svnDepth = getSvnDepth(l.getDepthOption());
                    SVNRevision revision = getRevision(l);
                    listener.getLogger().println("Checking out " + l.remote + " revision: " +
                        (revision != null ? revision.toString() : "null") + " depth:" + svnDepth +
                        " ignoreExternals: " + l.isIgnoreExternalsOption());

                    File local = new File(ws, l.getLocalDir());
                    svnuc.setIgnoreExternals(l.isIgnoreExternalsOption());
                    svnuc.setEventHandler(
                        new SubversionUpdateEventHandler(new PrintStream(pos), externals, local, l.getLocalDir()));
                    svnuc.doCheckout(l.getSVNURL(), local.getCanonicalFile(), SVNRevision.HEAD, revision,
                        svnDepth, true);
                }
            } catch (SVNException e) {
                e.printStackTrace(listener.error("Failed to check out " + location.remote));
                return null;
            } finally {
                try {
                    pos.close();
                } finally {
                    try {
                        sct.join(); // wait for all data to be piped.
                    } catch (InterruptedException e) {
                        throw new IOException2("interrupted", e);
                    }
                }
            }

            return externals;
        }
    }
}
