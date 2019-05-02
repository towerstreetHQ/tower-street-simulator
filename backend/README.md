# Tower Street Backend

Directory contains Tower Street Backend scala source codes. Includes
API, services, commons a and helper projects for scala source code.

## Backend directory structure

### Api

* attacksimulator-api

### Commons

Contains projects with common code shared by other projects.

* play-api-helpers
    * Reusable code for APIs based on play
    * Controller helpers
    * Service helpers
    * Common DAO
    * Exceptions structure

* slick-model
    * Definition of DAO layer for TS database
    * Slick profile and base DAO definition
    * Generated model classes and tables
    * Custom enums and classes


## Scala project structure

Common project structure for scala code. Parts of the structure can be
omitted of don't make sense for particular project.

```
docker/
    Dockerfile
    parameters.sh                   // File with constants for build script
src/
    main/
        resources/
            application.conf        // Default config for play application
            logback.xml             // Default config for logger (XML SL4J)
            routes                  // Play routing definition
            secret.conf             // Gitignored, local config overrides
        scala/
            io/towerstreet/<proj_package_name>/
                controllers         // Play controllers
                dao                 // DB queries
                exceptions          // Exception definitions
                models/
                    api             // Model used in API (with JSON format)
                    db              // Model used to get data from database
                services            // Implementation of services
    test/
        resources/
            logback-test.xml
        scala
            io/towerstreet/<proj_package_name>/
```


## Naming convention

### Project names

* Lowercase words delimited by dash character
* APIs end with ```-api``` suffix
* Common helper projects ends with ```-helpers``` suffix
* Service project ends with ```-service``` suffix
* Attacksimulator is considered as one word

### Package names

* Lowercase word
* Root package is always ```io.towerstreet```
* The third level is project package name
    * Can be omitted for helpers
    * Simplified project name (dashes and suffixes removed if makes sense)
    * If one word is not possible then use ```_``` as separator
* Inner packages (4th + level)
    * Should be one word
    * Use plural variant of words (also shortcut as DAO is allowed)
