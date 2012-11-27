/*
 * The MIT License
 * 
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Fulvio Cavarretta,
 * Jean-Baptiste Quenot, Luca Domenico Milanesio, Renaud Bruyeron, Stephen Connolly,
 * Tom Huybrechts, Yahoo! Inc., Manufacture Francaise des Pneumatiques Michelin,
 * Romain Seguy, Anton Kozak, Nikita Levyankov
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
package hudson.scm;

import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Hudson.MasterComputer;
import hudson.model.Node;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.remoting.Channel;
import hudson.remoting.DelegatingCallable;
import hudson.remoting.VirtualChannel;
import hudson.scm.PollingResult.Change;
import hudson.scm.UserProvidedCredential.AuthenticationManagerImpl;
import hudson.scm.auth.ISVNAuthenticationManager;
import hudson.scm.auth.ISVNAuthenticationOutcomeListener;
import hudson.scm.subversion.Messages;
import hudson.scm.subversion.WorkspaceUpdaterDescriptor;
import hudson.scm.subversion.CheckoutUpdater;
import hudson.scm.subversion.UpdateUpdater;
import hudson.scm.subversion.UpdateWithRevertUpdater;
import hudson.scm.subversion.WorkspaceUpdater;
import hudson.scm.subversion.WorkspaceUpdater.UpdateTask;
import hudson.util.EditDistance;
import hudson.util.FormValidation;
import hudson.util.MultipartFormDataParser;
import hudson.util.TimeUnit2;
import hudson.util.XStream2;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;
import javax.xml.transform.stream.StreamResult;

import net.sf.json.JSONObject;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.dav.http.DefaultHTTPConnectionFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNExternal;
import org.tmatesoft.svn.core.internal.wc.admin.SVNAdminAreaFactory;
import org.tmatesoft.svn.core.io.SVNCapability;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNInfo;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import com.thoughtworks.xstream.XStream;
import com.trilead.ssh2.DebugLogger;
import com.trilead.ssh2.SCPClient;

/**
 * Subversion SCM.
 * <p/>
 * <h2>Plugin Developer Notes</h2>
 * <p/>
 * Plugins that interact with Subversion can use {@link DescriptorImpl#createAuthenticationProvider(AbstractProject)}
 * so that it can use the credentials (username, password, etc.) that the user entered for Hudson.
 * See the javadoc of this method for the precautions you need to take if you run Subversion operations
 * remotely on slaves.
 * <p/>
 * <h2>Implementation Notes</h2>
 * <p/>
 * Because this instance refers to some other classes that are not necessarily
 * Java serializable (like {@link #browser}), remotable {@link FileCallable}s all
 * need to be declared as static inner classes.
 *
 * @author Kohsuke Kawaguchi
 */
public class SubversionSCM extends SCM implements Serializable {
    protected static final String UNDEFINED_REVISION_VALUE = "UNDEFINED";
    /**
     * the locations field is used to store all configured SVN locations (with
     * their local and remote part). Direct access to this field should be
     * avoided and the getLocations() method should be used instead. This is
     * needed to make importing of old hudson-configurations possible as
     * getLocations() will check if the modules field has been set and import
     * the data.
     *
     * @since 1.91
     */
    private ModuleLocation[] locations = new ModuleLocation[0];

    private final SubversionRepositoryBrowser browser;
    private String excludedRegions;
    private String includedRegions;
    private String excludedUsers;
    /**
     * Revision property names that are ignored for the sake of polling. Whitespace separated, possibly null.
     */
    private String excludedRevprop;
    private String excludedCommitMessages;

    private WorkspaceUpdater workspaceUpdater;

    // No longer in use but left for serialization compatibility.
    @Deprecated
    private String modules;

    private static File subversionConfigDir;

    /**
     * Supported revision policies.
     */
    public enum RevisionPolicy {
        QUEUE_TIME("Queue time"),
        BUILD_TIME("Build time"),
        HEAD("Head revision");

        private String policyName;

        /**
         * Creates the policy.
         *
         * @param policyName policy name.
         */
        RevisionPolicy(String policyName) {
            this.policyName = policyName;
        }

        /**
         * Returns the name of policy.
         *
         * @return the name of policy.
         */
        public String getPolicyName() {
            return this.policyName;
        }
    }

    // No longer used but left for serialization compatibility
    @Deprecated
    private Boolean useUpdate;
    @Deprecated
    private Boolean doRevert;


    /**
     * @deprecated as of 1.286
     */
    public SubversionSCM(String[] remoteLocations, String[] localLocations,
                         boolean useUpdate, SubversionRepositoryBrowser browser) {
        this(remoteLocations, localLocations, useUpdate, browser, null, null, null);
    }

    /**
     * @deprecated as of 1.311
     */
    public SubversionSCM(String[] remoteLocations, String[] localLocations,
                         boolean useUpdate, SubversionRepositoryBrowser browser, String excludedRegions) {
        this(ModuleLocation.parse(remoteLocations, localLocations, null, null), useUpdate, false, browser,
            excludedRegions, null,
            null, null);
    }

    /**
     * @deprecated as of 1.315
     */
    public SubversionSCM(String[] remoteLocations, String[] localLocations,
                         boolean useUpdate, SubversionRepositoryBrowser browser, String excludedRegions,
                         String excludedUsers, String excludedRevprop) {
        this(ModuleLocation.parse(remoteLocations, localLocations, null, null), useUpdate, false, browser,
            excludedRegions,
            excludedUsers, excludedRevprop, null);
    }

    /**
     * @deprecated as of 1.315
     */
    public SubversionSCM(List<ModuleLocation> locations,
                         boolean useUpdate, SubversionRepositoryBrowser browser, String excludedRegions) {
        this(locations, useUpdate, false, browser, excludedRegions, null, null, null);
    }

    /**
     * @deprecated as of 1.324
     */
    public SubversionSCM(List<ModuleLocation> locations,
                         boolean useUpdate, SubversionRepositoryBrowser browser, String excludedRegions,
                         String excludedUsers, String excludedRevprop) {
        this(locations, useUpdate, false, browser, excludedRegions, excludedUsers, excludedRevprop, null);
    }

    /**
     * @deprecated as of 1.328
     */
    public SubversionSCM(List<ModuleLocation> locations,
                         boolean useUpdate, SubversionRepositoryBrowser browser, String excludedRegions,
                         String excludedUsers, String excludedRevprop, String excludedCommitMessages) {
        this(locations, useUpdate, false, browser, excludedRegions, excludedUsers, excludedRevprop,
            excludedCommitMessages);
    }

    /**
     * @deprecated as of 1.xxx
     */
    public SubversionSCM(List<ModuleLocation> locations,
                         boolean useUpdate, boolean doRevert, SubversionRepositoryBrowser browser,
                         String excludedRegions, String excludedUsers, String excludedRevprop,
                         String excludedCommitMessages) {
        this(locations, useUpdate, doRevert, browser, excludedRegions, excludedUsers, excludedRevprop,
            excludedCommitMessages, null);
    }

    /**
     * @deprecated as of 1.23
     */
    public SubversionSCM(List<ModuleLocation> locations,
                         boolean useUpdate, boolean doRevert, SubversionRepositoryBrowser browser,
                         String excludedRegions, String excludedUsers, String excludedRevprop,
                         String excludedCommitMessages,
                         String includedRegions) {
        this(locations,
            useUpdate ? (doRevert ? new UpdateWithRevertUpdater() : new UpdateUpdater()) : new CheckoutUpdater(),
            browser, excludedRegions, excludedUsers, excludedRevprop, excludedCommitMessages, includedRegions);
    }

    @DataBoundConstructor
    public SubversionSCM(List<ModuleLocation> locations, WorkspaceUpdater workspaceUpdater,
                         SubversionRepositoryBrowser browser, String excludedRegions, String excludedUsers,
                         String excludedRevprop, String excludedCommitMessages,
                         String includedRegions) {
        for (Iterator<ModuleLocation> itr = locations.iterator(); itr.hasNext(); ) {
            ModuleLocation ml = itr.next();
            if (ml.remote == null) {
                itr.remove();
            }
        }
        this.locations = locations.toArray(new ModuleLocation[locations.size()]);

        this.workspaceUpdater = workspaceUpdater;
        this.browser = browser;
        this.excludedRegions = excludedRegions;
        this.excludedUsers = excludedUsers;
        this.excludedRevprop = excludedRevprop;
        this.excludedCommitMessages = excludedCommitMessages;
        this.includedRegions = includedRegions;
    }

    /**
     * Convenience constructor, especially during testing.
     */
    public SubversionSCM(String svnUrl) {
        this(svnUrl, ".");
    }

    /**
     * Convenience constructor, especially during testing.
     */
    public SubversionSCM(String svnUrl, String local) {
        this(new String[]{svnUrl}, new String[]{local}, true, null, null, null, null);
    }

    /**
     * @deprecated as of 1.91. Use {@link #getLocations()} instead.
     */
    public String getModules() {
        return null;
    }

    /**
     * list of all configured svn locations
     *
     * @since 1.91
     */
    @Exported
    public ModuleLocation[] getLocations() {
        return getLocations(null);
    }

    @Exported
    public WorkspaceUpdater getWorkspaceUpdater() {
        if (workspaceUpdater != null) {
            return workspaceUpdater;
        }

        // data must have been read from old configuration.
        if (useUpdate != null && !useUpdate.booleanValue()) {
            return new CheckoutUpdater();
        }
        if (doRevert != null && doRevert.booleanValue()) {
            return new UpdateWithRevertUpdater();
        }
        return new UpdateUpdater();
    }

    public void setWorkspaceUpdater(WorkspaceUpdater workspaceUpdater) {
        this.workspaceUpdater = workspaceUpdater;
    }

    /**
     * list of all configured svn locations, expanded according to
     * build parameters values;
     *
     * @param build If non-null, variable expansions are performed against the build parameters.
     * @since 1.252
     */
    public ModuleLocation[] getLocations(AbstractBuild<?, ?> build) {
        // check if we've got a old location
        if (modules != null) {
            // import the old configuration
            List<ModuleLocation> oldLocations = new ArrayList<ModuleLocation>();
            StringTokenizer tokens = new StringTokenizer(modules);
            while (tokens.hasMoreTokens()) {
                // the remote (repository location)
                // the normalized name is always without the trailing '/'
                String remoteLoc = Util.removeTrailingSlash(tokens.nextToken());

                oldLocations.add(new ModuleLocation(remoteLoc, null));
            }

            locations = oldLocations.toArray(new ModuleLocation[oldLocations.size()]);
            modules = null;
        }

        if (build == null) {
            return locations;
        }

        ModuleLocation[] outLocations = new ModuleLocation[locations.length];
        for (int i = 0; i < outLocations.length; i++) {
            outLocations[i] = locations[i].getExpandedLocation(build);
        }

        return outLocations;
    }

