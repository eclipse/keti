# How to Provision Zones

## Get token for a user authorized to create zones
* Eventually, this step will be done by the ACS service broker. 
* The command below is a sample to illustrate how to get a token from any UAA instance. The value passed in Authorization header below is base64 encoded value of '{client-id:client-secret}'.  
For the steps below, the token must be for a user/clientid combination which is available in the trusted UAA for the ACS Instance being used.

```bash
curl 'http://localhost:8080/uaa/oauth/token'  \
	-H 'Authorization: Basic <base64(<CLIENT_ID:<CLIENT_SECRET>)>' \
	-H 'Content-Type: application/x-www-form-urlencoded'  \
	--data 'username=<USER_ID>&password=<PASSWORD>&grant_type=password'
```
	
## Create a zone
```bash
curl -X PUT http://localhost:8181/v1/zone/my-acs-zone -d@<FILE_PATH>/sample-zone.json -v \
-H "Authorization: Bearer <TOKEN_FROM_PREVIOUS_CMD>" \
-H "Content-Type: application/json"	
```

## Verify a zone is created
```bash
curl -X GET http://localhost:8181/v1/zone/my-acs-zone -v \
-H "Authorization: Bearer <TOKEN_FROM_PREVIOUS_CMD>" \
-H "Content-Type: application/json"	
```
