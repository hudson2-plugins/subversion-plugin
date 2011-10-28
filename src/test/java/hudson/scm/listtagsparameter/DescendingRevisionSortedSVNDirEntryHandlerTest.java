/*
 * The MIT License
 *
 * Copyright 2011 Hudson.
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

import java.util.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;

/**
 * Unit-test for {@link SimpleSVNDirEntryHandler}.
 */
public class DescendingRevisionSortedSVNDirEntryHandlerTest {

    private static final String SVNTAG_URL = "http://localhost/test/tags";
    private DirectoriesSvnEntryHandler entryHandler;
    private long revision = 1;

    @Before
    public void setup() {
        entryHandler = new DescendingRevisionSortedSVNDirEntryHandler();
    }

    @Test
    public void getDirsShouldAlwaysReturnList() {
        assertNotNull(entryHandler.getDirectoryNames());
        assertTrue(entryHandler.getDirectoryNames().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void handleDirEntryFailsOnNullEntry() throws SVNException {
        entryHandler.handleDirEntry(null);
    }

    @Test
    public void handledDirEntryEndsUpInDirsList() throws SVNException {
        entryHandler.handleDirEntry(generateSvnDirEntry("tag-1", SVNNodeKind.DIR));

        assertNotNull(entryHandler.getDirectoryNames());
        assertEquals(1, entryHandler.getDirectoryNames().size());
    }

    @Test
    public void allHandledDirEntriesEndUpInDirsList() throws SVNException {
        List<String> entryNames = getTagNamesUnsorted();
        for (String name : entryNames) {
            entryHandler.handleDirEntry(generateSvnDirEntry(name, SVNNodeKind.DIR));
        }
        entryHandler.handleDirEntry(generateSvnDirEntry("file", SVNNodeKind.FILE));
        entryHandler.handleDirEntry(generateSvnDirEntry("none", SVNNodeKind.NONE));
        entryHandler.handleDirEntry(generateSvnDirEntry("unknown", SVNNodeKind.UNKNOWN));

        assertNotNull(entryHandler.getDirectoryNames());
        assertEquals(entryNames.size(), entryHandler.getDirectoryNames().size());
        assertTrue(entryHandler.getDirectoryNames().containsAll(entryNames));
    }

    @Test
    public void entryInDirsListMustBeStrippedFromTrailingSlash() throws SVNException {
        entryHandler.handleDirEntry(generateSvnDirEntry("tag-1/", SVNNodeKind.DIR));

        assertNotNull(entryHandler.getDirectoryNames());
        assertEquals(1, entryHandler.getDirectoryNames().size());
        assertEquals("tag-1", entryHandler.getDirectoryNames().get(0));
    }

    @Test
    public void entriesInDirsListAreSortedOnRevisionDescending() throws SVNException {
        List<SVNDirEntry> svnDirEntries = generateSvnDirEntries(getTagNamesWithRevision(), SVNNodeKind.DIR);
        for (SVNDirEntry entry : svnDirEntries) {
            entryHandler.handleDirEntry(entry);
        }
        List<String> actualDirs = entryHandler.getDirectoryNames();
        List<String> expected = getTagNamesSortedByRevisionDescending();
        assertEquals(expected, actualDirs);
        assertEquals(expected.size(), actualDirs.size());
    }

    private Map<Long, String> getTagNamesWithRevision() {
        return new LinkedHashMap<Long, String>() {

            {
                put(8473L, "2.1.0-10");
                put(9364L, "2.1.0-100");
                put(9373L, "2.1.0-101");
                put(9381L, "2.1.0-102");
                put(7033L, "build-99-nl-b2c");
                put(9554L, "2.1.0-119");
                put(5828L, "20110623");
                put(8483L, "2.1.0-12");
                put(9561L, "2.1.0-120");
                put(9646L, "2.1.0-129");
                put(8503L, "2.1.0-13");
                put(9666L, "2.1.0-130");
                put(8389L, "build-207-nl-b2c");
                put(9755L, "2.1.0-139");
                put(8520L, "2.1.0-14");
                put(9763L, "2.1.0-140");
                put(9802L, "2.1.0-149");
                put(9817L, "2.1.0-150");
                put(9891L, "2.1.0-159");
                put(6506L, "build-67-nl-b2c");
                put(8527L, "2.1.0-16");
                put(7095L, "build-104-nl-b2c");
                put(9904L, "2.1.0-160");
                put(8540L, "2.1.0-17");
                put(8499L, "build-221-nl-b2c");
                put(8577L, "2.1.0-19");
                put(8582L, "2.1.0-20");
                put(8602L, "2.1.0-22");
                put(9115L, "2.1.0-79");
                put(8458L, "2.1.0-8");
                put(9125L, "2.1.0-80");
                put(5322L, "20110609");
                put(7075L, "build-102-nl-b2c");
                put(8382L, "build-206-nl-b2c");
                put(8441L, "build-219-nl-b2c");
                put(6441L, "build-64-nl-b2c");
                put(6913L, "build-93-nl-b2c");
            }
        };
    }

    private List<String> getTagNamesSortedByRevisionDescending() {
        TreeMap<Long, String> sortedByKeysMap = new TreeMap<Long, String>(getTagNamesWithRevision());
        return new LinkedList<String>(sortedByKeysMap.descendingMap().values());
    }

    private List<String> getTagNamesUnsorted() {
        return new LinkedList<String>(getTagNamesWithRevision().values());
    }

    private List<SVNDirEntry> generateSvnDirEntries(final Map<Long, String> entryData, final SVNNodeKind kind) throws SVNException {
        List<SVNDirEntry> svnEntries = new LinkedList<SVNDirEntry>();
        for (Long revision : entryData.keySet()) {
            svnEntries.add(generateSvnDirEntry(entryData.get(revision), revision, kind));
        }
        return svnEntries;
    }

    private SVNDirEntry generateSvnDirEntry(final String name, final SVNNodeKind kind) throws SVNException {
        return generateSvnDirEntry(name, revision++, kind);
    }

    private SVNDirEntry generateSvnDirEntry(final String name, final long revision, final SVNNodeKind kind) throws SVNException {
        Date createdDate = new Date(System.currentTimeMillis());
        return createSvnDirEntry(name, revision, kind, createdDate);
    }

    private SVNDirEntry createSvnDirEntry(final String name, final long revision, final SVNNodeKind kind,
            final Date createdDate) throws SVNException {
        SVNURL svnUrl = SVNURL.parseURIDecoded(String.format("%s/%s", SVNTAG_URL, name));
        SVNURL svnRootUrl = SVNURL.parseURIDecoded(SVNTAG_URL);
        String lastAuthor = "tester";
        SVNDirEntry svnDirEntry = new SVNDirEntry(svnUrl, svnRootUrl, name,
                kind, 0, false, revision, createdDate, lastAuthor);
        return svnDirEntry;
    }
}
