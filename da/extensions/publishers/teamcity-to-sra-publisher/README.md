TeamCity to Serena Deploy Plugin
================================

This plugin integrates TeamCity with Serena Deployment Automation. It allows you to publish the artifacts from a successful build into Deployment Automation and then
optionally deploy the uploaded version to an environment.

Build Instructions
------------------

You will need Apache Maven and a Java JDK 1.6 or later to build the plugin.

To successfully build the plugin using maven you will need to have built SDA firstt for the command and vfs libraries to
be cached and available in your local .m2 repository.

Then execute:

> mvn clean package

See README.docx for instructions on how to use the plugin.

Thank you!

klee@serena.com

