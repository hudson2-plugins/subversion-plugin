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
 * Kohsuke Kawaguchi
 *
 *******************************************************************************/
package org.eclipse.hudson.scm;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogParser;
import hudson.util.Digester2;
import hudson.util.IOException2;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * {@link hudson.scm.ChangeLogParser} for Subversion.
 *
 * @author Kohsuke Kawaguchi
 */
public class SubversionChangeLogParser extends ChangeLogParser {
    public SubversionChangeLogSet parse(AbstractBuild build, File changelogFile) throws IOException, SAXException {
        // http://svn.collab.net/repos/svn/trunk/subversion/svn/schema/

        Digester digester = new Digester2();
        ArrayList<SubversionChangeLogSet.LogEntry> r = new ArrayList<SubversionChangeLogSet.LogEntry>();
        digester.push(r);

        digester.addObjectCreate("*/logentry", SubversionChangeLogSet.LogEntry.class);
        digester.addSetProperties("*/logentry");
        digester.addBeanPropertySetter("*/logentry/author","user");
        digester.addBeanPropertySetter("*/logentry/date");
        digester.addBeanPropertySetter("*/logentry/msg");
        digester.addSetNext("*/logentry","add");

        digester.addObjectCreate("*/logentry/paths/path", SubversionChangeLogSet.Path.class);
        digester.addSetProperties("*/logentry/paths/path");
        digester.addBeanPropertySetter("*/logentry/paths/path","value");
        digester.addSetNext("*/logentry/paths/path","addPath");

        try {
            digester.parse(changelogFile);
        } catch (IOException e) {
            throw new IOException2("Failed to parse "+changelogFile,e);
        } catch (SAXException e) {
            throw new IOException2("Failed to parse "+changelogFile,e);
        }

        return new SubversionChangeLogSet(build,r);
    }

}
