Feature: PolicySet Creation
Scenario: Create a policy with no action defined
    Given A policy with no action defined
    Then the policy creation returns SUCCESS

Scenario: Create a policy with single valid action defined
    Given A policy with single valid action defined
    Then the policy creation returns SUCCESS

Scenario: Create a policy with multiple valid actions defined
    Given A policy with multiple valid actions defined
    Then the policy creation returns SUCCESS

Scenario: Create a policy with single invalid action defined
    Given A policy with single invalid action defined
    Then the policy creation returns INVALID_POLICY_SET

Scenario: Create a policy with multiple actions containing one invalid action defined
    Given A policy with multiple actions containing one invalid action defined
    Then the policy creation returns INVALID_POLICY_SET