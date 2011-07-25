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
import java.io.IOException;
import org.eclipse.hudson.scm.SubversionSCM;

/**
 * Plugin entry point.
 *
 * @author Anton Kozak
 */
public class PluginImpl extends Plugin {

    @Override
    public void start() throws IOException {
        SubversionSCM.initialize();
    }
}