# 0.1.10

## Breaking Changes
None

## Enhancements
* ZDBC Urls can now start with `jdbc:ziti` in addition to `zdbc`.  This new format gets along better with some jdbc clients.
Example `jdbc:ziti:postgresql://localhost:1521/simpledb`
* ZDBC can wait for a Ziti service to be available before attempting to connect.
Some Ziti networks take several seconds to make the list of services available to a new connection.  During this time the JDBC driver may fail to resolve the database host and/or fail to connect. This is usually not an issue for java applications that are properly coded to handle network interruptions. Some utilities expect the database to be immediately available and can fail to run correctly.  Two new JDBC properties have been added to the ZDBC driver to allow these applications to function.

    * **zitiWaitForService:** The name of a Ziti service that must be available before the JDBC driver attempts to connect to your database
    * **zitiWaitForServiceTimeout:** The timeout to wait, expressed as a Java Duration.  For example `PT120S` waits for 30 seconds
    > A note on timeouts - The OpenZiti Java SDK typically refreshes its service list every 60 seconds.  Making the value of `zitiWaitForServiceTimeout` smaller than the SDK timeout will make it innefectual
* ZDBC can now accept an OpenZiti identity as a jdbc string property in addition to a json file or a keystore.  This property allows providing an identity when you do not have permission to write a file to the server. The property name is `zitiIdentity`

## Other Changes
* Ziti Java SDK dependency has been updated to `0.23.13`
