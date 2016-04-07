## Access Control Service

For more information about Access Control Services, please read the following documentation:
https://www.predix.io/docs#IGyNp2eM

### LICENSE
This project is licensed under Apache v2.

### How to run ACS locally
To run the service locally, go to service/ directory

```
cd service
./start-acs-public.sh
```
The ACS service requires a UAA (User Account and Authentication: https://github.com/cloudfoundry/uaa) service to mange OAuth clients and users used in conjunction with ACS.
When running ACS locally, by default the service is configured to trust the local UAA. You can modify the environment varibale ACS_DEFAULT_ISSUER_ID and UAA_CHECK_HEALTH_URL to correspond to your existing UAA.

### How to run UAA locally

Clone the UAA repository from the following url and checkout the 3.2.1 branch.

```
git clone https://github.com/cloudfoundry/uaa.git
cd uaa
git checkout releases/3.2.1
./gradlew assemble -x javadoc
./gradlew run -x javadoc --info
 
```

###Run ACS integration tests

Add the following entries to the /etc/hosts file
  * 127.0.0.1   testzone1.localhost
  * 127.0.0.1   testzone2.localhost
  * 127.0.0.1   testzone3.localhost

The public profile starts UAA and ACS, runs the tests, then stops the ACS and UAA services.

```
cd acs-integration-tests
mvn clean verify -Ppublic
```