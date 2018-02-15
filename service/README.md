### Monitoring

The following monitoring API endpoint are available:

* GET /monitoring/heartbeat
  
  Description: Returns "alive" as long asa the ACS Service is up and running. No dependencies are checked. This is API is light weigh as can be invoked as frequently as needed.

1. GET /health
  
  Description: Returns on of the following custom statuses:
    - ACS_DB_OUT_OF_SERVICE: Returned when the ACS Service could not communicate with the ACS Database. 
    - UAA_OUT_OF_SERVICE: Returned when the ACS Service could not communicate with the UAA.
    - Other Spring Standard status values can be returned: DOWN, OUT_OF_SERVICE, UNKNOWN, UP

NOTE: **Since this API does perform checks on remote resources (DB and UAA) calling it too frequently might cause additional traffic and resource consumption, please use it judiciously.**
