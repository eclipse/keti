## Access Control Service (ACS)

For more information about Access Control Services, please read the following documentation:
https://www.predix.io/docs#IGyNp2eM

### LICENSE
This project is licensed under Apache v2.

### How to build ACS and run unit tests
To build ACS without support for hierarchical attributes and run unit tests

```
mvn clean install [-P "public-titan"] [-s <maven_settings_file>]
```
Specify "public-titan" profile to build ACS with support for hierarchical attributes.

### How to run ACS locally
To run the service locally without support for hierarchical attributes

```
./service/start-acs-public.sh
```
To run the service locally with support for hierarchical attributes

```
./service/start-acs-public-titan.sh
```

The ACS service requires a UAA (User Account and Authentication: https://github.com/cloudfoundry/uaa) service to manage OAuth clients and users used in conjunction with ACS.
When running ACS locally, by default the service is configured to trust the local UAA. You can modify the environment variable `ACS_DEFAULT_ISSUER_ID` and `UAA_CHECK_HEALTH_URL` to correspond to your existing UAA.

### How to run UAA locally

Clone the UAA repository from the following url

```
git clone https://github.com/cloudfoundry/uaa.git
cd uaa
./gradlew assemble -x javadoc
./gradlew run -x javadoc --info
```

### How to run ACS integration tests

The script below starts UAA and ACS, runs the tests, then stops the ACS and UAA services.

```
./run-integration-tests.sh [-t -s <maven_settings_file>]
```

Specify -t option to run integration tests against ACS that supports hierarchical attributes.
