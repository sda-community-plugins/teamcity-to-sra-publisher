TeamCity to Micro Focus Deploy Plugin
=====================================

This plugin integrates TeamCity with Micro Focus Deployment Automation. It allows you to publish the artifacts from a 
successful build into Deployment Automation and then optionally deploy the uploaded version to an environment.

Build Instructions
------------------

You will need Apache Maven and a Java JDK 1.7 or later to build the plugin.

Execute (from the root directory):

> mvn clean package

The resulting package 'SerenaDeploy-<version>.zip' will be placed in the 'target' directory.

Installation Instructions
-------------------------

To install the plugin, wither copy the zip archive to the 'plugins' directory under the TeamCity 'data' directory
or use the 'Administration' page in the TeamCity web app to upload the plugin. Either way, the server will need to
be restarted for the change to take affect.

See README.docx for instructions on how to use the plugin.

Thank you!

kevin.lee@microfocus.com

