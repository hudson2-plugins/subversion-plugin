/*
 * The MIT License
 *
 * Copyright (c) 2011, Oracle Corporation, Winston Prakash, Nikita Levyankov, Anton Kozak
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

import java.io.Serializable;
import java.util.Map;

/**
 * {@link SCMRevisionState} for {@link SubversionSCM}. {@link Serializable} since we compute
 * this remote.
 */
final class SVNRevisionState extends SCMRevisionState implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * All the remote locations that we checked out. This includes those that are specified
     * explicitly via {@link SubversionSCM#getLocations()} as well as those that
     * are implicitly pulled in via svn:externals, but it excludes those locations that
     * are added via svn:externals in a way that fixes revisions.
     */
    final Map<String,Long> revisions;

    SVNRevisionState(Map<String, Long> revisions) {
        this.revisions = revisions;
    }

}
