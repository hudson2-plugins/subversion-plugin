/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Anton Kozak, Nikita Levyankov
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static junit.framework.Assert.assertEquals;

/**
 * Verifies the equals and hashcode methods of subclasses of {@link WorkspaceUpdater}
 * <p/>
 * Date: 10/5/2011
 *
 * @author Anton Kozak, Nikita Levyankov
 */
@RunWith(Parameterized.class)
public class WorkspaceUpdaterEqualsHashCodeTest {

    private WorkspaceUpdater defaultUpdater = new CheckoutUpdater();
    private WorkspaceUpdater updater;
    private boolean expectedResult;

    public WorkspaceUpdaterEqualsHashCodeTest(WorkspaceUpdater updater, boolean expectedResult) {
        this.updater = updater;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection generateData() {
        return Arrays.asList(new Object[][]{
            {new CheckoutUpdater(), true},
            {new CheckoutWithLocationFoldersCleanupUpdater(), false},
            {new UpdateUpdater(), false},
            {new UpdateWithCleanUpdater(), false},
            {new UpdateWithRevertUpdater(), false}
        });
    }

    @Test
    public void testEquals() {
        assertEquals(expectedResult, defaultUpdater.equals(updater));
    }

    @Test
    public void testHashCode() {
        assertEquals(expectedResult, defaultUpdater.hashCode() == updater.hashCode());
    }
}
