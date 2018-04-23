CHANGES
=======

1.2.2
-----
 - Fixed "Add Files to Existing Version" not finding existing version
 - If "Add Files to Existing Version" is selected and the version doesn't exist, it is now created instead of failing the build.
 
1.2.1
-----
 - Updated check for existing files as searchQuery is limited - now retrieve all the existing component versions and iterate over them
 
1.2
---
 - Added checkbox to allow users to publish into existing version.
 - Plugin now checks if a component version exists before uploading into it.
 - Tidied up logging and build failure messages.
 - Tested against TeamCity 10.x.
 - Changed Serena references to Micro Focus.
 
1.1
---
 - Added ability to configure and send version properties to SDA.
 - Added ability to configure and send process deployment properties to SDA.
 - Added ability to publish and deploy based on checkbox and build resolveable parameters, i.e. allow user to tick a box at execution time.
 
1.0
---
 - Initial version created for MUFJ.
