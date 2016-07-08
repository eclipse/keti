package com.ge.predix.acs.service.policy.evaluation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ge.predix.acs.model.Attribute;
import com.ge.predix.acs.privilege.management.PrivilegeManagementService;
import com.ge.predix.acs.rest.BaseSubject;

public class SubjectAttributeResolver {

    private final Map<String, Set<Attribute>> subjectAttributeMap = new HashMap<>();
    private final PrivilegeManagementService privilegeService;
    private final String subjectIdentifier;
    private final Set<Attribute> supplementalSubjectAttributes;

    public SubjectAttributeResolver(final PrivilegeManagementService privilegeService, final String subjectIdentifier,
            final Set<Attribute> supplementalSubjectAttributes) {
        this.privilegeService = privilegeService;
        this.subjectIdentifier = subjectIdentifier;
        if (null == supplementalSubjectAttributes) {
            this.supplementalSubjectAttributes = Collections.emptySet();
        } else {
            this.supplementalSubjectAttributes = supplementalSubjectAttributes;
        }
    }

    public Set<Attribute> getResult(final Set<Attribute> scopes) {
        Set<Attribute> subjectAttributes = this.subjectAttributeMap.get(this.subjectIdentifier);
        if (null == subjectAttributes) {
            subjectAttributes = new HashSet<>();
            BaseSubject subject = this.privilegeService
                    .getBySubjectIdentifierAndScopes(this.subjectIdentifier, scopes);
            if (null != subject) {
                subjectAttributes.addAll(subject.getAttributes());
            }
            subjectAttributes.addAll(this.supplementalSubjectAttributes);
            this.subjectAttributeMap.put(this.subjectIdentifier, subjectAttributes);
        }
        return subjectAttributes;
    }

}
