## POM Versioning and Release Steps
### Scenario:  
* develop is at 2.3.0-SNAPSHOT
* master is at 2.2.0
* Next release version is 2.3.0

### Steps
* Create a branch off master, say release230
* Merge develop to release230. Resolve any conflicts and commit.
* Update POM version of release230 branch, to 2.3.0  (from 2.3.0-SNAPSHOT)
  *  ```./versioning-acs.sh 2.3.0```
  * Update the acs .jar path with new version in service/start-acs.sh and service/manifest.yml  
  * Review diffs, run unit and integration tests
  * Commit to release230
  * Create a pull request from release230 to master
  * ACS Pipeline changes - update *_VERSION variables in "shell build step" in acs-integration-master and  acs-release-master acs-spring-sec-integ-sample-master build plans.
  * After review,  merge release230 to master. This will trigger the deployment to 'release' space.
  * Create a lightweight tag for v2.3.0 in master. 
* Update POM version in develop to 2.4.0-SNAPSHOT using steps similar to what were used for master, above.
  * Update APP_VERSION variable in acs-integration-develop and acs-rc-develop jenkins plans. 
 
