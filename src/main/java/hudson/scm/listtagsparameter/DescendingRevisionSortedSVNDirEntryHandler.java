/*
 * The MIT License
 *
 * Copyright 2011 Patrick van Dissel (id:pvdissel).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package hudson.scm.listtagsparameter;

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
    @Override
    public List<String> getDirectoryNames() {
        return new LinkedList<String>(dirs.descendingMap().values());
    }

    @Override
    public void handleDirEntry(final SVNDirEntry dirEntry) throws SVNException {
        if (!dirEntry.getKind().equals(SVNNodeKind.DIR)) {
            return;
        }
        String directoryName = Util.removeTrailingSlash(dirEntry.getName());
        dirs.put(Long.valueOf(dirEntry.getRevision()), directoryName);
    }
}
