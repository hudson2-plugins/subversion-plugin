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
 * Verifies equals and hashcode of {@link SubversionSCM.ModuleLocation}.
 * <p/>
 * Date: 10/5/2011
 *
 * @author Anton Kozak, Nikita Levyankov
 */
@RunWith(Parameterized.class)
public class ModuleLocationEqualsHashCodeTest {

    private SubversionSCM.ModuleLocation defaultLocation =
        new SubversionSCM.ModuleLocation("url", "local", "options", false);
    private SubversionSCM.ModuleLocation location;
    private boolean expectedResult;

    public ModuleLocationEqualsHashCodeTest(SubversionSCM.ModuleLocation location, boolean expectedResult) {
        this.location = location;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection generateData() {
        return Arrays.asList(new Object[][]{
            {new SubversionSCM.ModuleLocation("url", "local", "options", false), true},
            {new SubversionSCM.ModuleLocation("url1", "local", "options", false), false},
            {new SubversionSCM.ModuleLocation(null, "local1", "options", false), false},
            {new SubversionSCM.ModuleLocation("url", null, "options", false), false},
            {new SubversionSCM.ModuleLocation("url", "local", "options1", false), false},
            {new SubversionSCM.ModuleLocation("url", "local", null, false), false},
            {new SubversionSCM.ModuleLocation("url", "local", "options", true), false}
        });
    }

    @Test
    public void testEquals() {
        assertEquals(expectedResult, defaultLocation.equals(location));
    }

    @Test
    public void testHashCode() {
        assertEquals(expectedResult, defaultLocation.hashCode() == location.hashCode());
    }
}