    @Override
    @Exported
    public SubversionRepositoryBrowser getBrowser() {
        return browser;
    }

    @Exported
    public String getExcludedRegions() {
        return excludedRegions;
    }

    public String[] getExcludedRegionsNormalized() {
        return (excludedRegions == null || excludedRegions.trim().equals(""))
            ? null : excludedRegions.split("[\\r\\n]+");
    }

    private Pattern[] getExcludedRegionsPatterns() {
        String[] excluded = getExcludedRegionsNormalized();
        if (excluded != null) {
            Pattern[] patterns = new Pattern[excluded.length];

            int i = 0;
            for (String excludedRegion : excluded) {
                patterns[i++] = Pattern.compile(excludedRegion);
            }

            return patterns;
        }

        return new Pattern[0];
    }

    @Exported
    public String getIncludedRegions() {
        return includedRegions;
    }

    public String[] getIncludedRegionsNormalized() {
        return (includedRegions == null || includedRegions.trim().equals(""))
            ? null : includedRegions.split("[\\r\\n]+");
    }

    private Pattern[] getIncludedRegionsPatterns() {
        String[] included = getIncludedRegionsNormalized();
        if (included != null) {
            Pattern[] patterns = new Pattern[included.length];

            int i = 0;
            for (String includedRegion : included) {
                patterns[i++] = Pattern.compile(includedRegion);
            }

            return patterns;
        }

        return new Pattern[0];
    }

    @Exported
    public String getExcludedUsers() {
        return excludedUsers;
    }

    public Set<String> getExcludedUsersNormalized() {
        String s = Util.fixEmptyAndTrim(excludedUsers);
        if (s == null) {
            return Collections.emptySet();
        }

        Set<String> users = new HashSet<String>();
        for (String user : s.split("[\\r\\n]+")) {
            users.add(user.trim());
        }
        return users;
    }

    @Exported
    public String getExcludedRevprop() {
        return excludedRevprop;
    }

    private String getExcludedRevpropNormalized() {
        String s = Util.fixEmptyAndTrim(getExcludedRevprop());
        if (s != null) {
            return s;
        }
        return getDescriptor().getGlobalExcludedRevprop();
    }

    @Exported
    public String getExcludedCommitMessages() {
        return excludedCommitMessages;
    }

    public String[] getExcludedCommitMessagesNormalized() {
        String s = Util.fixEmptyAndTrim(excludedCommitMessages);
        return s == null ? new String[0] : s.split("[\\r\\n]+");
    }

    private Pattern[] getExcludedCommitMessagesPatterns() {
        String[] excluded = getExcludedCommitMessagesNormalized();
        Pattern[] patterns = new Pattern[excluded.length];

        int i = 0;
        for (String excludedCommitMessage : excluded) {
            patterns[i++] = Pattern.compile(excludedCommitMessage);
        }

        return patterns;
    }

