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
 * Anton Kozak, Nikita Levyankov
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion;

import org.eclipse.hudson.scm.subversion.browsers.CollabNetSVN;
import org.eclipse.hudson.scm.subversion.browsers.FishEyeSVN;
import org.eclipse.hudson.scm.subversion.browsers.Sventon;
import org.eclipse.hudson.scm.subversion.browsers.Sventon2;
import org.eclipse.hudson.scm.subversion.browsers.WebSVN;
import org.eclipse.hudson.scm.subversion.browsers.WebSVN2;
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
 * Verifies equals and hashcode of {@link SubversionRepositoryBrowser}.
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
