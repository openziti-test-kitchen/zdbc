# zdbc
A repository to provide jdbc driver configurations for access to databases via ziti

# Overview
## Goal
The goals of this project are:

1.  Provide a set of tools to automatically configure existing JDBC drivers to connect via a Ziti network.
1.  Leave the JDBC drivers provided by the vendor alone.  This project does not include forks of any JDBC drivers.
1.  Provide a way to connect developer tools to a database over a Ziti network.

## Driver Features
Each JDBC driver needs specific ziti features in order to work.  This table attempts to capture which each driver requires

| Driver | Shim Included | Ziti Features | Notes |
| ------ | :------------:| ------------- | ----- |
| org.postgresql.Driver | Y | Socket Factory | Requires jdbc property socketFactory |
| oracle.jdbc.OracleDriver | Y | NIO Provider | <ul><li>Tested with public and private autonomous databases</li><li>Requires the database host to be resolvable</li></ul> |
| com.mysql.jdbc.Driver | Y | init with seamless mode | |
| org.h2.Driver | N | init with seamless mode | Requires the database host to be resolvable |

# How it works
The zdbc driver registers with `java.sql.DriverManager` when the zdbc wrapper jar is included in the application.  The Ziti JDBC wrapper checks each database URL to see if it starts with `zdbc`.  If it does, then the wrapper accepts the connection request, configures Ziti,  configures the driver,  and then delegates to the driver to establish a database connection over the Ziti network fabric.

# Example of integrating into developer tools
## Requirements
1.  A Ziti network and database: <https://github.com/openziti/ziti-sdk-jvm/blob/main/samples/jdbc-postgres/cheatsheet.md> 
1.  Squirrel-Sql client: http://squirrel-sql.sourceforge.net/#installation
1.  Ziti all-in-one (fat) jar: <insert link to Java SDK>
1.  zdbc wrapper (this project): <insert link to jar>

## Step by Step
1.  Configure a ziti network and postgres database following the cheatsheet https://github.com/openziti/ziti-sdk-jvm/blob/main/samples/jdbc-postgres/cheatsheet.md> 
1.  Copy the Ziti all-in-one jar into the Squirrel-Sql `lib` folder

> ls $SQUIRREL_HOME\lib | grep ziti <br>
  ziti-0.22.5-all.jar

1.  Start Squirrel-Sql
1.  Configure the Squirrel-Sql PostgreSQL driver
    1. Add the PostgreSQL and ziti-jdbc-wrapper jar files to the driver's `Extra Class Path`
    1. Click 'List Drivers' and select `org.openziti.jdbc.ZitiDriver` in the `Class Name` field
    <br>![Edit Driver](/images/Driver-Edit.png)
    <br>![Configure Driver](/images/Driver-Details.png)
1.  Create a PostgreSQL alias with the following values
    1. Name: `Ziti example PostgreSQL`
    1. Driver: `PostgreSQL`
    1. URL: `zdbc:postgresql://zitified-postgres/simpledb`
    1. User Name: `postgres`
    1. Password: `postgres`
    1. The Ziti Identity file is provided via driver properties.   Click the Properties button and set the zitiIdentityFile property to the java-identity.json file created during the network setup
    <br>![Create Alias](/images/Alias-Create.png)
    <br>![Alias Details](/images/Alias-Details.png)
    <br>![Open Properties](/images/Alias-OpenProps.png)
    <br>![Open Properties](/images/Alias-SelectProps.png)
    <br>![Set Property](/images/Alias-SetProp.png)

# Example of integrating into a Java application
## Requirements
1.  A Ziti network and database: <https://github.com/openziti/ziti-sdk-jvm/blob/main/samples/jdbc-postgres/cheatsheet.md> 

## Step by Step
1.  Configure a ziti network and postgres database following the cheatsheet https://github.com/openziti/ziti-sdk-jvm/blob/main/samples/jdbc-postgres/cheatsheet.md>
1.  Run the included `samples/postgresql` project, providing the name of the java-identity.json file created during the previous step.