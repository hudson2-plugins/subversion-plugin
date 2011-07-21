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
 * Nikita Levyankov
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.config;

import com.thoughtworks.xstream.XStream;
import hudson.XmlFile;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import hudson.matrix.MatrixProject;
import hudson.model.FreeStyleProject;
import hudson.model.Items;
import org.apache.commons.io.FileUtils;
import org.junit.Before;

public abstract class BaseLegacyConverterTest {
    private File sourceConfigFile;
    private File targetConfigFile;
    public static final com.thoughtworks.xstream.XStream XSTREAM = Items.XSTREAM;

    static {
        XSTREAM.alias("project",FreeStyleProject.class);
        XSTREAM.alias("matrix-project",MatrixProject.class);
    }

    @Before
    public void setUp() throws URISyntaxException, IOException {
        sourceConfigFile = new File(this.getClass().getResource(getResourceName()).toURI());
        //Create target config file in order to perform marshall operation
        targetConfigFile = new File(sourceConfigFile.getParent(), "target_" + getResourceName());
        FileUtils.copyFile(sourceConfigFile, targetConfigFile);
    }

    protected abstract String getResourceName();

    protected XmlFile getSourceConfigFile(XStream XSTREAM) {
        return new XmlFile(XSTREAM, sourceConfigFile);
    }

    protected XmlFile getTargetConfigFile(XStream XSTREAM) {
        return new XmlFile(XSTREAM, targetConfigFile);
    }

}

