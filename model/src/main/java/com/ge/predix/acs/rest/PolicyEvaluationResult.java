/*******************************************************************************
 * Copyright 2016 General Electric Company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ge.predix.acs.rest;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.model.Effect;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 *
 * @author 212304931
 *
 */
@SuppressWarnings({ "javadoc", "nls" })
@ApiModel(description = "Policy evaluation result")
public class PolicyEvaluationResult {

    @ApiModelProperty(value = "The effect of the policy evaluation", required = true)
    private Effect effect;

    @ApiModelProperty(value = "The collection of the subject's attributes", required = false)
    private Set<Attribute> subjectAttributes = Collections.emptySet();

    @ApiModelProperty(value = "The collection of the resource's attributes", required = false)
    private List<Attribute> resourceAttributes = Collections.emptyList();

    @ApiModelProperty(
            value = "The resources that matched the policy evaluation request's resource identifier based on "
                    + "the attribute uri templates defined in the policy set. For example, a policy request of"
                    + "    /v1/site/1/plant/asset/1\n" + "against a policy set with attribute uri templates:\n"
                    + "    /v1{attribute_uri}/plant/asset/{asset_id}\n"
                    + "    /v1/site/{site_id}/plant/asset/{asset_id}\n" + "would include:\n" + "    /site/1\n"
                    + "    /asset/2\n" + "in this set.",
            required = false)
    private Set<String> resolvedResourceUris;

    private long timestamp;

    private String message;

    public PolicyEvaluationResult() {
        this.effect = Effect.NOT_APPLICABLE;
    }

    public PolicyEvaluationResult(final Effect decision) {
        this.effect = decision;
    }

    public PolicyEvaluationResult(final Effect effect, final Set<Attribute> subjectAttributes,
            final List<Attribute> resourceAttributes) {
        this.effect = effect;
        this.subjectAttributes = subjectAttributes;
        this.resourceAttributes = resourceAttributes;
    }

    public PolicyEvaluationResult(final Effect effect, final Set<Attribute> subjectAttributes,
            final List<Attribute> resourceAttributes, final Set<String> resolvedResourceUris) {
        this.effect = effect;
        this.subjectAttributes = subjectAttributes;
        this.resourceAttributes = resourceAttributes;
        this.resolvedResourceUris = resolvedResourceUris;
    }

    public Effect getEffect() {
        return this.effect;
    }

    public void setEffect(final Effect effect) {
        this.effect = effect;
    }

    public Set<Attribute> getSubjectAttributes() {
        return this.subjectAttributes;
    }

    public void setSubjectAttributes(final Set<Attribute> subjectAttributes) {
        this.subjectAttributes = subjectAttributes;
    }

    public List<Attribute> getResourceAttributes() {
        return this.resourceAttributes;
    }

    public void setResourceAttributes(final List<Attribute> resourceAttributes) {
        this.resourceAttributes = resourceAttributes;
    }

    public Set<String> getResolvedResourceUris() {
        return this.resolvedResourceUris;
    }

    public void setResolvedResourceUris(final Set<String> resolvedResourceUris) {
        this.resolvedResourceUris = resolvedResourceUris;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PolicyEvaluationResult [effect=" + this.effect + ", subjectAttributes=" + this.subjectAttributes
                + ", resourceAttributes=" + this.resourceAttributes + "]";
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }
}
