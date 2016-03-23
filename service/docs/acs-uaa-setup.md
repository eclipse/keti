# Setting Up UAA for ACS

## Installing UAAC

UAAC is a ruby gem that wraps RESTful API calls to UAA in a convenient
CLI command. To install this feature:

1. Install ruby on your platform
2. Install the cloud foundry UAAC ruby gem
```
gem install cf-uaac
```

## Getting an Administrator Token

Before you can issue commands to UAA you have to login as an
administrator.

1. Instruct UAAC to target your UAA server.
```
uaac target https://[host]:[port]/uaa
```
2. Login as the UAAC administrator.
```
uaac token owner get account_manager admin -s \
[account_manager client secret] -p [admin password]
```

## Create ACS Privilege Groups

The following groups designate ACS privileges to their members.

1. Create group for reading and evaluating policies.
```
uaac group add acs.policies.read
```
2. Create group for writing policies.
```
uaac group add acs.policies.write
```
3. Create group for reading attributes.
```
uaac group add acs.attributes.read
```
4. Create group for writing attributes.
```
uaac group add acs.attributes.write
```

## Making an Application User an ACS Administrator

ACS administrators can read/write policies and/or attributes. This
user will create and maintain policies and attribute assignments that
will enforce application privileges.

1. Create an administrative user if one does not yet exist
```
uaac user add admin-user -p [password] --emails admin-user@example.com
```
2. Assign membership to the required groups.
```
uaac member add acs.policies.read admin-user
```
```
uaac member add acs.zones.admin admin-user
```
```
uaac member add acs.policies.write admin-user
```
```
uaac member add acs.attributes.read admin-user
```
```
uaac member add acs.attributes.write admin-user
```

## Giving Application Users Attribute Management Privileges

Your application will most likely have users that can assign and
revoke privileges to other users. This is an example of how to create
such users or simply assign the required group membership.

1. Create the application user if one does not yet exist
```
uaac user add app-user -p [password] --emails app-user@example.com
```
2. Assign membership to the required groups.
```
uaac member add predix-acs.zones.<acs_zone_name>.user app-user
```
```
uaac member add acs.attributes.read app-user
```
```
uaac member add acs.attributes.write app-user
```

## Creating or Modifying an OAuth Client to Allow ACS Operations

You will need an OAuth client that allows users to perform ACS
operations. To create the OAuth client:

```
uaac client add acs-app --scope \
acs.policies.read,acs.policies.write,acs.attributes.read,acs.attributes.write,predix-acs.zones.<acs_zone_name>.user \
--authorized_grant_types [grant types] --authorities uaa.resource -s [client secret]
```

Note: See UAAC help for available OAuth grant types.
Note: If a client already exists use "uaac client update" instead.

## Giving Your Application OAuth Client Policy Evaluation Privileges

You need an application OAuth client in order to call the ACS policy
evaluation service. Here's how you can create one if you have not
already. You can reuse the client created in the previous
step. However, do not reuse that client if you want to separate which
applications have the ability to perform ACS administrative tasks on
behalf of users.

```
uaac client add myapp --authorized_grant_types [grant types] -s \
[client secret]
```

## Registering your UAA with ACS

TODO: Add steps for registering UAA with ACS

