/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 * Anton Kozak
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import hudson.Plugin;
import hudson.XmlFile;
import hudson.model.Items;
import hudson.model.Run;
import hudson.model.Hudson;
import java.io.IOException;
import org.eclipse.hudson.scm.subversion.SubversionSCM.DescriptorImpl;
import org.eclipse.hudson.scm.subversion.SubversionSCM.ModuleLocation;

/**
 * Plugin entry point.
 *
 * @author Anton Kozak
 */
public class PluginImpl extends Plugin {

    @Override
    public void start() throws IOException {
        setXtreamAliasForBackwardCompatibility();
    }
    
    /**
     * Register XStream aliases for backward compatibility - should be removed eventually
     */
    public static void setXtreamAliasForBackwardCompatibility(){
        Items.XSTREAM.alias("hudson.scm.SubversionSCM", SubversionSCM.class);
        Items.XSTREAM.alias("hudson.scm.subversion.UpdateUpdater", UpdateUpdater.class);
        Items.XSTREAM.alias("hudson.scm.subversion.UpdateWithCleanUpdater", UpdateWithCleanUpdater.class);
        Items.XSTREAM.alias("hudson.scm.subversion.CheckoutUpdater", CheckoutUpdater.class);
        Items.XSTREAM.alias("hudson.scm.subversion.CheckoutWithLocationFoldersCleanupUpdater",
                CheckoutWithLocationFoldersCleanupUpdater.class);
        Items.XSTREAM.alias("hudson.scm.subversion.UpdateWithRevertUpdater", UpdateWithRevertUpdater.class);
        Items.XSTREAM.alias("hudson.scm.PerJobCredentialStore", PerJobCredentialStore.class);
        Items.XSTREAM.alias("hudson.scm.SubversionChangeLogParser", SubversionChangeLogParser.class);
        Items.XSTREAM.alias("hudson.scm.SVNRevisionState", SVNRevisionState.class);
        Items.XSTREAM.alias("hudson.scm.SubversionSCM$ModuleLocation", ModuleLocation.class);
        Items.XSTREAM.alias("hudson.scm.SubversionTagAction", SubversionTagAction.class);

        XmlFile.DEFAULT_XSTREAM.alias("hudson.scm.SubversionSCM$DescriptorImpl$PasswordCredential",
                DescriptorImpl.PasswordCredential.class);
        XmlFile.DEFAULT_XSTREAM.alias("hudson.scm.SubversionSCM$DescriptorImpl$SshPublicKeyCredential",
                DescriptorImpl.SshPublicKeyCredential.class);
        XmlFile.DEFAULT_XSTREAM.alias("hudson.scm.SubversionSCM$DescriptorImpl$SslClientCertificateCredential",
                DescriptorImpl.SslClientCertificateCredential.class);
        XmlFile.DEFAULT_XSTREAM.alias("hudson.scm.SubversionSCM$DescriptorImpl", DescriptorImpl.class);
        XmlFile.DEFAULT_XSTREAM.alias("hudson.scm.SubversionTagAction", SubversionTagAction.class);

        Run.XSTREAM.alias("hudson.scm.SubversionTagAction", SubversionTagAction.class);

        Hudson.XSTREAM.alias("hudson.scm.SubversionTagAction", SubversionTagAction.class);
    }
}