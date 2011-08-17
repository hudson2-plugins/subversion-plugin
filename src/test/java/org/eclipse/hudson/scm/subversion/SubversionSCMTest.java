/*******************************************************************************
 *
 * Copyright (c) 2004-2010 Oracle Corporation.
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

import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.Bug;


/**
 * Test for {@link SubversionSCM}
 * </p>
 * Date: 7/21/11
 *
 * @author Anton Kozak
 */
@RunWith(Parameterized.class)
public class SubversionSCMTest {

    private String userName;
    private boolean expectedResult;

    public SubversionSCMTest(String userName, boolean expectedResult) {
        this.userName = userName;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection generateData() {
        return Arrays.asList(new Object[][]{
            {"user", true}, {"user-123", true}, {"user_123", true}, {"domain\\user-123", true},
            {"user\\", false}, {"user[]", false}
        });
    }

    @Bug(8700)
    @Test
    public void testValidateExcludedUsers() {
        Assert.assertEquals(expectedResult, SubversionSCM.DescriptorImpl.validateExcludedUser(userName));
    }
}
