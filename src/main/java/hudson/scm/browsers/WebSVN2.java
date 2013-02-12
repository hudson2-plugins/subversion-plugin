package hudson.scm.browsers;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.scm.RepositoryBrowser;
import hudson.scm.SubversionChangeLogSet;
import hudson.scm.SubversionRepositoryBrowser;
import hudson.util.FormValidation;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * {@link SubversionRepositoryBrowser} that produces links to http://www.websvn.info/ for SVN
 * compatible with Version 2.3.1 of WebSVN.
 *
 * @author Andreas Mandel, based on ViewVC plugin by Mike Salnikov, based on Polarion plug-in by Jonny Wray
 * @author Nikita Levyankov, changes were moved from WebSVN2 plugin
 */
public class WebSVN2 extends SubversionRepositoryBrowser {

    // alternative: https://server/websvn/comp.php?repname=rep&compare[]=/@2222&compare[]=/@2225
    private static final String CHANGE_SET_FORMAT = "revision.php?%1srev=%2d";

    private static final String DIFF_FORMAT = "diff.php?%1spath=%2s&rev=%3d";

    // alternative: "blame.php?%1spath=%2s&rev=%3d";
    private static final String FILE_FORMAT = "filedetails.php?%1spath=%2s&rev=%3d";

    private static final Pattern URL_PATTERN
        = Pattern.compile("(.*/)(revision|diff|comp|filedetails|listing|blame|dl|log)"
        + "\\.php([^?]*)\\?(repname=([^&]*))?(.*)");
    private static final int URL_PATTERN_BASE_URL_GROUP = 1;
    private static final int URL_PATTERN_REPNAME_GROUP = 4;

    public final URL url;
    private final URL baseUrl;
    private final String repname;

    @DataBoundConstructor
    public WebSVN2(URL url) throws MalformedURLException {
        final Matcher webSVNurl = URL_PATTERN.matcher(url.toString());
        this.url = url;
        if (!webSVNurl.matches()) {
            this.repname = "";
            this.baseUrl = url;
        } else {
            this.baseUrl = new URL(webSVNurl.group(URL_PATTERN_BASE_URL_GROUP));
            this.repname = webSVNurl.group(URL_PATTERN_REPNAME_GROUP) + "&";
        }
    }

    @Override
    public URL getDiffLink(SubversionChangeLogSet.Path path) throws IOException {
        return new URL(this.baseUrl,
            String.format(DIFF_FORMAT, this.repname, URLEncoder.encode(path.getValue(), "UTF-8"),
            		Integer.valueOf(path.getLogEntry().getRevision())));
    }

    @Override
    public URL getFileLink(SubversionChangeLogSet.Path path) throws IOException {
        return new URL(this.baseUrl,
            String.format(FILE_FORMAT, this.repname, URLEncoder.encode(path.getValue(), "UTF-8"),
            		Integer.valueOf(path.getLogEntry().getRevision())));
    }

    @Override
    public URL getChangeSetLink(SubversionChangeLogSet.LogEntry logEntry) throws IOException {
        return new URL(this.baseUrl, String.format(CHANGE_SET_FORMAT, this.repname, Integer.valueOf(logEntry.getRevision())));
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<RepositoryBrowser<?>> {
        public String getDisplayName() {
            return "WebSVN2";
        }

        public FormValidation doCheckUrl(@AncestorInPath AbstractProject project,
                                         @QueryParameter(fixEmpty = true) final String value) {
            FormValidation result;
            if (value == null) {
                return FormValidation.ok();
            }
            final Matcher matcher = URL_PATTERN.matcher(value);
            if (matcher.matches()) {
                try {
                    new URL(matcher.group(URL_PATTERN_BASE_URL_GROUP));
                    final String repName = matcher.group(URL_PATTERN_REPNAME_GROUP);
                    if (StringUtils.isBlank(repName)) {   // Go online??
                        result = FormValidation.okWithMarkup(
                            "Please set a url including the repname property if needed.");
                    } else {
                        result = FormValidation.ok();
                    }
                } catch (MalformedURLException ex) {
                    result = FormValidation.error("The entered url is not accepted: " + ex.getLocalizedMessage());
                }
            } else if (StringUtils.isBlank(value)) {
                result = FormValidation.okWithMarkup(
                    "Please set a WebSVN url in the form "
                        + "https://<i>server</i>/websvn/listing.php?repname=<i>rep</i>&path=/trunk/..");
            } else {
                result = FormValidation.error("Please set a url including the WebSVN php script.");
            }
            return result;
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WebSVN2)) {
            return false;
        }

        WebSVN2 that = (WebSVN2) o;

        return new EqualsBuilder()
            .append(baseUrl, that.baseUrl)
            .append(repname, that.repname)
            .append(url, that.url)
            .isEquals();
    }

    @Override
    public final int hashCode() {
        return new HashCodeBuilder()
            .append(baseUrl)
            .append(repname)
            .append(url)
            .toHashCode();
    }
}
