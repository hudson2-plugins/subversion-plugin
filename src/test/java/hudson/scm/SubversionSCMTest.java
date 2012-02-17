/*
  * The MIT License
  *
  * Copyright (c) 2011, Oracle Corporation
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

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.Bug;

import static junit.framework.Assert.assertEquals;


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
        assertEquals(expectedResult, SubversionSCM.DescriptorImpl.validateExcludedUser(userName));
    }
}
