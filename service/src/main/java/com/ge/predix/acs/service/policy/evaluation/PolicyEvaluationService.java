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
package com.ge.predix.acs.service.policy.evaluation;

import java.util.Set;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.rest.PolicyEvaluationRequestV1;
import com.ge.predix.acs.rest.PolicyEvaluationResult;

public interface PolicyEvaluationService {
    PolicyEvaluationResult evalPolicy(PolicyEvaluationRequestV1 request);

    PolicyEvaluationResult evalPolicy(final String uri, final String subjectIdentifier, final String action,
            final Set<Attribute> supplementalResourceAttributes, final Set<Attribute> supplementalSubjectAttributes);
}
