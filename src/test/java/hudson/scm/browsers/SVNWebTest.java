package hudson.scm.browsers;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

import hudson.scm.SubversionChangeLogSet.Path;
import hudson.scm.SubversionChangeLogSet.LogEntry;

public class SVNWebTest {

    private SVNWeb repoBrowser = createRepoBrowser();

    @Test
    public void noDiffLinkShouldBeGeneratedForAddChanges() throws Exception {
        assertThat(repoBrowser.getDiffLink(path("Add", 2)), is(nullValue()));
    }

    @Test
    public void noDiffLinkShouldBeGeneratedForDeleteChanges() throws Exception {
        assertThat(repoBrowser.getDiffLink(path("Delete", 2)), is(nullValue()));
    }

    @Test
    public void diffLinkShouldBeGeneratedForEditChanges() throws Exception {
        // given
        Path path = path("Edit", 2);
        // when
        String diffLink = repoBrowser.getDiffLink(path).toString();
        // then
        assertThat(diffLink, is("http://localhost/svnweb/repo/diff/trunk/foo.c?rev1=1;rev2=2"));
    }

    @Test
    public void fileLinkShouldBeGeneratedCorrect() throws Exception {
        // given
        Path path = path("Edit", 2);
        // when
        String fileLink = repoBrowser.getFileLink(path).toString();
        // then
        assertThat(fileLink, is("http://localhost/svnweb/repo/view/trunk/foo.c?rev=2"));
    }

    @Test
    public void changeSetLinkShouldBeGeneratedCorrect() throws Exception {
        // given
        LogEntry entry = path("Edit", 2).getLogEntry();
        // when
        String changeSetLink = repoBrowser.getChangeSetLink(entry).toString();
        // then
        assertThat(changeSetLink, is("http://localhost/svnweb/repo/revision/?rev=2"));
    }

    private Path path(String action, int revision) {
        final LogEntry logEntry = new LogEntry();
        logEntry.setRevision(revision);

        final Path path = new Path();
        path.setAction(action);
        path.setLogEntry(logEntry);
        path.setValue("/trunk/foo.c");

        return path;
    }

    private SVNWeb createRepoBrowser() {
        try {
          return new SVNWeb(new URL("http://localhost/svnweb/repo"));
        } catch (MalformedURLException ex) {
          throw new RuntimeException("Invalid URL", ex);
        }
    }

}
