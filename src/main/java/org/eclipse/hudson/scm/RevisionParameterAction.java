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
 * Tom Huybrechts, Yahoo! Inc.
 *
 *******************************************************************************/
package org.eclipse.hudson.scm;

import hudson.model.InvisibleAction;
import hudson.model.Action;
import hudson.model.Queue;
import hudson.model.Queue.Task;
import hudson.model.queue.FoldableAction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Action containing a list of SVN revisions that should be checked out. Used for parameterized builds.
 * 
 * @author Tom Huybrechts
 */
public class RevisionParameterAction extends InvisibleAction implements Serializable, FoldableAction {
	
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(RevisionParameterAction.class.getName());
	private final List<SubversionSCM.SvnInfo> revisions;

	public RevisionParameterAction(List<SubversionSCM.SvnInfo> revisions) {
		super();
		this.revisions = revisions;
	}
	
	public RevisionParameterAction(RevisionParameterAction action) {
		super();
		this.revisions = new ArrayList<SubversionSCM.SvnInfo>(action.revisions);
	}
	
	public RevisionParameterAction(SubversionSCM.SvnInfo... revisions) {
		this.revisions = new ArrayList<SubversionSCM.SvnInfo>(Arrays.asList(revisions));
	}

	public List<SubversionSCM.SvnInfo> getRevisions() {
		return revisions;
	}
	
	public SVNRevision getRevision(String url) {
		for (SubversionSCM.SvnInfo revision: revisions) {
			if (revision.url.equals(url)) {
				return SVNRevision.create(revision.revision);
			}
		}
		return null;
	}
		
	public void foldIntoExisting(Queue.Item item, Task owner, List<Action> otherActions) {
		RevisionParameterAction existing = item.getAction(RevisionParameterAction.class);
		if (existing!=null) {
		    existing.mergeRevisions(this.revisions);
		    return;
		}
		// no RevisionParameterAction found, so add a copy of this one
		item.getActions().add(new RevisionParameterAction(this));
	}
	
	private void mergeRevisions(List<SubversionSCM.SvnInfo> newRevisions) {
		
		for (SubversionSCM.SvnInfo newRev : newRevisions) {
			boolean found = false;
			for (SubversionSCM.SvnInfo oldRev : this.revisions) {
				if (oldRev.url.equals(newRev.url)) {

					LOGGER.info("Updating revision parameter for " + oldRev.url + " from " + oldRev.revision + " to " + newRev.revision);

					this.revisions.add(new SubversionSCM.SvnInfo(oldRev.url, newRev.revision));
					this.revisions.remove(oldRev);
					found = true;
					break;
				}
			}
			if (!found) {
				this.revisions.add(newRev);
			}
		}
	}

	@Override
	public String toString() {
		String result = "[RevisionParameterAction ";
		for(SubversionSCM.SvnInfo i : revisions) {
			result += i.url + "(" + i.revision + ") ";
		}
		return result + "]";
	}
}
