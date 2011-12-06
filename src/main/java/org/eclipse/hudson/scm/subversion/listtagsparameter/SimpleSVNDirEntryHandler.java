/*******************************************************************************
 *
 * Copyright (c) 2010-2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Manufacture Francaise des Pneumatiques Michelin, Romain Seguy, Patrick van Dissel (id:pvdissel).
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion.listtagsparameter;

import hudson.Util;
import java.util.ArrayList;
import java.util.List;
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;

import org.tmatesoft.svn.core.SVNNodeKind;

/**
 * Simple {@link ISVNDirEntryHandler} used to get a list containing all the directories in a given Subversion
 * repository.
 * <p/>
 * @author Romain Seguy (http://openromain.blogspot.com)
 */
public class SimpleSVNDirEntryHandler implements DirectoriesSvnEntryHandler {

    private List<String> dirs = new ArrayList<String>();

    public List<String> getDirectoryNames() {
        return dirs;
    }

    public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
        if (!dirEntry.getKind().equals(SVNNodeKind.DIR)) {
            return;
        }
        dirs.add(Util.removeTrailingSlash(dirEntry.getName()));
    }
}
