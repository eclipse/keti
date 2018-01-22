/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.eclipse.keti.acs.model;

import java.util.Collections;
import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * @author Sebastian Torres Brown
 * 
 *         Keti obligations model defined by XACML 3.O
 *
 */
@ApiModel(
        description = "Obligation Expressions are intended to allow a policy writer to add "
                + "dynamic expressions in to the obligation statements. When a policy is "
                + "evaluated, any obligation expression included in that policy is evaluated "
                + "to an obligation by resolving arguments of the action defined by the obligation "
                + "expression. Resolved obligations are then passed by the the policy engine (PDP) "
                + "back to policy enforcement point (PEP) as part of policy evaluation result.")
public class ObligationExpression {

    private String id;

    private ObligationType type = ObligationType.CUSTOM;

    private Object actionTemplate;

    private List<ActionArgument> actionArguments = Collections.emptyList();

    /**
     * @return the id
     */
    @ApiModelProperty(value = "id to uniquely identify obligation expression", required = true)
    public String getId() {
        return this.id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * @return the type
     */
    public ObligationType getType() {
        return this.type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(final ObligationType type) {
        this.type = type;
    }

    /**
     * @return the actionTemplate
     */
    @ApiModelProperty(value = "An arbitrary JSON that represents a template for obligation action", required = true)
    public Object getActionTemplate() {
        return this.actionTemplate;
    }

    /**
     * @param actionTemplate
     *            the actionTemplate to set
     */
    public void setActionTemplate(final Object actionTemplate) {
        this.actionTemplate = actionTemplate;
    }

    /**
     * @return the attributes
     */
    public List<ActionArgument> getActionArguments() {
        return this.actionArguments;
    }

    /**
     * @param attributes
     *            the attributes to set
     */
    public void setActionArguments(final List<ActionArgument> actionArguments) {
        this.actionArguments = actionArguments;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.actionArguments == null) ? 0 : this.actionArguments.hashCode());
        result = prime * result + ((this.actionTemplate == null) ? 0 : this.actionTemplate.hashCode());
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ObligationExpression other = (ObligationExpression) obj;
        if (this.actionArguments == null) {
            if (other.actionArguments != null) {
                return false;
            }
        } else if (!this.actionArguments.equals(other.actionArguments)) {
            return false;
        }
        if (this.actionTemplate == null) {
            if (other.actionTemplate != null) {
                return false;
            }
        } else if (!this.actionTemplate.equals(other.actionTemplate)) {
            return false;
        }
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!this.id.equals(other.id)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }
}
