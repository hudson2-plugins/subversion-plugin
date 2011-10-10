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
import hudson.scm.browsers.FishEyeSVN;
import hudson.scm.browsers.Sventon;
import hudson.scm.browsers.Sventon2;
import hudson.scm.browsers.WebSVN;
import hudson.scm.browsers.WebSVN2;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static junit.framework.Assert.assertEquals;

/**
 * Verifies equals and hashcode of subcalsses of {@link SubversionRepositoryBrowser}.
 * <p/>
 * Date: 10/5/2011
 *
 * @author Anton Kozak, Nikita Levyankov
 */
@RunWith(Parameterized.class)
public class SubversionBrowserEqualsHashCodeTest {

    private SubversionRepositoryBrowser defaultBrowser;
    private SubversionRepositoryBrowser browser;

    private boolean expectedResult;

    public SubversionBrowserEqualsHashCodeTest(SubversionRepositoryBrowser browser, boolean expectedResult) {
        this.browser = browser;
        this.expectedResult = expectedResult;
    }

    @Before
    public void setUp() throws MalformedURLException {
        defaultBrowser = new WebSVN(new URL("http://websvn.com"));
    }

    @Parameterized.Parameters
    public static Collection generateData() throws MalformedURLException {
        return Arrays.asList(new Object[][]{
            {new WebSVN(new URL("http://websvn.com")), true},
            {new WebSVN2(new URL("http://websvn2.com")), false},
            {new CollabNetSVN(new URL("http://collabnetsvn.com")), false},
            {new FishEyeSVN(new URL("http://fisheyesvn.com"), "module"), false},
            {new Sventon(new URL("http://sventon.com"), "instance"), false},
            {new Sventon2(new URL("http://sventon2.com"), "instance"), false}
        });
    }

    @Test
    public void testEquals() throws MalformedURLException {
        assertEquals(expectedResult, defaultBrowser.equals(browser));
    }

    @Test
    public void testHashCode() throws MalformedURLException {
        assertEquals(expectedResult, defaultBrowser.hashCode() == browser.hashCode());
    }
}
