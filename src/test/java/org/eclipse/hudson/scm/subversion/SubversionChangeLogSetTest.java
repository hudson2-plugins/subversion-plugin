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
package org.eclipse.hudson.scm.subversion;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.hudson.scm.subversion.SubversionChangeLogSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * SubversionChangeLogSet TestCase
 * <p/>
 * Date: 6/28/11
 *
 * @author Nikita Levyankov
 */
public class SubversionChangeLogSetTest {

    @Test
    public void testRemoveDuplicateEntries() throws Exception{
        //One two duplicated entries. Total 8
        List<SubversionChangeLogSet.LogEntry> items = new ArrayList<SubversionChangeLogSet.LogEntry>();
        items.add(buildChangeLogEntry(1, "Test msg"));
        items.add(buildChangeLogEntry(2, "Test msg"));
        items.add(buildChangeLogEntry(1, "Test msg"));
        items.add(buildChangeLogEntry(3, "Test msg"));
        items.add(buildChangeLogEntry(4, "Test msg"));
        items.add(buildChangeLogEntry(5, "Test msg"));
        items.add(buildChangeLogEntry(6, "Test msg"));
        items.add(buildChangeLogEntry(1, "Test msg1"));
        Assert.assertEquals("Items size is not equals to expected", items.size(), 8);
        List<SubversionChangeLogSet.LogEntry> resultItems = SubversionChangeLogSet.removeDuplicatedEntries(items);
        Assert.assertFalse(resultItems.size() == items.size());
        Assert.assertEquals(resultItems.size(), 7);

        //Duplicated entries are absent. Total 7
        items = new ArrayList<SubversionChangeLogSet.LogEntry>();
        items.add(buildChangeLogEntry(1, "Test msg"));
        items.add(buildChangeLogEntry(2, "Test msg"));
        items.add(buildChangeLogEntry(3, "Test msg"));
        items.add(buildChangeLogEntry(4, "Test msg"));
        items.add(buildChangeLogEntry(5, "Test msg"));
        items.add(buildChangeLogEntry(6, "Test msg"));
        items.add(buildChangeLogEntry(1, "Test msg1"));
        Assert.assertEquals("Items size is not equals to expected", items.size(), 7);
        resultItems = SubversionChangeLogSet.removeDuplicatedEntries(items);
        Assert.assertTrue(resultItems.size() == items.size());
        Assert.assertEquals(resultItems.size(), 7);
    }


    private SubversionChangeLogSet.LogEntry buildChangeLogEntry(int revision, String msg) {
        SubversionChangeLogSet.LogEntry entry = new SubversionChangeLogSet.LogEntry();
        entry.setRevision(revision);
        entry.setMsg(msg);
        return entry;
    }
}
