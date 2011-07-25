/*******************************************************************************
 *
 * Copyright (c) 2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Manufacture Francaise des Pneumatiques Michelin, Romain Seguy
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion.listtagsparameter;

import hudson.Util;
import java.util.ArrayList;
import java.util.List;
import org.tmatesoft.svn.core.ISVNDirEntryHandler;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;

/**
 * Simple {@link ISVNDirEntryHandler} used to get a list containing all the
 * directories in a given Subversion repository.
 *
 * @author Romain Seguy (http://openromain.blogspot.com)
 */
public class SimpleSVNDirEntryHandler implements ISVNDirEntryHandler {

  private List<String> dirs = new ArrayList<String>();

  public List<String> getDirs() {
    return dirs;
  }

  @Override
  public void handleDirEntry(SVNDirEntry dirEntry) throws SVNException {
    dirs.add(Util.removeTrailingSlash(dirEntry.getName()));
  }

}
