/*******************************************************************************
 *
 * Copyright (c) 2009-2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Kohsuke Kawaguchi, Erik Ramfelt, Nikita Levyankov
 *
 *******************************************************************************/
package org.eclipse.hudson.scm;

import hudson.model.AbstractBuild;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import hudson.scm.SCM;
import org.eclipse.hudson.scm.SubversionChangeLogSet.LogEntry;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * {@link hudson.scm.ChangeLogSet} for Subversion.
 *
 * @author Kohsuke Kawaguchi
 * @author Nikita Levyankov
 */
public final class SubversionChangeLogSet extends ChangeLogSet<LogEntry> {
    private final List<LogEntry> logs;

    /**
     * @GuardedBy this
     */
    private Map<String, Long> revisionMap;

    /*package*/ SubversionChangeLogSet(AbstractBuild build, List<LogEntry> logs) {
        super(build);
        this.logs = prepareChangeLogEntries(logs);
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    @Override
    public String getKind() {
        return "svn";
    }

    public synchronized Map<String, Long> getRevisionMap() throws IOException {
        if (revisionMap == null) {
            revisionMap = SubversionSCM.parseRevisionFile(build);
        }
        return revisionMap;
    }

    @Exported
    public List<RevisionInfo> getRevisions() throws IOException {
        List<RevisionInfo> r = new ArrayList<RevisionInfo>();
        for (Map.Entry<String, Long> e : getRevisionMap().entrySet()) {
            r.add(new RevisionInfo(e.getKey(), e.getValue()));
        }
        return r;
    }

    protected List<LogEntry> prepareChangeLogEntries(List<LogEntry> items) {
        items = removeDuplicatedEntries(items);
        // we want recent changes first
        Collections.sort(items, new LogEntryComparator());
        for (LogEntry log : items) {
            log.setParent(this);
        }
        return Collections.unmodifiableList(items);
    }

    /**
     * Removes duplicate entries, ie those coming form svn:externals.
     *
     * @param items list of items
     * @return filtered list without duplicated entries
     */
    protected static List<LogEntry> removeDuplicatedEntries(List<LogEntry> items) {
        Set<LogEntry> entries = new HashSet<LogEntry>(items);
        return new ArrayList<LogEntry>(entries);
    }

    @ExportedBean(defaultVisibility = 999)
    public static final class RevisionInfo {
        @Exported
        public final String module;
        @Exported
        public final long revision;

        public RevisionInfo(String module, long revision) {
            this.module = module;
            this.revision = revision;
        }
    }

    /**
     * One commit.
     * <p/>
     * Setter methods are public only so that the objects can be constructed from Digester.
     * So please consider this object read-only.
     */
    public static class LogEntry extends ChangeLogSet.Entry {
        private int revision;
        private User author;
        private String date;
        private String msg;
        private List<Path> paths = new ArrayList<Path>();

        /**
         * Gets the {@link SubversionChangeLogSet} to which this change set belongs.
         */
        public SubversionChangeLogSet getParent() {
            return (SubversionChangeLogSet) super.getParent();
        }

        // because of the classloader difference, we need to extend this method to make it accessible
        // to the rest of SubversionSCM
        @Override
        @SuppressWarnings({"PMD"})
        protected void setParent(ChangeLogSet changeLogSet) {
            super.setParent(changeLogSet);
        }

        /**
         * Gets the revision of the commit.
         * <p/>
         * <p/>
         * If the commit made the repository revision 1532, this
         * method returns 1532.
         */
        @Exported
        public int getRevision() {
            return revision;
        }

        public void setRevision(int revision) {
            this.revision = revision;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getCurrentRevision() {
            return String.valueOf(getRevision());
        }

        /**
         * {@inheritDoc}
         */
        public User getAuthor() {
            if (author == null) {
                return User.getUnknown();
            }
            return author;
        }

        /**
         * {@inheritDoc}
         */
        public Collection<String> getAffectedPaths() {
            return new AbstractList<String>() {
                public String get(int index) {
                    return preparePath(paths.get(index).value);
                }

                public int size() {
                    return paths.size();
                }
            };
        }

        private String preparePath(String path) {
            SCM scm = getParent().build.getProject().getScm();
            if (!(scm instanceof SubversionSCM)) {
                return path;
            }
            SubversionSCM.ModuleLocation[] locations = ((SubversionSCM) scm).getLocations();
            for (int i = 0; i < locations.length; i++) {
                String commonPart = findCommonPart(locations[i].remote, path);
                if (commonPart != null) {
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    String newPath = path.substring(commonPart.length());
                    if (newPath.startsWith("/")) {
                        newPath = newPath.substring(1);
                    }
                    return newPath;
                }
            }
            return path;
        }

        private String findCommonPart(String folder, String filePath) {
            if (folder == null || filePath == null) {
                return null;
            }
            if (filePath.startsWith("/")) {
                filePath = filePath.substring(1);
            }
            for (int i = 0; i < folder.length(); i++) {
                String part = folder.substring(i);
                if (filePath.startsWith(part)) {
                    return part;
                }
            }
            return null;
        }

        public void setUser(String author) {
            this.author = User.get(author);
        }

        @Exported
        public String getUser() {// digester wants read/write property, even though it never reads. Duh.
            return getAuthor().getDisplayName();
        }

        @Exported
        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        @Exported
        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public void addPath(Path p) {
            p.entry = this;
            paths.add(p);
        }

        /**
         * Gets the files that are changed in this commit.
         *
         * @return can be empty but never null.
         */
        @Exported
        public List<Path> getPaths() {
            return paths;
        }

        @Override
        public Collection<Path> getAffectedFiles() {
            return paths;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            LogEntry that = (LogEntry) o;

            if (revision != that.revision) {
                return false;
            }
            if (author != null ? !author.equals(that.author) : that.author != null) {
                return false;
            }
            if (date != null ? !date.equals(that.date) : that.date != null) {
                return false;
            }
            if (msg != null ? !msg.equals(that.msg) : that.msg != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = revision;
            result = 31 * result + (author != null ? author.hashCode() : 0);
            result = 31 * result + (date != null ? date.hashCode() : 0);
            result = 31 * result + (msg != null ? msg.hashCode() : 0);
            return result;
        }
    }

    /**
     * A file in a commit.
     * <p/>
     * Setter methods are public only so that the objects can be constructed from Digester.
     * So please consider this object read-only.
     */
    @ExportedBean(defaultVisibility = 999)
    public static class Path implements AffectedFile {
        private LogEntry entry;
        private char action;
        private String value;

        /**
         * Gets the {@link LogEntry} of which this path is a member.
         */
        public LogEntry getLogEntry() {
            return entry;
        }

        /**
         * Sets the {@link LogEntry} of which this path is a member.
         */
        public void setLogEntry(LogEntry entry) {
            this.entry = entry;
        }

        public void setAction(String action) {
            this.action = action.charAt(0);
        }

        /**
         * Path in the repository. Such as <tt>/test/trunk/foo.c</tt>
         */
        @Exported(name = "file")
        public String getValue() {
            return value;
        }

        /**
         * Inherited from AffectedFile
         */
        public String getPath() {
            return getValue();
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Exported
        public EditType getEditType() {
            if (action == 'A') {
                return EditType.ADD;
            }
            if (action == 'D') {
                return EditType.DELETE;
            }
            return EditType.EDIT;
        }
    }

    private static final class LogEntryComparator implements Comparator<LogEntry> {
        public int compare(LogEntry a, LogEntry b) {
            return b.getRevision() - a.getRevision();
        }
    }
}
