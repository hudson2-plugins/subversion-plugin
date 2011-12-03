/*
 * The MIT License
 *
 * Copyright (c) 2011, Oracle Corporation, Anton Kozak, Nikita Levyankov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
