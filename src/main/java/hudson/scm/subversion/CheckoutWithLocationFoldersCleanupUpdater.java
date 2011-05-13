/*
  * The MIT License
  *
  * Copyright (c) 2011, Oracle Corporation, Anton Kozak
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
import hudson.scm.SubversionSCM;
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
