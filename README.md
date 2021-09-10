# zdbc
A repository to provide jdbc driver configurations for access to databases via ziti

# Overview
## Goal
The goals of this project are:

1.  Provide a set of tools to automatically configure existing JDBC drivers to connect via a Ziti network.
1.  Leave the JDBC drivers provided by the vendor alone.  This project does not include forks of any JDBC drivers.
1.  Provide a way to connect developer tools to a database over a Ziti network.

# Example of integrating into developer tools
## Requirements
1.  A Ziti network and database: <https://github.com/openziti/ziti-sdk-jvm/blob/main/samples/jdbc-postgres/cheatsheet.md> 
1.  Squirre-Sql client: http://squirrel-sql.sourceforge.net/#installation
1.  Ziti all-in-one (fat) jar: <insert link to Java SDK>
1.  zdbc wrapper (this project): <insert link to jar>

## Step by Step
1.  Configure a ziti network and postgres database following the cheatsheet https://github.com/openziti/ziti-sdk-jvm/blob/main/samples/jdbc-postgres/cheatsheet.md> 
1.  Copy the Ziti all-in-one jar into the Squirrel-Sql `lib` folder
1.  Start Squirrel-Sql
1.  Configure the Squirrel-Sql PostgreSQL driver
    1. Add the PostgreSQL and ziti-jdbc-wrapper jar files to the driver's `Extra Class Path`
    1. Click 'List Drivers' and select `org.openziti.jdbc.ZitiDriver` in the `Class Name` field
1.  Create a PostgreSQL alias with the following values
    1. Name: `Ziti example PostgreSQL`
    1. Driver: `PostgreSQL`
    1. URL: `zdbc:postgresql://zitified-postgres/simpledb`
    1. User Name: `postgres`
    1. Password: `postgres`
    1. The Ziti Identity file is provided via driver properties.   Click the Properties button and set the zitiIdentityFile property to the java-identity.json file created during the network setup

# Example of integrating into a Java application
## Requirements
1.  A Ziti network and database: <https://github.com/openziti/ziti-sdk-jvm/blob/main/samples/jdbc-postgres/cheatsheet.md> 

## Step by Step
1.  Configure a ziti network and postgres database following the cheatsheet https://github.com/openziti/ziti-sdk-jvm/blob/main/samples/jdbc-postgres/cheatsheet.md>
1.  Run the included `samples/postgresql` project, providing the name of the java-identity.json file created during the previous step.