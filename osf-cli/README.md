#OSF Command Line Interface 
This module supplies a command line interface (CLI) for retrieving a registration from an OSF instance and writing it locally into a package which conforms to the Data Conservancy packaging specification.

##How It Works
The CLI uses functionality in the `osf-packager` module and the OSF Java client in the `osf-client` package to attach to the OSF REST APIs (both v1 and v2) on a running OSF instance specified by a configuration file whose location must be supplied by the user. Additional parameters will need to be supplied as indicated below. The packager code leverages a workflow from the Dataconservancy Package Tool GUI to construct the package once the content from the target registration has been retrieved. The package is then saved to a location specified by the user as a command line option.


##Command Line Usage
The CLI takes a single argument which is a URL pointing to the registration whcih is to be packaged (for example, `http://192.168.99.100:8000/v2/registrations/vacbu/`
This is preceded by a list of command line options as follows:

```
-c (-configuration, --configuration)  FILE   : path to the OSF Java client configuration
-h (-help, --help)                           : print help message
-m (-metadata, --metadata) FILE              : the path to the metadata properties file for additional bag metadata
-n (-name, --name) VAL                       : the name for the bag
-o (-output, --output) FILE                  : path to the directory where the package will be written
-v (-version, --version)                     : print version information
```
The `-c, -m, -n` and `-o` options are required. The OSF Java client must be configured so that it knows which running OSF instance to attach to. Additional metadata is specified in a bag metadata properties file. Some of this metadata will be required in order to produce a conforming package. The package name is one of these required metadat properties - it is used to both name the package and the bag. Finally, the output location will tell the CLI where to write the package.

###OSF Java Client Configuration
Configuration mut be supplied for both the v1 and v2 endpoints, since both are needed to build the package.  An example is below:
```
{
  "osf": {
    "v2": {
      "host": "192.168.99.100",
      "port": "8000",
      "basePath": "/v2/",
      "authHeader": "Basic ZW1ldHNnZXJAZ21haWwuY29tOmZvb2JhcmJheg==",
      "scheme": "http"
    }
  },
  "wb": {
    "v1": {
      "host": "192.168.99.100",
      "port": "7777",
      "basePath": "/v1/",
      "scheme": "http"
    }
  }
}
```
The absolute path to the file containing this configuration is what must be supplied as the value for the `-c` option.

###Bag Metadata File
The `-m` option also takes an absolute path as its value. This file contains bag metadata which will conforn to the Data Conservancy BagIt Profile specification. Metadata entries will consist of key = value pairs, one per line. The values may be lists where appropriate, expressed as comma separated strings. If the value of Package-Name is specified in this file, it will be superseded by the value specified for the `-n` option below. 

###Output Directory
The `-o` option takes an absolute path as its value. This path must point to an existing directory on the filesystem.

###Bag Name
The `-n` option takes a string value. This string will be used as both the name of the bag, and the name of the package.

###Example
A command line invocation might look something like this:

```java -jar osf-cli-1.0.0-SNAPSHOT.jar -c /home/luser/OSF-Java-Client.cong -m /home/luser/bag.properties -n ImportantPackage -o /home/luser/packages http://192.168.99.100:8000/v2/registrations/vacbu/ ```

In this case, the command is executed in the working directory where the executable jar file osf-cli-1.0.0-SNAPSHOT.jar resides.

##Current Status
The first iteration of the CLI has soem simplifying constraints in order to deliver some kindof useful functionality. Right now, jusr registrations may be processed. We also have limited some of the options which are available to the GUI tool. The following bag metadata and package generation parameters are supplied to the CLI internally:
```
#***************************************************
# The following fields are REQUIRED to be supplied
#***************************************************
Package-Format-Id = BOREM
BagIt-Profile-Identifier = http://dataconservancy.org/formats/data-conservancy-pkg-1.0

#***************************************************
# The following fields SHOULD be supplied
#***************************************************
#Options for Checksum algorimth are: md5, sha1
Checksum-Algs = md5
#Options for Archiving-format are: tar, zip, none
Archiving-Format = tar
#Options for Compression-format are: gz, none
Compression-Format = gz
#Options for ReM-Serialization-Format are: json, turtle, xml
ReM-Serialization-Format = TURTLE
```
