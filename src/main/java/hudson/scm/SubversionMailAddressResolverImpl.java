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

import hudson.tasks.MailAddressResolver;
import hudson.Extension;
import hudson.model.User;
import hudson.model.AbstractProject;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * {@link MailAddressResolver} that checks for well-known repositories that and computes user e-mail address.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class SubversionMailAddressResolverImpl extends MailAddressResolver {
    public String findMailAddressFor(User u) {
        for (AbstractProject<?,?> p : u.getProjects()) {
            SCM scm = p.getScm();
            if (scm instanceof SubversionSCM) {
                SubversionSCM svn = (SubversionSCM) scm;
                for (SubversionSCM.ModuleLocation loc : svn.getLocations(p.getLastBuild())) {
                    String s = findMailAddressFor(u,loc.remote);
                    if(s!=null) return s;
                }
            }
        }

        // didn't hit any known rules
        return null;
    }

    /**
     *
     * @param scm
     *      String that represents SCM connectivity.
     */
    protected String findMailAddressFor(User u, String scm) {
        for (Map.Entry<Pattern, String> e : RULE_TABLE.entrySet())
            if(e.getKey().matcher(scm).matches())
                return u.getId()+e.getValue();
        return null;
    }

    private static final Map<Pattern,String/*suffix*/> RULE_TABLE = new HashMap<Pattern, String>();

    static {
        {// java.net
            Pattern svnurl = Pattern.compile("https://[^.]+.dev.java.net/svn/([^/]+)(/.*)?");
            RULE_TABLE.put(svnurl,"@dev.java.net");
        }

        {// source forge
            Pattern svnUrl = Pattern.compile("(http|https)://[^.]+.svn.(sourceforge|sf).net/svnroot/([^/]+)(/.*)?");

            RULE_TABLE.put(svnUrl,"@users.sourceforge.net");
        }

        // TODO: read some file under $HUDSON_HOME?
    }
}
