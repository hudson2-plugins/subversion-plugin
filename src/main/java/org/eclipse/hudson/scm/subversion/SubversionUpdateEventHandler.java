/*******************************************************************************
 *
 * Copyright (c) 2004-2009 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi, David Seymore, Renaud Bruyeron, Yahoo! Inc.
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import hudson.remoting.Which;
import org.eclipse.hudson.scm.subversion.SubversionSCM.External;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;

/**
 * Just prints out the progress of svn update/checkout operation in a way similar to
 * the svn CLI.
 * <p/>
 * This code also records all the referenced external locations.
 */
final class SubversionUpdateEventHandler extends SubversionEventHandlerImpl {
    private static final Logger LOGGER = Logger.getLogger(SubversionUpdateEventHandler.class.getName());
    /**
     * External urls that are fetched through svn:externals.
     * We add to this collection as we find them.
     */
    private final List<External> externals;
    /**
     * Relative path from the workspace root to the module root.
     */
    private final String modulePath;

    public SubversionUpdateEventHandler(PrintStream out, List<External> externals, File moduleDir, String modulePath) {
        super(out, moduleDir);
        this.externals = externals;
        this.modulePath = modulePath;
    }

    public void handleEvent(SVNEvent event, double progress) throws SVNException {
        File file = event.getFile();
        String path = null;
        if (file != null) {
            try {
                path = getRelativePath(file);
            } catch (IOException e) {
                throw new SVNException(SVNErrorMessage.create(SVNErrorCode.FS_GENERAL), e);
            }
            path = getLocalPath(path);
        }

        /*
         * Gets the current action. An action is represented by SVNEventAction.
         * In case of an update an  action  can  be  determined  via  comparing
         * SVNEvent.getAction() and SVNEventAction.UPDATE_-like constants.
         */
        SVNEventAction action = event.getAction();
        if (action == SVNEventAction.UPDATE_EXTERNAL) {
            // for externals definitions
            SVNExternal ext = event.getExternalInfo();
            if (ext == null) {
                // prepare for the situation where the user created their own svnkit
                URL jarFile = null;
                try {
                    jarFile = Which.jarURL(SVNEvent.class);
                } catch (IOException e) {
                    LOGGER.log(Level.FINEST, "Exception occurred while loading SVNEvent", e);
                    // ignore this failure
                }
                out.println("AssertionError: appears to be using unpatched svnkit at " + jarFile);
            } else {
                out.println(Messages.SubversionUpdateEventHandler_FetchExternal(
                    ext.getResolvedURL(), ext.getRevision().getNumber(), event.getFile()));
                //#1539 - an external inside an external needs to have the path appended 
                externals.add(
                    new External(modulePath + "/" + path.substring(0, path.length() - ext.getPath().length()), ext));
            }
            return;
        }
        if (action == SVNEventAction.SKIP && event.getExpectedAction() == SVNEventAction.UPDATE_EXTERNAL
            && event.getNodeKind() == SVNNodeKind.FILE) {
            // svn:externals file support requires 1.6 workspace
            out.println(
                "svn:externals to a file requires Subversion 1.6 workspace support. Use the system configuration to enable that.");
        }

        super.handleEvent(event, progress);
    }

    public void checkCancelled() throws SVNCancelException {
        if (Thread.interrupted()) {
            throw new SVNCancelException();
        }
    }
}