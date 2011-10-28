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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
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
public class SimpleSVNDirEntryHandlerTest {

    private static final String SVNTAG_URL = "http://localhost/test/tags";
    private DirectoriesSvnEntryHandler entryHandler;

    @Before
    public void setup() {
        entryHandler = new SimpleSVNDirEntryHandler();
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
        List<String> entryNames = getEntryNames();
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

    private List<String> getEntryNames() {
        return Arrays.asList(
                "2.1.0-10",
                "2.1.0-100",
                "2.1.0-101",
                "2.1.0-102",
                "2.1.0-119",
                "2.1.0-12",
                "2.1.0-120",
                "2.1.0-129",
                "2.1.0-13",
                "2.1.0-130",
                "2.1.0-139",
                "2.1.0-14",
                "2.1.0-140",
                "2.1.0-159",
                "2.1.0-16",
                "2.1.0-160",
                "2.1.0-17",
                "2.1.0-19",
                "2.1.0-20",
                "2.1.0-22",
                "2.1.0-79",
                "2.1.0-8",
                "2.1.0-80",
                "20110609",
                "20110623",
                "build-102-nl-b2c",
                "build-104-nl-b2c",
                "build-197-nl-b2c",
                "build-206-nl-b2c",
                "build-207-nl-b2c",
                "build-219-nl-b2c",
                "build-221-nl-b2c",
                "build-64-nl-b2c",
                "build-67-nl-b2c",
                "build-93-nl-b2c",
                "build-99-nl-b2c");
    }

    private List<SVNDirEntry> generateSvnDirEntries(final List<String> names, final SVNNodeKind kind) throws SVNException {
        List<SVNDirEntry> entries = new ArrayList<SVNDirEntry>(names.size());
        for (String name : names) {
            entries.add(generateSvnDirEntry(name, kind));
        }
        return entries;
    }

    private SVNDirEntry generateSvnDirEntry(final String name, final SVNNodeKind kind) throws SVNException {
        long revision = 1;
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
