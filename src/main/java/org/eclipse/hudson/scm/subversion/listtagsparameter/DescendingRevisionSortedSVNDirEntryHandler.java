/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Patrick van Dissel (id:pvdissel).
 *
 *
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion.listtagsparameter;
import hudson.Util;
import java.util.LinkedList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;

/**
 * {@link ISVNDirEntryHandler} used to get a list containing all the directories in a given Subversion repository sorted
 * by their SVN revision number, in descending order (from new to old).
 */
public class DescendingRevisionSortedSVNDirEntryHandler implements DirectoriesSvnEntryHandler {

    private NavigableMap<Long, String> dirs = new TreeMap<Long, String>();

    /**
     * {@inheritDoc}
     * <p/>Sorted, descending by their revision number.
     */
    public List<String> getDirectoryNames() {
        return new LinkedList<String>(dirs.descendingMap().values());
    }

    public void handleDirEntry(final SVNDirEntry dirEntry) throws SVNException {
        if (!dirEntry.getKind().equals(SVNNodeKind.DIR)) {
            return;
        }
        String directoryName = Util.removeTrailingSlash(dirEntry.getName());
        dirs.put(dirEntry.getRevision(), directoryName);
    }
}
