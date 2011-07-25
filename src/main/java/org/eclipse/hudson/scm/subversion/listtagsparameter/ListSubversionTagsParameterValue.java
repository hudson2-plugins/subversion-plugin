/*******************************************************************************
 *
 * Copyright (c) 2010 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Manufacture Francaise des Pneumatiques Michelin, Romain Seguy
 *
 *******************************************************************************/
package org.eclipse.hudson.scm.subversion.listtagsparameter;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.util.VariableResolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

/**
 * This class represents the actual {@link ParameterValue} for the
 * {@link ListSubversionTagsParameterDefinition} parameter.
 *
 * @author Romain Seguy (http://openromain.blogspot.com)
 */
public class ListSubversionTagsParameterValue extends ParameterValue {

    @Exported(visibility = 3)
    private String tagsDir; // this att comes from ListSubversionTagsParameterDefinition
    @Exported(visibility = 3)
    private String tag;

    @DataBoundConstructor
    public ListSubversionTagsParameterValue(String name, String tagsDir, String tag) {
        super(name);
        this.tagsDir = tagsDir;
        this.tag = tag;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        env.put(getName(), getTag());
    }

    @Override
    public VariableResolver<String> createVariableResolver(AbstractBuild<?, ?> build) {
        return new VariableResolver<String>() {
            public String resolve(String name) {
                return ListSubversionTagsParameterValue.this.name.equals(name) ? getTag() : null;
            }
        };
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTagsDir() {
        return tagsDir;
    }

    public void setTagsDir(String tagsDir) {
        this.tagsDir = tagsDir;
    }

}
