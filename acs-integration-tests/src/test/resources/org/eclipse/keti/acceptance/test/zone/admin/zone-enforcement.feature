Feature: Zone enforcement

    Scenario Outline: A client can only access resources in the zone to which it is registered
    Given zone 1 and zone 2
    When client_one does a PUT on <api> with <identifier_in_zone1> in zone 1
    And client_one does a GET on <api> with <identifier_in_zone1> in zone 1
    Then the request has status code 200
    When client_two does a GET on <api> with <identifier_in_zone1> in zone 1
    Then the request has status code 403
    When client_two does a PUT on <api> with <identifier_in_zone1> in zone 2
    And client_two does a GET on <api> with <identifier_in_zone1> in zone 2
    Then the request has status code 200

    Examples:
    | api | identifier_in_zone1|
    |subject|subject_id_1|
    |resource|resource_id_1|
    |policy-set|policy_set_1|

    Scenario Outline: A client can only delete resources in the zone to which it is registered
    Given zone 1 and zone 2
    When client_one does a PUT on <api> with <identifier_in_zone1> in zone 1
    And client_two does a DELETE on <api> with <identifier_in_zone1> in zone 2
    And client_one does a GET on <api> with <identifier_in_zone1> in zone 1
    Then the request has status code 200
    When client_one does a DELETE on <api> with <identifier_in_zone1> in zone 1
    Then the request has status code 204

    Examples:
    | api | identifier_in_zone1|
    |subject|subject_id_1|
    |resource|resource_id_1|
    |policy-set|policy_set_1|
