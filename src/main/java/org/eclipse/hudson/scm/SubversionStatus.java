/*******************************************************************************
 *
 * Copyright (c) 2004-2011 Oracle Corporation.
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

import hudson.Extension;
import hudson.model.AbstractModelObject;
import hudson.model.RootAction;

import java.util.regex.Pattern;
import java.util.UUID;

/**
 * Information screen for the use of Subversion in Hudson.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class SubversionStatus extends AbstractModelObject implements RootAction {
    public String getDisplayName() {
        return "Subversion";
    }

    public String getSearchUrl() {
        return getUrlName();
    }

    public String getIconFileName() {
        // TODO
        return null;
    }

    public String getUrlName() {
        return "subversion";
    }

    public SubversionRepositoryStatus getDynamic(String uuid) {
        if(UUID_PATTERN.matcher(uuid).matches())
            return new SubversionRepositoryStatus(UUID.fromString(uuid));
        return null;
    }

    private static final Pattern UUID_PATTERN = Pattern.compile("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");
}
