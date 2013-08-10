package hudson.scm;

import hudson.ClassicPluginStrategy;
import hudson.Launcher.LocalLauncher;
import hudson.Proc;
import hudson.scm.SubversionSCM.DescriptorImpl;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;

import org.jvnet.hudson.test.HudsonHomeLoader.CopyExisting;
import org.jvnet.hudson.test.HudsonTestCase;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

/**
 * Base class for Subversion related tests.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class AbstractSubversionTest extends HudsonTestCase {
    protected DescriptorImpl descriptor;
    protected String kind = ISVNAuthenticationManager.PASSWORD;


    @Override
    protected void setUp() throws Exception {
        //Enable classic plugin strategy, because some extensions are duplicated with default strategy
        System.setProperty("hudson.PluginStrategy", "hudson.ClassicPluginStrategy");
        super.setUp();
        descriptor = hudson.getDescriptorByType(DescriptorImpl.class);
    }

    protected Proc runSvnServe(URL zip) throws Exception {
        return runSvnServe(new CopyExisting(zip).allocate());
    }

    /**
     * Runs svnserve to serve the specified directory as a subversion repository.
     * @throws IOException 
     * @throws InterruptedException 
     */
    protected Proc runSvnServe(File repo) throws IOException, InterruptedException {
        LocalLauncher launcher = new LocalLauncher(new StreamTaskListener(System.out, null));
        try {
            launcher.launch().cmds("svnserve", "--help").start().join();
        } catch (IOException e) {
            // if we fail to launch svnserve, skip the test
        	fail();
        }
        Socket s = null;
        try {
        	s = new Socket("localhost", 3690);
        	
        	// Reaching this point implies that port 3690 received a response. 
        	fail();
        } catch (IOException e) {
        	// Failed to receive any reposnse from port 3690. That means port is available.
        } finally {
        	if (s != null) s.close();
        }
        
        // Now since we verified port is not in use let's run svnserve -d.
        return launcher.launch().cmds(
            "svnserve", "-d", "--foreground", "-r", repo.getAbsolutePath()).pwd(repo).start();
    }

    protected ISVNAuthenticationManager createInMemoryManager() {
        ISVNAuthenticationManager m = SVNWCUtil.createDefaultAuthenticationManager(hudson.root, null, null, false);
        m.setAuthenticationProvider(descriptor.createAuthenticationProvider(null));
        return m;
    }

    static {
        ClassicPluginStrategy.useAntClassLoader = true;
    }
}
