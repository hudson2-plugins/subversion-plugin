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
package hudson.scm;

import hudson.scm.browsers.CollabNetSVN;
import hudson.scm.browsers.WebSVN;
import hudson.scm.subversion.CheckoutUpdater;
import hudson.scm.subversion.UpdateUpdater;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static junit.framework.Assert.assertEquals;

/**
 * Verifies the equals and hashcode methods of {@link SubversionSCM}
 * <p/>
 * Date: 10/5/2011
 *
 * @author Anton Kozak, Nikita Levyankov
 */
@RunWith(Parameterized.class)
public class SubversionSCMEqualsHashCodeTest {

    private SubversionSCM defaultSubversionSCM;

    private SubversionSCM subversionSCM;
    private boolean expectedResult;

    public SubversionSCMEqualsHashCodeTest(SubversionSCM subversionSCM, boolean expectedResult) {
        this.subversionSCM = subversionSCM;
        this.expectedResult = expectedResult;
    }

    @Before
    public void setUp() throws MalformedURLException {
        List<SubversionSCM.ModuleLocation> locations = SubversionSCM.ModuleLocation.parse(
            new String[]{"url1", "url2"}, new String[]{"subdir1", "subdir2"},
            new String[]{null, null}, new boolean[]{true, false});
        defaultSubversionSCM = new SubversionSCM(locations, new CheckoutUpdater(),
            new CollabNetSVN(new URL("http://svn.com")), "excludedRegions", "excludedUsers", "excludedRevprop",
            "excludedCommit", "excludedRegions");

    }

    @Parameterized.Parameters
    public static Collection generateData() throws MalformedURLException {
        List<SubversionSCM.ModuleLocation> locations = SubversionSCM.ModuleLocation.parse(
            new String[]{"url1", "url2"}, new String[]{"subdir1", "subdir2"},
            new String[]{null, null}, new boolean[]{true, false});
        List<SubversionSCM.ModuleLocation> locations1 = SubversionSCM.ModuleLocation.parse(
            new String[]{"url2", "url1"}, new String[]{"subdir2", "subdir1"},
            new String[]{null, null}, new boolean[]{false, true});
        List<SubversionSCM.ModuleLocation> locations2 = SubversionSCM.ModuleLocation.parse(
            new String[]{"url", "url1"}, new String[]{"subdir", "subdir1"},
            new String[]{null, null}, new boolean[]{false, true});

        CheckoutUpdater u = new CheckoutUpdater();
        SubversionRepositoryBrowser br = new CollabNetSVN(new URL("http://svn.com"));

        return Arrays.asList(new Object[][]{
            {new SubversionSCM(locations, u, br, "excludedRegions", "excludedUsers", "excludedRevprop",
                "excludedCommit", "excludedRegions"), true},
            {new SubversionSCM(locations1, u, br, "excludedRegions", "excludedUsers", "excludedRevprop",
                "excludedCommit", "excludedRegions"), true},
            {new SubversionSCM(locations2, u, br, "excludedRegions", "excludedUsers", "excludedRevprop",
                "excludedCommit", "excludedRegions"), false},
            {new SubversionSCM(locations, new UpdateUpdater(), br, "excludedRegions", "excludedUsers",
                "excludedRevprop", "excludedCommit", "excludedRegions"), false},
            {new SubversionSCM(locations, null, br, "excludedRegions", "excludedUsers", "excludedRevprop",
                "excludedCommit", "excludedRegions"), false},
            {new SubversionSCM(locations, u, new WebSVN(new URL("http://websvn.com")), "excludedRegions",
                "excludedUsers", "excludedRevprop", "excludedCommit", "excludedRegions"), false},
            {new SubversionSCM(locations, u, null, "excludedRegions", "excludedUsers", "excludedRevprop",
                "excludedCommit", "excludedRegions"), false},
            {new SubversionSCM(locations, u, br, "excludedRegions1", "excludedUsers", "excludedRevprop",
                "excludedCommit", "excludedRegions"), false},
            {new SubversionSCM(locations, u, br, null, "excludedUsers", "excludedRevprop", "excludedCommit",
                "excludedRegions"), false},
            {new SubversionSCM(locations, u, br, "excludedRegions", "excludedUsers1", "excludedRevprop",
                "excludedCommit", "excludedRegions"), false},
            {new SubversionSCM(locations, u, br, "excludedRegions", null, "excludedRevprop", "excludedCommit",
                "excludedRegions"), false},
            {new SubversionSCM(locations, u, br, "excludedRegions", "excludedUsers", "excludedRevprop1",
                "excludedCommit", "excludedRegions"), false},
            {new SubversionSCM(locations, u, br, "excludedRegions", "excludedUsers", null, "excludedCommit",
                "excludedRegions"), false},
            {new SubversionSCM(locations, u, br, "excludedRegions", "excludedUsers", "excludedRevprop",
                "excludedCommit1", "excludedRegions"), false},
            {new SubversionSCM(locations, u, br, "excludedRegions", "excludedUsers", "excludedRevprop", null,
                "excludedRegions"), false},
            {new SubversionSCM(locations, u, br, "excludedRegions", "excludedUsers", "excludedRevprop",
                "excludedCommit", "excludedRegions1"), false},
            {new SubversionSCM(locations, u, br, "excludedRegions", "excludedUsers", "excludedRevprop",
                "excludedCommit", null), false}
        });
    }

    @Test
    public void testEquals() {
        assertEquals(expectedResult, defaultSubversionSCM.equals(subversionSCM));
    }

    @Test
    public void testHashCode() throws MalformedURLException {
        assertEquals(expectedResult, defaultSubversionSCM.hashCode() == subversionSCM.hashCode());
    }

}
