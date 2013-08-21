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

import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.User;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Future;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.hudson.test.Bug;
import org.kohsuke.stapler.export.Exported;

import static junit.framework.Assert.assertEquals;


/**
 * Test for {@link SubversionSCM}
 * </p>
 * Date: 7/21/11
 *
 * @author Anton Kozak
 */
@RunWith(Parameterized.class)
public class SubversionSCMTest extends AbstractSubversionTest{

    private String userName;
    private boolean expectedResult;

    public SubversionSCMTest(String userName, boolean expectedResult) {
        this.userName = userName;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection generateData() {
        return Arrays.asList(new Object[][]{
            {"user", Boolean.TRUE}, {"user-123", Boolean.TRUE}, {"user_123", Boolean.TRUE}, {"domain\\user-123", Boolean.TRUE},
            {"user\\", Boolean.FALSE}, {"user[]", Boolean.FALSE}
        });
    }

    @Bug(8700)
    @Test
    public void testValidateExcludedUsers() {
        assertEquals(expectedResult, SubversionSCM.DescriptorImpl.validateExcludedUser(userName));
    }
}