    /**
     * Sets the <tt>SVN_REVISION</tt> environment variable during the build.
     */
    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, Map<String, String> env) {
        super.buildEnvVars(build, env);

        ModuleLocation[] svnLocations = getLocations(build);

        try {
            Map<String, Long> revisions = parseRevisionFile(build);
            if (svnLocations.length == 1) {
                Long rev = revisions.get(svnLocations[0].remote);
                if (rev != null) {
                    env.put("SVN_REVISION", rev.toString());
                    env.put("SVN_URL", svnLocations[0].getURL());
                }
            } else if (svnLocations.length > 1) {
                for (int i = 0; i < svnLocations.length; i++) {
                    Long rev = revisions.get(svnLocations[i].remote);
                    if (rev != null) {
                        env.put("SVN_REVISION_" + (i + 1), rev.toString());
                        env.put("SVN_URL_" + (i + 1), svnLocations[i].getURL());
                    }
                }
            }

        } catch (IOException e) {
            // ignore this error
            LOGGER.log(Level.FINEST, "Exception while building envVars. Error will be ignored.", e);
        }
    }

    /**
     * Called after checkout/update has finished to compute the changelog.
     */
    private boolean calcChangeLog(AbstractBuild<?, ?> build, File changelogFile, BuildListener listener,
                                  List<External> externals) throws IOException, InterruptedException {
        if (build.getPreviousBuild() == null) {
            // nothing to compare against
            return createEmptyChangeLog(changelogFile, listener, "log");
        }

        // some users reported that the file gets created with size 0. I suspect
        // maybe some XSLT engine doesn't close the stream properly.
        // so let's do it by ourselves to be really sure that the stream gets closed.
        OutputStream os = new BufferedOutputStream(new FileOutputStream(changelogFile));
        boolean created;
        try {
            created = new SubversionChangeLogBuilder(build, listener, this).run(externals, new StreamResult(os));
        } finally {
            os.close();
        }
        if (!created) {
            createEmptyChangeLog(changelogFile, listener, "log");
        }

        return true;
    }


    /**
     * Return subversion configuration directory.
     *
     * @return directory.
     */
    public static File getSubversionConfigDir() {
        if (null == subversionConfigDir) {
            subversionConfigDir = new File(
                Hudson.getInstance().getRootDir().getPath() + File.separator + ".subversion");
            try {
                FileUtils.forceMkdir(subversionConfigDir);
            } catch (IOException e) {
                subversionConfigDir = SVNWCUtil.getDefaultConfigurationDirectory();
            }
        }
        return subversionConfigDir;
    }

    /*package*/
    static Map<String, Long> parseRevisionFile(AbstractBuild<?, ?> build) throws IOException {
        return parseRevisionFile(build, false);
    }

    /**
     * Reads the revision file of the specified build (or the closest, if the flag is so specified.)
     *
     * @param findClosest If true, this method will go back the build history until it finds a revision file.
     * A build may not have a revision file for any number of reasons (such as failure, interruption, etc.)
     * @return map from {@link SvnInfo#url Subversion URL} to its revision.
     */
    /*package*/
    static Map<String, Long> parseRevisionFile(AbstractBuild<?, ?> build, boolean findClosest) throws IOException {
        Map<String, Long> revisions = new HashMap<String, Long>(); // module -> revision

        if (findClosest) {
            for (AbstractBuild<?, ?> b = build; b != null; b = b.getPreviousBuild()) {
                if (getRevisionFile(b).exists()) {
                    build = b;
                    break;
                }
            }
        }

        {// read the revision file of the build
            File file = getRevisionFile(build);
            if (!file.exists())
            // nothing to compare against
            {
                return revisions;
            }

            BufferedReader br = new BufferedReader(new FileReader(file));
            try {
                String line;
                while ((line = br.readLine()) != null) {
                    int index = line.lastIndexOf('/');
                    if (index < 0) {
                        continue;   // invalid line?
                    }
                    try {
                        revisions.put(line.substring(0, index), Long.valueOf(line.substring(index + 1)));
                    } catch (NumberFormatException e) {
                        // perhaps a corrupted line. ignore
                        LOGGER.log(Level.FINEST, "Error parsing line", e);
                    }
                }
            } finally {
                br.close();
            }
        }

        return revisions;
    }

    /**
     * Parses the file that stores the locations in the workspace where modules loaded by svn:external
     * is placed.
     * <p/>
     * <p/>
     * Note that the format of the file has changed in 1.180 from simple text file to XML.
     *
     * @return immutable list. Can be empty but never null.
     */
    /*package*/
    static List<External> parseExternalsFile(AbstractProject project) {
        File file = getExternalsFile(project);
        if (file.exists()) {
            try {
                return (List<External>) new XmlFile(External.XSTREAM, file).read();
            } catch (IOException e) {
                // in < 1.180 this file was a text file, so it may fail to parse as XML,
                // in which case let's just fall back
                LOGGER.log(Level.FINEST,
                    "Couldn't parse externals file. It is text file with version < 1.180. Expected XML", e);
            }
        }

        return Collections.emptyList();
    }

    /**
     * Polling can happen on the master and does not require a workspace.
     */
    @Override
    public boolean requiresWorkspaceForPolling() {
        return false;
    }

    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, final BuildListener listener,
                            File changelogFile) throws IOException, InterruptedException {
        List<External> externals = checkout(build, workspace, listener);

        if (externals == null) {
            return false;
        }

        // write out the revision file
        PrintWriter w = new PrintWriter(new FileOutputStream(getRevisionFile(build)));
        try {
            Map<String, SvnInfo> revMap = workspace.act(new BuildRevisionMapTask(build, this, listener, externals));
            for (Entry<String, SvnInfo> e : revMap.entrySet()) {
                w.println(e.getKey() + '/' + e.getValue().revision);
            }
            build.addAction(new SubversionTagAction(build, revMap.values()));
        } finally {
            w.close();
        }

        // write out the externals info
        new XmlFile(External.XSTREAM, getExternalsFile(build.getProject())).write(externals);

        return calcChangeLog(build, changelogFile, listener, externals);
    }

    /**
     * Performs the checkout or update, depending on the configuration and workspace state.
     * <p/>
     * <p/>
     * Use canonical path to avoid SVNKit/symlink problem as described in
     * https://wiki.svnkit.com/SVNKit_FAQ
     *
     * @return null
     *         if the operation failed. Otherwise the set of local workspace paths
     *         (relative to the workspace root) that has loaded due to svn:external.
     */
    private List<External> checkout(AbstractBuild build, FilePath workspace, TaskListener listener)
        throws IOException, InterruptedException {
        if (repositoryLocationsNoLongerExist(build, listener)) {
            Run lsb = build.getProject().getLastSuccessfulBuild();
            if (lsb != null && build.getNumber() - lsb.getNumber() > 10
                && build.getTimestamp().getTimeInMillis() - lsb.getTimestamp().getTimeInMillis() > TimeUnit2.DAYS
                .toMillis(1)) {
                // Disable this project if the location doesn't exist any more, see issue #763
                // but only do so if there was at least some successful build,
                // to make sure that initial configuration error won't disable the build. see issue #1567
                // finally, only disable a build if the failure persists for some time.
                // see http://www.nabble.com/Should-Hudson-have-an-option-for-a-content-fingerprint--td24022683.html

                listener.getLogger()
                    .println("One or more repository locations do not exist anymore for " + build.getProject().getName()
                        + ", project will be disabled.");
                build.getProject().makeDisabled(true);
                return null;
            }
        }

        //TODO get build time from listener.
        return workspace.act(new CheckOutTask(build, this, build.getTimestamp().getTime(),
            new GregorianCalendar().getTime(), listener));
    }


    /**
     * Either run "svn co" or "svn up" equivalent.
     */
    private static class CheckOutTask extends UpdateTask implements FileCallable<List<External>> {
        private final UpdateTask task;

        public CheckOutTask(AbstractBuild<?, ?> build, SubversionSCM scm, Date queueTime, Date buildTime,
                            TaskListener listener) {
            this.authProvider = scm.getDescriptor().createAuthenticationProvider(build.getParent());
            this.queueTime = queueTime;
            this.buildTime = buildTime;
            this.listener = listener;
            this.locations = scm.getLocations(build);
            this.revisionParameterAction = build.getAction(RevisionParameterAction.class);
            this.task = scm.getWorkspaceUpdater().createTask();
            this.revisionPolicy = (scm.getDescriptor() != null ? scm.getDescriptor().getRevisionPolicy() : null);
        }

        public List<External> invoke(File ws, VirtualChannel channel) throws IOException, InterruptedException {
            manager = createSvnClientManager(authProvider);
            this.ws = ws;
            try {
                List<External> externals = perform();

                checkClockOutOfSync();

                return externals;

            } finally {
                manager.dispose();
            }
        }

        /**
         * This round-about way of executing the task ensures that the error-prone {@link #delegateTo(UpdateTask)} method
         * correctly copies everything.
         */
        @Override
        public List<External> perform() throws IOException, InterruptedException {
            return delegateTo(task);
        }

        private void checkClockOutOfSync() {
            try {
                for (ModuleLocation l : locations) {
                    SVNDirEntry dir = manager.createRepository(l.getSVNURL(), true).info("/", -1);
                    // see http://www.nabble.com/NullPointerException-in-SVN-Checkout-Update-td21609781.html that reported this being null.
                    if (dir != null && dir.getDate() != null && dir.getDate().after(new Date())) {
                        listener.getLogger().println(Messages.SubversionSCM_ClockOutOfSync());
                    }
                }
            } catch (SVNAuthenticationException e) {
                // if we don't have access to '/', ignore. error
                LOGGER.log(Level.FINE, "Failed to estimate the remote time stamp", e);
            } catch (SVNException e) {
                LOGGER.log(Level.INFO, "Failed to estimate the remote time stamp", e);
            }
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Creates {@link SVNClientManager}.
     * <p/>
     * <p/>
     * This method must be executed on the slave where svn operations are performed.
     *
     * @param authProvider The value obtained from {@link DescriptorImpl#createAuthenticationProvider(AbstractProject)}.
     * If the operation runs on slaves,
     * (and properly remoted, if the svn operations run on slaves.)
     */
    public static SVNClientManager createSvnClientManager(ISVNAuthenticationProvider authProvider) {
        SubversionWorkspaceSelector.syncWorkspaceFormatFromMaster();
        ISVNAuthenticationManager sam = new DefaultSVNAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager());
        sam.setAuthenticationProvider(authProvider);
        return SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), sam.getAuthenticationManager());
    }

    /**
     * Creates {@link SVNClientManager} for code running on the master.
     * <p/>
     * CAUTION: this code only works when invoked on master. On slaves, use
     * {@link #createSvnClientManager(ISVNAuthenticationProvider)} and get {@link ISVNAuthenticationProvider}
     * from the master via remoting.
     */
    public static SVNClientManager createSvnClientManager(AbstractProject context) {
        return createSvnClientManager(
            Hudson.getInstance().getDescriptorByType(DescriptorImpl.class).createAuthenticationProvider(context));
    }

    public static final class SvnInfo implements Serializable, Comparable<SvnInfo> {
        /**
         * Decoded repository URL.
         */
        public final String url;
        public final long revision;

        public SvnInfo(String url, long revision) {
            this.url = url;
            this.revision = revision;
        }

        public SvnInfo(SVNInfo info) {
            this(info.getURL().toDecodedString(), info.getCommittedRevision().getNumber());
        }

        public SVNURL getSVNURL() throws SVNException {
            return SVNURL.parseURIDecoded(url);
        }

        public int compareTo(SvnInfo that) {
            int r = this.url.compareTo(that.url);
            if (r != 0) {
                return r;
            }

            if (this.revision < that.revision) {
                return -1;
            }
            if (this.revision > that.revision) {
                return +1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SvnInfo)) {
                return false;
            }

            SvnInfo svnInfo = (SvnInfo) o;
            return revision == svnInfo.revision && url.equals(svnInfo.url);

        }

        @Override
        public int hashCode() {
            int result;
            result = url.hashCode();
            result = 31 * result + (int) (revision ^ (revision >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return String.format("%s (rev.%s)", url, Long.valueOf(revision));
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Information about svn:external
     */
    public static final class External implements Serializable {
        /**
         * Relative path within the workspace where this <tt>svn:exteranls</tt> exist.
         */
        public final String path;

        /**
         * External SVN URL to be fetched.
         */
        public final String url;

        /**
         * If the svn:external link is with the -r option, its number.
         * Otherwise -1 to indicate that the head revision of the external repository should be fetched.
         */
        public final long revision;

        /**
         * @param modulePath The root of the current module that svn was checking out when it hits 'ext'.
         * Since we call svnkit multiple times in general case to check out from multiple locations,
         * we use this to make the path relative to the entire workspace, not just the particular module.
         */
        public External(String modulePath, SVNExternal ext) {
            this.path = modulePath + '/' + ext.getPath();
            this.url = ext.getResolvedURL().toDecodedString();
            this.revision = ext.getRevision().getNumber();
        }

        /**
         * Returns true if this reference is to a fixed revision.
         */
        public boolean isRevisionFixed() {
            return revision != -1;
        }

        private static final long serialVersionUID = 1L;

        private static final XStream XSTREAM = new XStream2();

        static {
            XSTREAM.alias("external", External.class);
        }
    }


    /**
     * Gets the SVN metadata for the remote repository.
     *
     * @param remoteUrl The target to run "svn info".
     */
    private static SVNInfo parseSvnInfo(SVNURL remoteUrl, ISVNAuthenticationProvider authProvider) throws SVNException {
        final SVNClientManager manager = createSvnClientManager(authProvider);
        try {
            final SVNWCClient svnWc = manager.getWCClient();
            return svnWc.doInfo(remoteUrl, SVNRevision.HEAD, SVNRevision.HEAD);
        } finally {
            manager.dispose();
        }
    }

    /**
     * Checks .svn files in the workspace and finds out revisions of the modules
     * that the workspace has.
     *
     * @return null if the parsing somehow fails. Otherwise a map from the repository URL to revisions.
     */
    private static class BuildRevisionMapTask implements FileCallable<Map<String, SvnInfo>> {
        private final ISVNAuthenticationProvider authProvider;
        private final TaskListener listener;
        private final List<External> externals;
        private final ModuleLocation[] locations;

        public BuildRevisionMapTask(AbstractBuild<?, ?> build, SubversionSCM parent, TaskListener listener,
                                    List<External> externals) {
            this.authProvider = parent.getDescriptor().createAuthenticationProvider(build.getParent());
            this.listener = listener;
            this.externals = externals;
            this.locations = parent.getLocations(build);
        }

        public Map<String, SvnInfo> invoke(File ws, VirtualChannel channel) throws IOException {
            Map<String/*module name*/, SvnInfo> revisions = new HashMap<String, SvnInfo>();

            final SVNClientManager manager = createSvnClientManager(authProvider);
            try {
                final SVNWCClient svnWc = manager.getWCClient();
                // invoke the "svn info"
                for (ModuleLocation module : locations) {
                    try {
                        SvnInfo info = new SvnInfo(
                            svnWc.doInfo(new File(ws, module.getLocalDir()), SVNRevision.WORKING));
                        revisions.put(info.url, info);
                    } catch (SVNException e) {
                        e.printStackTrace(listener.error("Failed to parse svn info for " + module.remote));
                    }
                }
                for (External ext : externals) {
                    try {
                        SvnInfo info = new SvnInfo(svnWc.doInfo(new File(ws, ext.path), SVNRevision.WORKING));
                        revisions.put(info.url, info);
                    } catch (SVNException e) {
                        e.printStackTrace(
                            listener.error("Failed to parse svn info for external " + ext.url + " at " + ext.path));
                    }

                }

                return revisions;
            } finally {
                manager.dispose();
            }
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Gets the file that stores the revision.
     */
    public static File getRevisionFile(AbstractBuild build) {
        return new File(build.getRootDir(), "revision.txt");
    }

    /**
     * Gets the file that stores the externals.
     */
    private static File getExternalsFile(AbstractProject project) {
        return new File(project.getRootDir(), "svnexternals.txt");
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener)
        throws IOException, InterruptedException {
        // exclude locations that are svn:external-ed with a fixed revision.
        Map<String, Long> wsRev = parseRevisionFile(build, true);
        for (External e : parseExternalsFile(build.getProject())) {
            if (e.isRevisionFixed()) {
                wsRev.remove(e.url);
            }
        }

        return new SVNRevisionState(wsRev);
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher,
                                                      FilePath workspace, final TaskListener listener,
                                                      SCMRevisionState _baseline)
        throws IOException, InterruptedException {

        if (project.getLastBuild() == null) {
            listener.getLogger().println(Messages.SubversionSCM_pollChanges_noBuilds());
            return PollingResult.BUILD_NOW;
        }

        final SVNRevisionState baseline;
        if (_baseline instanceof SVNRevisionState) {
            baseline = (SVNRevisionState) _baseline;
        } else if (project.getLastBuild() != null) {
            baseline = (SVNRevisionState) calcRevisionsFromBuild(project.getLastBuild(), launcher, listener);
        } else {
            baseline = new SVNRevisionState(null);
        }

        final AbstractBuild<?, ?> lastCompletedBuild = project.getLastCompletedBuild();
        if (lastCompletedBuild != null) {
            if (repositoryLocationsNoLongerExist(lastCompletedBuild, listener)) {
                // Disable this project, see HUDSON-763
                listener.getLogger().println(
                    Messages.SubversionSCM_pollChanges_locationsNoLongerExist(project));
                project.makeDisabled(true);
                return PollingResult.NO_CHANGES;
            }

            // are the locations checked out in the workspace consistent with the current configuration?
            for (ModuleLocation loc : getLocations(lastCompletedBuild)) {
                String locURL = loc.getURL().replace("\\", "/"); //This is required for windows based machines. file:///C:\\dev\\ should be file:///C:/dev/
                if (!baseline.revisions.containsKey(locURL)) {
                    listener.getLogger().println(
                        Messages.SubversionSCM_pollChanges_locationNotInWorkspace(loc.getURL()));
                    return PollingResult.BUILD_NOW;
                }
            }
        }

        // determine where to perform polling. prefer the node where the build happened,
        // in case a cluster is non-uniform. see http://www.nabble.com/svn-connection-from-slave-only-td24970587.html
        VirtualChannel ch = null;
        Node n = lastCompletedBuild != null ? lastCompletedBuild.getBuiltOn() : null;
        if (POLL_FROM_MASTER) {
            n = null;
        }
        if (n != null) {
            Computer c = n.toComputer();
            if (c != null) {
                ch = c.getChannel();
            }
        }
        if (ch == null) {
            ch = MasterComputer.localChannel;
        }
        final String nodeName = n != null ? n.getNodeName() : "master";
        final String projectName = project.getName();

        final SVNLogHandler logHandler = new SVNLogHandler(listener);
        // figure out the remote revisions
        final ISVNAuthenticationProvider authProvider = getDescriptor().createAuthenticationProvider(project);
        final ModuleLocation[] moduleLocations = getLocations(lastCompletedBuild);

        return ch.call(new DelegatingCallable<PollingResult, IOException>() {
            public ClassLoader getClassLoader() {
                return Hudson.getInstance().getPluginManager().uberClassLoader;
            }

            /**
             * Computes {@link PollingResult}. Note that we allow changes that match the certain paths to be excluded,
             * so
             */
            public PollingResult call() throws IOException {
                listener.getLogger()
                    .println("Received SCM poll call on " + nodeName + " for " + projectName + " on "
                        + DateFormat.getDateTimeInstance().format(new Date()));
                final Map<String, Long> revs = new HashMap<String, Long>();
                boolean changes = false;
                boolean significantChanges = false;

                for (Map.Entry<String, Long> baselineInfo : baseline.revisions.entrySet()) {
                    String url = baselineInfo.getKey();
                    long baseRev = baselineInfo.getValue().longValue();
                    /*
                       If we fail to check the remote revision, assume there's no change.
                       In this way, a temporary SVN server problem won't result in bogus builds,
                       which will fail anyway. So our policy in the error handling in the polling
                       is not to fire off builds. see HUDSON-6136.
                    */
                    revs.put(url, Long.valueOf(baseRev));
                    // skip baselineInfo if build location URL contains revision like svn://svnserver/scripts@184375
                    if (!isRevisionSpecifiedInBuildLocation(url, moduleLocations)) {
                        try {
                            final SVNURL svnurl = SVNURL.parseURIDecoded(url);
                            long nowRev = new SvnInfo(parseSvnInfo(svnurl, authProvider)).revision;

                            changes |= (nowRev > baseRev);

                            listener.getLogger()
                                .println(Messages.SubversionSCM_pollChanges_remoteRevisionAt(url, Long.valueOf(nowRev)));
                            revs.put(url, Long.valueOf(nowRev));
                            // make sure there's a change and it isn't excluded
                            if (logHandler.findNonExcludedChanges(svnurl,
                                baseRev + 1, nowRev, authProvider)) {
                                listener.getLogger().println(Messages.SubversionSCM_pollChanges_changedFrom(Long.valueOf(baseRev)));
                                significantChanges = true;
                            }
                        } catch (SVNException e) {
                            e.printStackTrace(listener.error(Messages.SubversionSCM_pollChanges_exception(url)));
                        }
                    }
                }
                assert revs.size() == baseline.revisions.size();
                return new PollingResult(baseline, new SVNRevisionState(revs),
                    significantChanges ? Change.SIGNIFICANT : changes ? Change.INSIGNIFICANT : Change.NONE);
            }
        });
    }

    /**
     * Checks whether build locations contain specified revision.
     *
     * @param url url to verify.
     * @param locations module locations.
     * @return true if build locations contain specified revision.
     */
    boolean isRevisionSpecifiedInBuildLocation(String url, ModuleLocation[] locations) {
        if (null != locations) {
            for (ModuleLocation location : locations) {
                if (location.getURL() != null && location.getURL().contains(url)) {
                    SVNRevision revision = getRevisionFromRemoteUrl(location.getOriginRemote());
                    if (isRevisionPresent(revision)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isRevisionPresent(SVNRevision revision) {
        return revision != null && !(UNDEFINED_REVISION_VALUE.equals(revision.getName()));
    }

    /**
     * Goes through the changes between two revisions and see if all the changes
     * are excluded.
     */
    private final class SVNLogHandler implements ISVNLogEntryHandler, Serializable {
        private boolean changesFound = false;

        private final TaskListener listener;
        private final Pattern[] excludedPatterns = getExcludedRegionsPatterns();
        private final Pattern[] includedPatterns = getIncludedRegionsPatterns();
        private final Set<String> excludedUsers = getExcludedUsersNormalized();
        private final String excludedRevprop = getExcludedRevpropNormalized();
        private final Pattern[] excludedCommitMessages = getExcludedCommitMessagesPatterns();

        private SVNLogHandler(TaskListener listener) {
            this.listener = listener;
        }

        public boolean isChangesFound() {
            return changesFound;
        }

        /**
         * Checks it the revision range [from,to] has any changes that are not excluded via exclusions.
         */
        public boolean findNonExcludedChanges(SVNURL url, long from, long to, ISVNAuthenticationProvider authProvider)
            throws SVNException {
            if (from > to) {
                return false; // empty revision range, meaning no change
            }

            // if no exclusion rules are defined, don't waste time going through "svn log".
            if (!hasExclusionRule()) {
                return true;
            }

            final SVNClientManager manager = createSvnClientManager(authProvider);
            try {
                manager.getLogClient().doLog(url, null, SVNRevision.UNDEFINED,
                    SVNRevision.create(from), // get log entries from the local revision + 1
                    SVNRevision.create(to), // to the remote revision
                    false, // Don't stop on copy.
                    true, // Report paths.
                    false, // Don't included merged revisions
                    0, // Retrieve log entries for unlimited number of revisions.
                    null, // Retrieve all revprops
                    this);
            } finally {
                manager.dispose();
            }

            return isChangesFound();
        }

        /**
         * Is there any exclusion rule?
         */
        private boolean hasExclusionRule() {
            return excludedPatterns.length > 0 || !excludedUsers.isEmpty() || excludedRevprop != null
                || excludedCommitMessages.length > 0 || includedPatterns.length > 0;
        }

        /**
         * Handles a log entry passed.
         * Check for log entries that should be excluded from triggering a build.
         * If an entry is not an entry that should be excluded, set changesFound to true
         *
         * @param logEntry an {@link org.tmatesoft.svn.core.SVNLogEntry} object
         * that represents per revision information
         * (committed paths, log message, etc.)
         * @throws org.tmatesoft.svn.core.SVNException
         *
         */
        public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
            if (checkLogEntry(logEntry)) {
                changesFound = true;
            }
        }

        /**
         * Checks if the given log entry should be considered for the purposes
         * of SCM polling.
         *
         * @return <code>true</code> if the should trigger polling, <code>false</code> otherwise
         */
        private boolean checkLogEntry(SVNLogEntry logEntry) {
            if (excludedRevprop != null) {
                // If the entry includes the exclusion revprop, don't count it as a change
                SVNProperties revprops = logEntry.getRevisionProperties();
                if (revprops != null && revprops.containsName(excludedRevprop)) {
                    listener.getLogger().println(Messages.SubversionSCM_pollChanges_ignoredRevision(
                		Long.valueOf(logEntry.getRevision()),
                        Messages.SubversionSCM_pollChanges_ignoredRevision_revprop(excludedRevprop)));
                    return false;
                }
            }

            String author = logEntry.getAuthor();
            if (excludedUsers.contains(author)) {
                // If the author is an excluded user, don't count this entry as a change
                listener.getLogger().println(Messages.SubversionSCM_pollChanges_ignoredRevision(
            		Long.valueOf(logEntry.getRevision()),
                    Messages.SubversionSCM_pollChanges_ignoredRevision_author(author)));
                return false;
            }

            if (excludedCommitMessages != null) {
                // If the commit message contains one of the excluded messages, don't count it as a change
                String commitMessage = logEntry.getMessage();
                for (Pattern pattern : excludedCommitMessages) {
                    if (pattern.matcher(commitMessage).find()) {
                        return false;
                    }
                }
            }

            // If there were no changes, don't count this entry as a change
            Map changedPaths = logEntry.getChangedPaths();
            if (changedPaths.isEmpty()) {
                return false;
            }

            // If there are included patterns, see which paths are included
            List<String> includedPaths = new ArrayList<String>();
            if (includedPatterns.length > 0) {
                for (String path : (Set<String>) changedPaths.keySet()) {
                    for (Pattern pattern : includedPatterns) {
                        if (pattern.matcher(path).matches()) {
                            includedPaths.add(path);
                            break;
                        }
                    }
                }
            } else {
                includedPaths = new ArrayList<String>(changedPaths.keySet());
            }

            // If no paths are included don't count this entry as a change
            if (includedPaths.isEmpty()) {
                listener.getLogger().println(Messages.SubversionSCM_pollChanges_ignoredRevision(
            		Long.valueOf(logEntry.getRevision()),
                    Messages.SubversionSCM_pollChanges_ignoredRevision_noincpath()));
                return false;
            }

            // Else, check each changed path
            List<String> excludedPaths = new ArrayList<String>();
            if (excludedPatterns.length > 0) {
                for (String path : includedPaths) {
                    for (Pattern pattern : excludedPatterns) {
                        if (pattern.matcher(path).matches()) {
                            excludedPaths.add(path);
                            break;
                        }
                    }
                }
            }

            // If all included paths are in an excluded region, don't count this entry as a change
            if (includedPaths.size() == excludedPaths.size()) {
                listener.getLogger().println(Messages.SubversionSCM_pollChanges_ignoredRevision(
            		Long.valueOf(logEntry.getRevision()),
                    Messages.SubversionSCM_pollChanges_ignoredRevision_path(Util.join(excludedPaths, ", "))));
                return false;
            }

            // Otherwise, a change is a change
            return true;
        }

        private static final long serialVersionUID = 1L;
    }

    public ChangeLogParser createChangeLogParser() {
        return new SubversionChangeLogParser();
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public FilePath getModuleRoot(FilePath workspace) {
        if (getLocations().length > 0) {
            return workspace.child(getLocations()[0].getLocalDir());
        }
        return workspace;
    }

    @Override
    public FilePath[] getModuleRoots(FilePath workspace) {
        final ModuleLocation[] moduleLocations = getLocations();
        if (moduleLocations.length > 0) {
            FilePath[] moduleRoots = new FilePath[moduleLocations.length];
            for (int i = 0; i < moduleLocations.length; i++) {
                moduleRoots[i] = workspace.child(moduleLocations[i].getLocalDir());
            }
            return moduleRoots;
        }
        return new FilePath[]{getModuleRoot(workspace)};
    }

    private static String getLastPathComponent(String s) {
        String[] tokens = s.split("/");
        return tokens[tokens.length - 1]; // return the last token
    }

    @Extension
    public static class DescriptorImpl extends SCMDescriptor<SubversionSCM> implements hudson.model.ModelObject {
        /**
         * SVN authentication realm to its associated credentials.
         * This is the global credential repository.
         */
        private final Map<String, Credential> credentials = new Hashtable<String, Credential>();

        /**
         * Stores name of Subversion revision property to globally exclude
         */
        private String globalExcludedRevprop = null;

        private int workspaceFormat = SVNAdminAreaFactory.WC_FORMAT_14;

        /**
         * Selected global revision policy.
         */
        private RevisionPolicy revisionPolicy = RevisionPolicy.QUEUE_TIME;

        /**
         * When set to true, repository URLs will be validated up to the first
         * dollar sign which is encountered.
         */
        private boolean validateRemoteUpToVar = false;

        /**
         * Stores {@link SVNAuthentication} for a single realm.
         * <p/>
         * <p/>
         * {@link Credential} holds data in a persistence-friendly way,
         * and it's capable of creating {@link SVNAuthentication} object,
         * to be passed to SVNKit.
         */
        public static abstract class Credential implements Serializable {
            /**
             * @param kind One of the constants defined in {@link ISVNAuthenticationManager},
             * indicating what subtype of {@link SVNAuthentication} is expected.
             */
            public abstract SVNAuthentication createSVNAuthentication(String kind) throws SVNException;
        }

        /**
         * Username/password based authentication.
         *
         * @deprecated left for backward compatibility.
         */
        public static final class PasswordCredential extends hudson.scm.credential.PasswordCredential {

            public PasswordCredential(String userName, String password) {
                super(userName, password);
            }
        }

        /**
         * Public key authentication for Subversion over SSH.
         *
         * @deprecated left for backward compatibility.
         */
        public static final class SshPublicKeyCredential extends hudson.scm.credential.SshPublicKeyCredential {

            public SshPublicKeyCredential(String userName, String passphrase, File keyFile) throws SVNException {
                super(userName, passphrase, keyFile);
            }
        }

        /**
         * SSL client certificate based authentication.
         *
         * @deprecated left for backward compatibility.
         */
        public static final class SslClientCertificateCredential
            extends hudson.scm.credential.SslClientCertificateCredential {

            public SslClientCertificateCredential(File certificate, String password) throws IOException {
                super(certificate, password);
            }
        }

        /**
         * Remoting interface that allows remote {@link ISVNAuthenticationProvider}
         * to read from local {@link DescriptorImpl#credentials}.
         */
        interface RemotableSVNAuthenticationProvider extends Serializable {
            Credential getCredential(SerializableSVNURL serializableUrl, String realm) throws SVNException;

            /**
             * Indicates that the specified credential worked.
             */
            void acknowledgeAuthentication(String realm, Credential credential);
        }

        /**
         * Wraps an SVNURL and reconstructs the SVNURL after deserialization
         * 
         * @author Jeff Lauterbach
         *
         */
        static final class SerializableSVNURL implements Serializable {
        	private static final long serialVersionUID = 1L;
        	/**
        	 * The SVNURL that we are wrapping
        	 * SVNURL is not serializable so we have to declare it as transient        	
        	 */
        	private transient SVNURL url;
        	
        	/**
        	 * Decoded SVNURL string that will be used to rebuild an SVNURL 
        	 * object after it has been serialized
        	 */
        	private String decodedUrl;
        	
        	SerializableSVNURL(SVNURL url) {
        		this.url = url;
        		this.decodedUrl = url.toDecodedString();
        	}

			/**
        	 * Returns the SVNURL object that was wrapped 
        	 * @return If this object has not been serialized yet, the original
        	 *         SVNURL used to construct this object will be returned, after
        	 *         serialization a new SVNURL object will be constructed based on 
        	 *         the decoded string of the original SVNURL object
        	 * @throws SVNException
        	 */
        	public SVNURL getSVNURL() throws SVNException {
        		if (url == null) {
        			url = SVNURL.parseURIDecoded(decodedUrl);
        		}
        		
        		return url;
        	} 
        	
        	public synchronized void writeObject(ObjectOutputStream s) throws IOException {
        		s.writeUTF(url.toDecodedString());
        	}
        	
        	public synchronized Object readResolve() throws ObjectStreamException, SVNException {
                try {
            		return new SerializableSVNURL(SVNURL.parseURIDecoded(decodedUrl));
                } catch (SVNException e) {
                    StreamCorruptedException x = new StreamCorruptedException("Failed to load SVNURL");
                    x.initCause(e);
                    throw x;
                }
        	}
        }

        /**
         * There's no point in exporting multiple {@link RemotableSVNAuthenticationProviderImpl} instances,
         * so let's just use one instance.
         */
        private transient final RemotableSVNAuthenticationProviderImpl remotableProvider
            = new RemotableSVNAuthenticationProviderImpl();

        private final class RemotableSVNAuthenticationProviderImpl implements RemotableSVNAuthenticationProvider {
            public Credential getCredential(SerializableSVNURL serializableUrl, String realm) throws SVNException {
                for (SubversionCredentialProvider p : SubversionCredentialProvider.all()) {
                    Credential c = p.getCredential(serializableUrl.getSVNURL(), realm);
                    if (c != null) {
                        LOGGER.fine(String.format("getCredential(%s)=>%s by %s", realm, c, p));
                        return c;
                    }
                }
                LOGGER.fine(String.format("getCredential(%s)=>%s", realm, credentials.get(realm)));
                return credentials.get(realm);
            }

            public void acknowledgeAuthentication(String realm, Credential credential) {
                // this notification is only used on the project-local store.
            }

            /**
             * When sent to the remote node, send a proxy.
             */
            private Object writeReplace() {
                return Channel.current().export(RemotableSVNAuthenticationProvider.class, this);
            }
        }

        /**
         * See {@link DescriptorImpl#createAuthenticationProvider(AbstractProject)}.
         */
        static final class SVNAuthenticationProviderImpl
            implements ISVNAuthenticationProvider, ISVNAuthenticationOutcomeListener, Serializable {
            /**
             * Project-scoped authentication source. For historical reasons, can be null.
             */
            private final RemotableSVNAuthenticationProvider local;

            /**
             * System-wide authentication source. Used as a fallback.
             */
            private final RemotableSVNAuthenticationProvider global;

            /**
             * The {@link Credential} used to create the last {@link SVNAuthentication} that we've tried.
             */
            private Credential lastCredential;

            public SVNAuthenticationProviderImpl(RemotableSVNAuthenticationProvider local,
                                                 RemotableSVNAuthenticationProvider global) {
                this.global = global;
                this.local = local;
            }

            /**
             * For the tests only.
             *
             * @return local SVNAuthenticationProvide (PerJobCredentialStore).
             */
            RemotableSVNAuthenticationProvider getLocal() {
                return local;
            }

            private SVNAuthentication fromProvider(SVNURL url, String realm, String kind,
                                                   RemotableSVNAuthenticationProvider src, String debugName)
                throws SVNException {
                if (src == null) {
                    return null;
                }

                Credential cred = src.getCredential(new SerializableSVNURL(url), realm);
                LOGGER.fine(
                    String.format("%s.requestClientAuthentication(%s,%s,%s)=>%s", debugName, kind, url, realm, cred));
                this.lastCredential = cred;
                if (cred != null) {
                    return cred.createSVNAuthentication(kind);
                }
                return null;
            }

            public SVNAuthentication requestClientAuthentication(String kind, SVNURL url, String realm,
                                                                 SVNErrorMessage errorMessage,
                                                                 SVNAuthentication previousAuth,
                                                                 boolean authMayBeStored) {

                try {
                    SVNAuthentication auth = fromProvider(url, realm, kind, local, "local");

                    // first try the local credential, then the global credential.
                    if (auth == null || compareSVNAuthentications(auth, previousAuth)) {
                        auth = fromProvider(url, realm, kind, global, "global");
                    }

                    if (previousAuth != null && compareSVNAuthentications(auth, previousAuth)) {
                        // See HUDSON-2909
                        // this comparison is necessary, unlike the original fix of HUDSON-2909, since SVNKit may use
                        // other ISVNAuthenticationProviders and their failed auth might be passed to us.
                        // see HUDSON-3936
                        LOGGER.log(Level.FINE, "Previous authentication attempt failed, so aborting: {0}",
                            previousAuth);
                        return null;
                    }

                    if (auth == null && ISVNAuthenticationManager.USERNAME.equals(kind)) {
                        // this happens with file:// URL and svn+ssh (in this case this method gets invoked twice.)
                        // The base class does this, too.
                        // user auth shouldn't be null.
                        return new SVNUserNameAuthentication("", false);
                    }

                    return auth;
                } catch (SVNException e) {
                    LOGGER.log(Level.SEVERE, "Failed to authorize", e);
                    throw new RuntimeException("Failed to authorize", e);
                }
            }

            public void acknowledgeAuthentication(boolean accepted, String kind, String realm,
                                                  SVNErrorMessage errorMessage, SVNAuthentication authentication)
                throws SVNException {
                if (accepted && local != null) {
                    local.acknowledgeAuthentication(realm, lastCredential);
                }
            }

            public int acceptServerAuthentication(SVNURL url, String realm, Object certificate,
                                                  boolean resultMayBeStored) {
                return ACCEPTED_TEMPORARY;
            }

            private static final long serialVersionUID = 1L;
        }

        @Override
        public SCM newInstance(StaplerRequest staplerRequest, JSONObject jsonObject) throws FormException {
            return super.newInstance(staplerRequest, jsonObject);
        }

        public DescriptorImpl() {
            super(SubversionRepositoryBrowser.class);
            load();
        }

        protected DescriptorImpl(Class clazz, Class<? extends RepositoryBrowser> repositoryBrowser) {
            super(clazz, repositoryBrowser);
        }

        public String getDisplayName() {
            return "Subversion";
        }

        public String getGlobalExcludedRevprop() {
            return globalExcludedRevprop;
        }

        public int getWorkspaceFormat() {
            if (workspaceFormat == 0) {
                return SVNAdminAreaFactory.WC_FORMAT_14; // default
            }
            return workspaceFormat;
        }

        public boolean isValidateRemoteUpToVar() {
            return validateRemoteUpToVar;
        }

        /**
         * Returns available choose for revision policy option.
         *
         * @return available choose for revision policy option.
         */
        @Exported
        public RevisionPolicy[] getRevisionOptions() {
            return RevisionPolicy.values();
        }

        /**
         * Returns revision policy.
         *
         * @return revision policy.
         */
        public RevisionPolicy getRevisionPolicy() {
            return revisionPolicy;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            globalExcludedRevprop = Util.fixEmptyAndTrim(
                req.getParameter("svn.global_excluded_revprop"));
            workspaceFormat = Integer.parseInt(req.getParameter("svn.workspaceFormat"));
            validateRemoteUpToVar = formData.containsKey("validateRemoteUpToVar");

            try {
                revisionPolicy = req.getParameter("svn.revisionPolicy") != null ? RevisionPolicy.valueOf(
                    req.getParameter("svn.revisionPolicy")) : RevisionPolicy.QUEUE_TIME;
            } catch (IllegalArgumentException e) {
                revisionPolicy = RevisionPolicy.QUEUE_TIME;
            }

            // Save configuration
            save();

            return super.configure(req, formData);
        }

        @Override
        public boolean isBrowserReusable(SubversionSCM x, SubversionSCM y) {
            ModuleLocation[] xl = x.getLocations(), yl = y.getLocations();
            if (xl.length != yl.length) {
                return false;
            }
            for (int i = 0; i < xl.length; i++) {
                if (!xl[i].getURL().equals(yl[i].getURL())) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Creates {@link ISVNAuthenticationProvider} backed by {@link #credentials}.
         * This method must be invoked on the master, but the returned object is remotable.
         * <p/>
         * <p/>
         * Therefore, to access {@link ISVNAuthenticationProvider}, you need to call this method
         * on the master, then pass the object to the slave side, then call
         * {@link SubversionSCM#createSvnClientManager(ISVNAuthenticationProvider)} on the slave.
         *
         * @see SubversionSCM#createSvnClientManager(ISVNAuthenticationProvider)
         */
        public ISVNAuthenticationProvider createAuthenticationProvider(AbstractProject<?, ?> inContextOf) {
            return new SVNAuthenticationProviderImpl(
                inContextOf == null ? null : new PerJobCredentialStore(inContextOf, null), remotableProvider);
        }

        /**
         * @deprecated as of 1.18
         *             Now that Hudson allows different credentials to be given in different jobs,
         *             The caller should use {@link #createAuthenticationProvider(AbstractProject)} to indicate
         *             the project in which the subversion operation is performed.
         */
        public ISVNAuthenticationProvider createAuthenticationProvider() {
            return new SVNAuthenticationProviderImpl(null, remotableProvider);
        }

        /**
         * Submits the authentication info.
         */
        // TODO: stapler should do multipart/form-data handling
        public void doPostCredential(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            MultipartFormDataParser parser = new MultipartFormDataParser(req);

            // we'll record what credential we are trying here.
            StringWriter log = new StringWriter();
            PrintWriter logWriter = new PrintWriter(log);

            UserProvidedCredential upc = UserProvidedCredential.fromForm(req, parser);

            try {
                postCredential(parser.get("url"), upc, logWriter);
                rsp.sendRedirect("credentialOK");
            } catch (SVNException e) {
                logWriter.println("FAILED: " + e.getErrorMessage());
                req.setAttribute("message", log.toString());
                req.setAttribute("pre", Boolean.TRUE);
                req.setAttribute("exception", e);
                rsp.forward(Hudson.getInstance(), "error", req);
            } finally {
                upc.close();
            }
        }

        /**
         * @deprecated as of 1.18
         *             Use {@link #postCredential(AbstractProject, String, String, String, File, PrintWriter)}
         */
        public void postCredential(String url, String username, String password, File keyFile, PrintWriter logWriter)
            throws SVNException, IOException {
            postCredential(null, url, username, password, keyFile, logWriter);
        }

        public void postCredential(AbstractProject inContextOf, String url, String username, String password,
                                   File keyFile, PrintWriter logWriter) throws SVNException, IOException {
            postCredential(url, new UserProvidedCredential(username, password, keyFile, inContextOf), logWriter);
        }

        /**
         * Submits the authentication info.
         * <p/>
         * This code is fairly ugly because of the way SVNKit handles credentials.
         */
        public void postCredential(final String url, final UserProvidedCredential upc, PrintWriter logWriter)
            throws SVNException, IOException {
            SVNRepository repository = null;

            try {
                // the way it works with SVNKit is that
                // 1) svnkit calls AuthenticationManager asking for a credential.
                //    this is when we can see the 'realm', which identifies the user domain.
                // 2) DefaultSVNAuthenticationManager returns the username and password we set below
                // 3) if the authentication is successful, svnkit calls back acknowledgeAuthentication
                //    (so we store the password info here)
                repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));
                repository.setTunnelProvider(SVNWCUtil.createDefaultOptions(true));
                AuthenticationManagerImpl authManager = upc.new AuthenticationManagerImpl(logWriter) {
                    @Override
                    protected void onSuccess(String realm, Credential cred, Boolean overrideGlobal) {
                        if (overrideGlobal.booleanValue()) {
                            LOGGER.info("Persisted " + cred + " for " + realm);
                            credentials.put(realm, cred);
                            save();
                        }
                        if (upc.inContextOf != null) {
                            LOGGER.info("Persisted " + cred + " for " + url);
                            new PerJobCredentialStore(upc.inContextOf, url).acknowledgeAuthentication(realm, cred);
                        }

                    }
                };
                authManager.setAuthenticationForced(true);
                repository.setAuthenticationManager(authManager);
                repository.testConnection();
                authManager.checkIfProtocolCompleted();
                
                Credential cred = authManager.getCredential();
                String realm = authManager.getRealm();
                
                if (upc.getOverrideGlobal().booleanValue()) {
                	LOGGER.info("Persisted " + cred + " for " + realm);
                	credentials.put(realm, cred);
                	save();
                }
                
                if (upc.inContextOf != null) {
                	LOGGER.info("Persisted " + cred + " for " + url);
                	new PerJobCredentialStore(upc.inContextOf, url).acknowledgeAuthentication(realm, cred);
                }
            } finally {
                if (repository != null) {
                    repository.closeSession();
                }
            }
        }

        /**
         * Validates the value for a remote (repository) location.
         *
         * @param req {@link StaplerRequest}.
         * @param context {@link AbstractProject}.
         * @param value value to validate.
         * @return {@link FormValidation}.
         */
        public FormValidation doCheckRemote(StaplerRequest req, @AncestorInPath AbstractProject context,
                                            @QueryParameter String value) {
            // syntax check first
            String url = Util.nullify(value);
            // fixed HUDSON-7804 issue
            if (url == null) {
                return FormValidation.error(hudson.util.Messages.FormValidation_ValidateRequired());
            }

            // remove unneeded whitespaces
            url = url.trim();

            if (isValidateRemoteUpToVar()) {
                url = (url.indexOf('$') != -1) ? url.substring(0, url.indexOf('$')) : url;
            }

            if (!URL_PATTERN.matcher(url).matches()) {
                return FormValidation.errorWithMarkup(
                    Messages.SubversionSCM_doCheckRemote_invalidUrl());
            }

            // Test the connection only if we have admin permission
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
                return FormValidation.ok();
            }

            try {
                String urlWithoutRevision = getUrlWithoutRevision(url);

                SVNURL repoURL = SVNURL.parseURIDecoded(urlWithoutRevision);
                if (checkRepositoryPath(context, repoURL) != SVNNodeKind.NONE) {
                    // something exists; now check revision if any

                    SVNRevision revision = getRevisionFromRemoteUrl(url);
                    if (isRevisionPresent(revision) && !revision.isValid()) {
                        return FormValidation.errorWithMarkup(Messages.SubversionSCM_doCheckRemote_invalidRevision());
                    }

                    return FormValidation.ok();
                }

                SVNRepository repository = null;
                try {
                    repository = getRepository(context, repoURL);
                    long rev = repository.getLatestRevision();
                    // now go back the tree and find if there's anything that exists
                    String repoPath = getRelativePath(repoURL, repository);
                    String p = repoPath;
                    while (p.length() > 0) {
                        p = SVNPathUtil.removeTail(p);
                        if (repository.checkPath(p, rev) == SVNNodeKind.DIR) {
                            // found a matching path
                            List<SVNDirEntry> entries = new ArrayList<SVNDirEntry>();
                            repository.getDir(p, rev, false, entries);

                            // build up the name list
                            List<String> paths = new ArrayList<String>();
                            for (SVNDirEntry e : entries) {
                                if (e.getKind() == SVNNodeKind.DIR) {
                                    paths.add(e.getName());
                                }
                            }

                            String head = SVNPathUtil.head(repoPath.substring(p.length() + 1));
                            String candidate = EditDistance.findNearest(head, paths);

                            return FormValidation.error(
                                Messages.SubversionSCM_doCheckRemote_badPathSuggest(p, head,
                                    candidate != null ? "/" + candidate : ""));
                        }
                    }

                    return FormValidation.error(
                        Messages.SubversionSCM_doCheckRemote_badPath(repoPath));
                } finally {
                    if (repository != null) {
                        repository.closeSession();
                    }
                }
            } catch (SVNException e) {
                LOGGER.log(Level.INFO, Messages.SubversionSCM_doCheckRemote_accessRepository_error(url), e);
                String message = Messages.SubversionSCM_doCheckRemote_exceptionMsg1(
                    Util.escape(url), Util.escape(e.getErrorMessage().getFullMessage()),
                    "javascript:document.getElementById('svnerror').style.display='block';"
                        + "document.getElementById('svnerrorlink').style.display='none';"
                        + "return false;")
                    + "<br/><pre id=\"svnerror\" style=\"display:none\">"
                    + Functions.printThrowable(e) + "</pre>"
                    + Messages.SubversionSCM_doCheckRemote_exceptionMsg2(
                    "descriptorByName/" + SubversionSCM.class.getName() + "/enterCredential?" + url);
                return FormValidation.errorWithMarkup(message);
            }
        }

        public SVNNodeKind checkRepositoryPath(AbstractProject context, SVNURL repoURL) throws SVNException {
            SVNRepository repository = null;

            try {
                repository = getRepository(context, repoURL);
                repository.testConnection();

                long rev = repository.getLatestRevision();
                String repoPath = getRelativePath(repoURL, repository);
                return repository.checkPath(repoPath, rev);
            } finally {
                if (repository != null) {
                    repository.closeSession();
                }
            }
        }

        protected SVNRepository getRepository(AbstractProject context, SVNURL repoURL) throws SVNException {
            SVNRepository repository = SVNRepositoryFactory.create(repoURL);

            ISVNAuthenticationManager sam = new DefaultSVNAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager());
            sam = new FilterSVNAuthenticationManager(sam) {
                // If there's no time out, the blocking read operation may hang forever, because TCP itself
                // has no timeout. So always use some time out. If the underlying implementation gives us some
                // value (which may come from ~/.subversion), honor that, as long as it sets some timeout value.
                @Override
                public int getReadTimeout(SVNRepository repository) {
                    int r = super.getReadTimeout(repository);
                    if (r <= 0) {
                        r = DEFAULT_TIMEOUT;
                    }
                    return r;
                }
            };
            sam.setAuthenticationProvider(createAuthenticationProvider(context));
            repository.setTunnelProvider(SVNWCUtil.createDefaultOptions(true));
            repository.setAuthenticationManager(sam.getAuthenticationManager());

            return repository;
        }

        public static String getRelativePath(SVNURL repoURL, SVNRepository repository) throws SVNException {
            String repoPath = repoURL.getPath().substring(repository.getRepositoryRoot(false).getPath().length());
            if (repoPath.length() > 0 && repoPath.charAt(0) != '/') {
                repoPath = "/" + repoPath;
            }
            return repoPath;
        }


        /**
         * Validates the value for a local location (local checkout directory).
         *
         * @param value value to validate.
         * @return {@link FormValidation}.
         * @throws java.io.IOException            IOException.
         * @throws javax.servlet.ServletException ServletException.
         */
        public FormValidation doCheckLocal(@QueryParameter String value) throws IOException, ServletException {
            String v = Util.nullify(value);
            if (v == null)
            // local directory is optional so this is ok
            {
                return FormValidation.ok();
            }

            v = v.trim();

            // check if a absolute path has been supplied
            // (the last check with the regex will match windows drives)
            if (v.charAt(0) == '/' || v.charAt(0) == '\\' || v.startsWith("..") || v.matches("^[A-Za-z]:.*")) {
                return FormValidation.error("absolute path is not allowed");
            }

            // all tests passed so far
            return FormValidation.ok();
        }

        /**
         * Validates the excludeRegions Regex.
         *
         * @param value value to validate.
         * @return {@link FormValidation}.
         * @throws java.io.IOException            IOException.
         * @throws javax.servlet.ServletException ServletException.
         */
        public FormValidation doCheckExcludedRegions(@QueryParameter String value)
            throws IOException, ServletException {
            for (String region : Util.fixNull(value).trim().split("[\\r\\n]+")) {
                try {
                    Pattern.compile(region);
                } catch (PatternSyntaxException e) {
                    return FormValidation.error("Invalid regular expression. " + e.getMessage());
                }
            }
            return FormValidation.ok();
        }

        /**
         * Validates the includedRegions Regex.
         *
         * @param value value to validate.
         * @return {@link FormValidation}.
         * @throws java.io.IOException            IOException.
         * @throws javax.servlet.ServletException ServletException.
         */
        public FormValidation doCheckIncludedRegions(@QueryParameter String value)
            throws IOException, ServletException {
            return doCheckExcludedRegions(value);
        }

        /**
         * Regular expression for matching one username. Matches 'windows' names ('DOMAIN&#92;user') and
         * 'normal' names ('user'). Where user (and DOMAIN) has one or more characters in 'a-zA-Z_0-9-.')
         */
        private static final Pattern USERNAME_PATTERN = Pattern.compile("(\\w+\\\\)?+((\\w|[-\\.])+)");

        /**
         * Validates the excludeUsers field.
         *
         * @param value value to validate.
         * @return {@link FormValidation}.
         * @throws java.io.IOException            IOException.
         * @throws javax.servlet.ServletException ServletException.
         */
        public FormValidation doCheckExcludedUsers(@QueryParameter String value) throws IOException, ServletException {
            for (String user : Util.fixNull(value).trim().split("[\\r\\n]+")) {
                user = user.trim();

                if ("".equals(user)) {
                    continue;
                }

                if (!validateExcludedUser(user)) {
                    return FormValidation.error("Invalid username: " + user);
                }
            }

            return FormValidation.ok();
        }

        public List<WorkspaceUpdaterDescriptor> getWorkspaceUpdaterDescriptors() {
            return WorkspaceUpdaterDescriptor.all();
        }

        /**
         * Validates the excludeCommitMessages field.
         *
         * @param value value to validate.
         * @return {@link FormValidation}.
         * @throws java.io.IOException            IOException.
         * @throws javax.servlet.ServletException ServletException.
         */
        public FormValidation doCheckExcludedCommitMessages(@QueryParameter String value)
            throws IOException, ServletException {
            for (String message : Util.fixNull(value).trim().split("[\\r\\n]+")) {
                try {
                    Pattern.compile(message);
                } catch (PatternSyntaxException e) {
                    return FormValidation.error("Invalid regular expression. " + e.getMessage());
                }
            }
            return FormValidation.ok();
        }

        /**
         * Validates the remote server supports custom revision properties.
         *
         * @param context {@link AbstractProject}.
         * @param value value to validate.
         * @return {@link FormValidation}.
         * @throws java.io.IOException            IOException.
         * @throws javax.servlet.ServletException ServletException.
         */
        public FormValidation doCheckRevisionPropertiesSupported(@AncestorInPath AbstractProject context,
                                                                 @QueryParameter String value)
            throws IOException, ServletException {
            String v = Util.fixNull(value).trim();
            if (v.length() == 0) {
                return FormValidation.ok();
            }

            // Test the connection only if we have admin permission
            if (!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
                return FormValidation.ok();
            }

            try {
                SVNURL repoURL = SVNURL.parseURIDecoded(v);
                if (checkRepositoryPath(context, repoURL) != SVNNodeKind.NONE)
                // something exists
                {
                    return FormValidation.ok();
                }

                SVNRepository repository = null;
                try {
                    repository = getRepository(context, repoURL);
                    if (repository.hasCapability(SVNCapability.LOG_REVPROPS)) {
                        return FormValidation.ok();
                    }
                } finally {
                    if (repository != null) {
                        repository.closeSession();
                    }
                }
            } catch (SVNException e) {
                String message = "";
                message += "Unable to access " + Util.escape(v) + " : " + Util.escape(
                    e.getErrorMessage().getFullMessage());
                LOGGER.log(Level.INFO, "Failed to access subversion repository " + v, e);
                return FormValidation.errorWithMarkup(message);
            }

            return FormValidation.warning(Messages.SubversionSCM_excludedRevprop_notSupported(v));
        }

        static {
            new Initializer();
        }

        /**
         * Validates the excluded user name.
         *
         * @param value value to validate.
         * @return true if user name is valid.
         */
        static boolean validateExcludedUser(String value) {
            return USERNAME_PATTERN.matcher(value).matches();
        }
    }

    public boolean repositoryLocationsNoLongerExist(AbstractBuild<?, ?> build, TaskListener listener) {
        PrintStream out = listener.getLogger();

        for (ModuleLocation l : getLocations(build)) {
            try {
                if (getDescriptor().checkRepositoryPath(build.getProject(), l.getSVNURL()) == SVNNodeKind.NONE) {
                    out.println("Location '" + l.remote + "' does not exist");

                    ParametersAction params = build.getAction(ParametersAction.class);
                    if (params != null) {
                        // since this is used to disable projects, be conservative
                        LOGGER.fine("Location could be expanded on build '" + build
                            + "' parameters values:");
                        return false;
                    }
                    return true;
                }
            } catch (SVNException e) {
                // be conservative, since we are just trying to be helpful in detecting
                // non existent locations. If we can't detect that, we'll do nothing
                LOGGER.log(Level.FINE, "Location check failed", e);
            }
        }
        return false;
    }

    static final Pattern URL_PATTERN = Pattern.compile("(https?|svn(\\+[a-z0-9]+)?|file)://.+");

    private static final long serialVersionUID = 1L;

    // noop, but this forces the initializer to run.
    public static void init() {
    }

    static {
        new Initializer();
    }


    private static final class Initializer {
        static {
            if (Boolean.getBoolean("hudson.spool-svn")) {
                DAVRepositoryFactory.setup(new DefaultHTTPConnectionFactory(null, true, null));
            } else {
                DAVRepositoryFactory.setup();   // http, https
            }
            SVNRepositoryFactoryImpl.setup();   // svn, svn+xxx
            FSRepositoryFactory.setup();    // file

            // disable the connection pooling, which causes problems like
            // http://www.nabble.com/SSH-connection-problems-p12028339.html
            if (System.getProperty("svnkit.ssh2.persistent") == null) {
                System.setProperty("svnkit.ssh2.persistent", "false");
            }

            // push Negotiate to the end because it requires a valid Kerberos configuration.
            // see HUDSON-8153
            if (System.getProperty("svnkit.http.methods") == null) {
                System.setProperty("svnkit.http.methods", "Digest,Basic,NTLM,Negotiate");
            }

            // use SVN1.4 compatible workspace by default.
            
            SVNAdminAreaFactory.setSelector(new SubversionWorkspaceSelector());
        }
    }

    /**
     * small structure to store local and remote (repository) location
     * information of the repository. As a addition it holds the invalid field
     * to make failure messages when doing a checkout possible
     */
    @ExportedBean
    public static final class ModuleLocation implements Serializable {
        /**
         * Subversion URL to check out.
         * <p/>
         * This may include "@NNN" at the end to indicate a fixed revision.
         */
        @Exported
        public final String remote;

        /**
         * Remembers the user-given value.
         * Can be null.
         *
         * @deprecated Code should use {@link #getLocalDir()}. This field is only intended for form binding.
         */
        @Exported
        public final String local;

        /**
         * Subversion remote depth. Used as "--depth" option for checkout and update commands.
         * Default value is "infinity".
         */
        @Exported
        public final String depthOption;

        /**
         * Flag to ignore subversion externals definitions.
         */
        @Exported
        public boolean ignoreExternalsOption;

        /**
         * Cache of the repository UUID.
         */
        private transient volatile UUID repositoryUUID;
        private transient volatile SVNURL repositoryRoot;

        /**
         * Constructor to support backward compatibility.
         *
         * @param remote remote repository.
         * @param local local repository.
         */
        public ModuleLocation(String remote, String local) {
            this(remote, local, null, false);
        }

        @DataBoundConstructor
        public ModuleLocation(String remote, String local, String depthOption, boolean ignoreExternalsOption) {
            this.remote = Util.removeTrailingSlash(Util.fixNull(remote).trim());
            this.local = Util.fixEmptyAndTrim(local);
            this.depthOption = StringUtils.isEmpty(depthOption) ? SVNDepth.INFINITY.getName()
                : depthOption;
            this.ignoreExternalsOption = ignoreExternalsOption;
        }

        /**
         * Local directory to place the file to.
         * Relative to the workspace root.
         */
        public String getLocalDir() {
            if (local == null) {
                return getLastPathComponent(remote);
            }
            return local;
        }

        /**
         * Returns the pure URL portion of {@link #remote} by removing
         * possible "@NNN" suffix.
         */
        public String getURL() {
            return getUrlWithoutRevision(remote);
        }

        /**
         * Returns origin remote url, it can be specified with "@NNN" suffix.
         *
         * @return origin remote url.
         */
        public String getOriginRemote() {
            return remote;
        }


        /**
         * Gets {@link #remote} as {@link SVNURL}.
         */
        public SVNURL getSVNURL() throws SVNException {
            return SVNURL.parseURIEncoded(getURL());
        }

        /**
         * Repository UUID. Lazy computed and cached.
         */
        public UUID getUUID(AbstractProject context) throws SVNException {
            if (repositoryUUID == null || repositoryRoot == null) {
                synchronized (this) {
                    SVNRepository r = openRepository(context);
                    r.testConnection(); // make sure values are fetched
                    repositoryUUID = UUID.fromString(r.getRepositoryUUID(false));
                    repositoryRoot = r.getRepositoryRoot(false);
                }
            }
            return repositoryUUID;
        }

        public SVNRepository openRepository(AbstractProject context) throws SVNException {
            return Hudson.getInstance().getDescriptorByType(DescriptorImpl.class).getRepository(context, getSVNURL());
        }

        public SVNURL getRepositoryRoot(AbstractProject context) throws SVNException {
            getUUID(context);
            return repositoryRoot;
        }

        /**
         * Figures out which revision to check out.
         * <p/>
         * If {@link #remote} is {@code url@rev}, then this method
         * returns that specific revision.
         *
         * @param defaultValue If "@NNN" portion is not in the URL, this value will be returned.
         * Normally, this is the SVN revision timestamped at the build date.
         */
        public SVNRevision getRevision(SVNRevision defaultValue) {
            SVNRevision revision = getRevisionFromRemoteUrl(remote);
            return revision != null ? revision : defaultValue;
        }

        private String getExpandedRemote(AbstractBuild<?, ?> build) {
            String outRemote = remote;

            ParametersAction parameters = build.getAction(ParametersAction.class);
            if (parameters != null) {
                outRemote = parameters.substitute(build, remote);
            }

            return outRemote;
        }

        /**
         * Returns the value of remote depth option.
         *
         * @return the value of remote depth option.
         */
        public String getDepthOption() {
            return depthOption;
        }

        /**
         * Determines if subversion externals definitions should be ignored.
         *
         * @return true if subversion externals definitions should be ignored.
         */
        public boolean isIgnoreExternalsOption() {
            return ignoreExternalsOption;
        }

        /**
         * Expand location value based on Build parametric execution.
         *
         * @param build Build instance for expanding parameters into their values
         * @return Output ModuleLocation expanded according to Build parameters
         *         values.
         */
        public ModuleLocation getExpandedLocation(AbstractBuild<?, ?> build) {
            return new ModuleLocation(getExpandedRemote(build), getLocalDir(), getDepthOption(),
                isIgnoreExternalsOption());
        }

        @Override
        public String toString() {
            return remote;
        }

        private static final long serialVersionUID = 1L;

        //TODO javadoc me
        public static List<ModuleLocation> parse(String[] remoteLocations, String[] localLocations,
                                                 String[] depthOptions, boolean[] isIgnoreExternals) {
            List<ModuleLocation> modules = new ArrayList<ModuleLocation>();
            if (remoteLocations != null && localLocations != null) {
                int entries = Math.min(remoteLocations.length, localLocations.length);

                for (int i = 0; i < entries; i++) {
                    // the remote (repository) location
                    String remoteLoc = Util.nullify(remoteLocations[i]);

                    if (remoteLoc != null) {// null if skipped
                        remoteLoc = Util.removeTrailingSlash(remoteLoc.trim());
                        modules.add(new ModuleLocation(remoteLoc, Util.nullify(localLocations[i]),
                            depthOptions != null ? depthOptions[i] : null,
                            isIgnoreExternals != null && isIgnoreExternals[i]));
                    }
                }
            }
            return modules;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ModuleLocation)) {
                return false;
            }

            ModuleLocation that = (ModuleLocation) o;
            return new EqualsBuilder()
                .append(depthOption, that.depthOption)
                .append(getLocalDir(), that.getLocalDir())
                .append(remote, that.remote)
                .append(repositoryRoot, that.repositoryRoot)
                .append(repositoryUUID, that.repositoryUUID)
                .append(ignoreExternalsOption, that.ignoreExternalsOption)
                .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder()
                .append(remote)
                .append(getLocalDir())
                .append(depthOption)
                .append(ignoreExternalsOption)
                .append(repositoryUUID)
                .append(repositoryRoot).hashCode();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(SubversionSCM.class.getName());

    /**
     * Network timeout in milliseconds.
     * The main point of this is to prevent infinite hang, so it should be a rather long value to avoid
     * accidental time out problem.
     */
    public static int DEFAULT_TIMEOUT = Integer.getInteger(SubversionSCM.class.getName() + ".timeout", 3600 * 1000).intValue();

    /**
     * Property to control whether SCM polling happens from the slave or master
     */
    public static boolean POLL_FROM_MASTER = Boolean.getBoolean(SubversionSCM.class.getName() + ".pollFromMaster");

    /**
     * Enables trace logging of Ganymed SSH library.
     * <p/>
     * Intended to be invoked from Groovy console.
     */
    public static void enableSshDebug(Level level) {
        if (level == null) {
            level = Level.FINEST; // default
        }

        final Level lv = level;

        com.trilead.ssh2.log.Logger.enabled = true;
        com.trilead.ssh2.log.Logger.logger = new DebugLogger() {
            private final Logger LOGGER = Logger.getLogger(SCPClient.class.getPackage().getName());

            public void log(int level, String className, String message) {
                LOGGER.log(lv, className + ' ' + message);
            }
        };
    }

    /*package*/
    static boolean compareSVNAuthentications(SVNAuthentication a1, SVNAuthentication a2) {
        if (a1 == null && a2 == null) {
            return true;
        }
        if (a1 == null || a2 == null) {
            return false;
        }
        if (a1.getClass() != a2.getClass()) {
            return false;
        }

        try {
            return describeBean(a1).equals(describeBean(a2));
        } catch (IllegalAccessException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * In preparation for a comparison, char[] needs to be converted that supports value equality.
     */
    private static Map describeBean(Object o)
        throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Map<?, ?> m = PropertyUtils.describe(o);
        for (Entry e : m.entrySet()) {
            Object v = e.getValue();
            if (v instanceof char[]) {
                char[] chars = (char[]) v;
                e.setValue(new String(chars));
            }
        }
        return m;
    }

    static String getUrlWithoutRevision(
        String remoteUrlPossiblyWithRevision) {
        int idx = remoteUrlPossiblyWithRevision.lastIndexOf('@');
        if (idx > 0) {
            String n = remoteUrlPossiblyWithRevision.substring(idx + 1);
            SVNRevision r = SVNRevision.parse(n);
            if ((r != null) && (r.isValid())) {
                return remoteUrlPossiblyWithRevision.substring(0, idx);
            }
        }
        return remoteUrlPossiblyWithRevision;
    }

    /**
     * Gets the revision from a remote URL - i.e. the part after '@' if any
     *
     * @return the revision or null
     */
    private static SVNRevision getRevisionFromRemoteUrl(
        String remoteUrlPossiblyWithRevision) {
        int idx = remoteUrlPossiblyWithRevision.lastIndexOf('@');
        if (idx > 0) {
            String n = remoteUrlPossiblyWithRevision.substring(idx + 1);
            return SVNRevision.parse(n);
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        
        if (!(o instanceof SubversionSCM)) {
            return false;
        }

        SubversionSCM that = (SubversionSCM) o;

        if (!isEqualsWithoutOrdering(locations, that.locations)) {
            return false;
        }
        
        return new EqualsBuilder()
            .append(browser, that.browser)
            .append(workspaceUpdater, that.workspaceUpdater)
            .append(excludedCommitMessages, that.excludedCommitMessages)
            .append(excludedRegions, that.excludedRegions)
            .append(excludedRevprop, that.excludedRevprop)
            .append(excludedUsers, that.excludedUsers)
            .append(includedRegions, that.includedRegions)
            .isEquals();
    }

    /**
     * Verify if two arrays of objects are equal without same order of elements.
     * TODO: Remove this methods
     * @param array1 first array.
     * @param array2 second array.
     * @return true if two arrays equals and false in other way.
     */
    public static boolean isEqualsWithoutOrdering(Object[] array1, Object[] array2) {
        if (array1 == null && array2 == null) {
            return true;
        }
        if (array1 == null || array2 == null) {
            return false;
        }
        if (array1.length != array2.length) {
            return false;
        }

        for (int i = 0; i < array1.length; i++) {
            boolean contains = false;
            for (int j = 0; j < array2.length; j++) {
                if (array1[i] == null && array2[i] == null) {
                    contains = true;
                    break;
                }

                if (array1[i] != null && array1[i].equals(array2[j])) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int locationsHash = 0;
        for (ModuleLocation location : locations) {
            locationsHash += location.hashCode();
        }
        return new HashCodeBuilder()
            .append(locationsHash)
            .append(browser)
            .append(excludedRegions)
            .append(includedRegions)
            .append(excludedUsers)
            .append(excludedRevprop)
            .append(excludedCommitMessages)
            .append(workspaceUpdater)
            .hashCode();
    }
}
