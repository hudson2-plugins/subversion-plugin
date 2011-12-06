/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Patrick van Dissel (id:pvdissel).
 *
 *
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion.listtagsparameter;

import java.util.List;
import org.tmatesoft.svn.core.ISVNDirEntryHandler;

/**
 * {@link ISVNDirEntryHandler} with the possibility to get the handled directory names.
 */
public interface DirectoriesSvnEntryHandler extends ISVNDirEntryHandler {

    /**
     * Get the handled Directory names.
     */
    List<String> getDirectoryNames();
}
