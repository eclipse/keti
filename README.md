## Keti - Access Control Service
Note: Keti was earlier called ACS. This repository still has references to artifacts with that name, and can be treated as synonymous to Keti.

### LICENSE
This project is licensed under Apache v2.

### How to build and run unit tests
To build Keti without support for hierarchical attributes and run unit tests

```
mvn clean install [-P "public-titan"] [-s <maven_settings_file>]
```
Specify "public-titan" profile to build Keti with support for hierarchical attributes.

### Start Keti locally
To run the service locally without support for hierarchical attributes

```
./service/start-acs-public.sh
```
To run the service locally with support for hierarchical attributes

```
./service/start-acs-public-titan.sh
```

The Keti service requires a UAA (User Account and Authentication: https://github.com/cloudfoundry/uaa) service to manage OAuth clients and users, which are authorized to access it. When running Keti locally, by default the service is configured to trust the local UAA. You can modify the environment variable `ACS_DEFAULT_ISSUER_ID` and `UAA_CHECK_HEALTH_URL` to correspond to your existing UAA.

### Runnning UAA locally

Clone the UAA repository from the following url

```
git clone https://github.com/cloudfoundry/uaa.git
cd uaa
./gradlew assemble -x javadoc
./gradlew run -x javadoc --info
```

### How to run integration tests

The script below starts UAA and Keti, runs the tests, then stops the Keti and UAA services.

```
./run-integration-tests.sh [-t -s <maven_settings_file>]
```

Specify -t option to run integration tests against Keti that supports hierarchical attributes.
