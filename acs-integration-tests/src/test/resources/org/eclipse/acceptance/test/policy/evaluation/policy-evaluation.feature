Feature: PolicyEvaluation
  Policy evaluation feature allows the users of ACS to get access decisions based on policy evaluation. This feature expects
  policy to be defined. Optionally user can setup attributes which are used during policy evaluation. As a part of this call, 
  user can supply subject, resource and action which are mandatory fields. They can also supply subject attributes which are 
  optional - when supplied they should be merged with subject attributes that are provisioned in ACS and should get used during
  policy evaluation.

  Scenario: policy evaluation request which returns PERMIT
    Given A policy set that allows access to all
    When Any evaluation request
    Then policy evaluation returns PERMIT

  Scenario: policy evaluation request which returns DENY
    Given A policy set that allows access to none
    When Any evaluation request
    Then policy evaluation returns DENY

  Scenario: policy evaluation request which returns PERMIT based on role using condition
    Given A policy set that allows access only to subject with role administrator using condition
    When Evaluation request which has the subject attribute role with the value administrator
    Then policy evaluation returns PERMIT

  Scenario: policy evaluation request which returns DENY based on role using condition
    Given A policy set that allows access only to subject with role administrator using condition
    When Evaluation request which has subject attribute which are null
    Then policy evaluation returns DENY

  Scenario: policy evaluation request which returns PERMIT based on role using matcher
    Given A policy set that allows access only to subject with role administrator using matcher
    When Evaluation request which has the subject attribute role with the value administrator
    Then policy evaluation returns PERMIT
    And policy evaluation response includes subject attribute role with the value administrator
    
  Scenario: policy evaluation request which returns PERMIT based on ACS subject attributes 
    Given A policy set that allows access only to subject with role administrator using matcher
    And ACS has subject attribute role with value administrator for the subject
    When Evaluation request which has no subject attribute
    Then policy evaluation returns PERMIT
    And policy evaluation response includes subject attribute role with the value administrator
    
  Scenario: policy evaluation request which returns PERMIT based union of ACS subject attributes and evaluation request attributes
    Given A policy set that allows access only to subject with role administrator using matcher
    And ACS has subject attribute role with value analyst for the subject
    When Evaluation request which has the subject attribute role with the value administrator
    Then policy evaluation returns PERMIT
    And policy evaluation response includes subject attribute role with the value administrator
    And policy evaluation response includes subject attribute role with the value analyst
    
  Scenario: policy evaluation request which returns PERMIT based on ACS subject attributes and evaluation request attributes
    Given A policy set that allows access only to subject with role administrator and site sanramon
    And ACS has subject attribute site with value sanramon for the subject
    When Evaluation request for resource /site/sanramon which has the subject attribute role with the value administrator
    Then policy evaluation returns PERMIT
    And policy evaluation response includes subject attribute role with the value administrator
    And policy evaluation response includes subject attribute site with the value sanramon 
