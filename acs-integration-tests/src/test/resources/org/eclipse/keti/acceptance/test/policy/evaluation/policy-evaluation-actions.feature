Feature: PolicyEvaluationActions

Scenario: Policy Evaluation request and no action defined in ACS policy
    Given an existing policy with no defined action
    When A policy evaluation is requested with any HTTP action
    Then policy evaluation returns PERMIT

Scenario: Policy Evaluation request with action matching one of the actions defined in ACS policy
	Given an existing policy set stored in ACS with multiple actions
    When A policy evaluation is requested with an HTTP action matching one of the policies in the action list
    Then policy evaluation returns PERMIT
    
Scenario: Policy Evaluation request with action matching the action defined in ACS policy
	Given an existing policy set stored in ACS with a single action
    When A policy evaluation is requested with an HTTP action matching the policy action
    Then policy evaluation returns PERMIT

Scenario: Policy Evaluation request with action NOT matching any of the actions defined in ACS policy
    Given an existing policy set stored in ACS with multiple actions
    When A policy evaluation is requested with an HTTP action not matching one of the policies in the action list
    Then policy evaluation returns NOT_APPLICABLE
   
Scenario: Policy Evaluation request with action NOT matching the action defined in ACS policy
	Given an existing policy set stored in ACS with a single action
    When A policy evaluation is requested with an HTTP action not matching the policy action
    Then policy evaluation returns NOT_APPLICABLE

Scenario: Policy Evaluation request and empty action defined in ACS policy
    Given an existing policy with empty defined action
    When A policy evaluation is requested with any HTTP action
    Then policy evaluation returns PERMIT

Scenario: Policy Evaluation request and null action defined in ACS policy
    Given an existing policy with null defined action
    When A policy evaluation is requested with any HTTP action
    Then policy evaluation returns PERMIT
    