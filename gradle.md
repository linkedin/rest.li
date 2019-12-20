---
layout: get_started
title: Gradle build integration
permalink: /setup/gradle
index: 1
---
# Gradle Build Integration

## Contents

* [Introduction](#introduction)
* [An Example](#an-example)
* [Compatibility](#compatibility)
* [Publishing Maven Artifacts](#publishing-maven-artifacts)
* [Pegasus Plugin in Detail](#pegasus-plugin-in-detail)
* [Underlying Java Classes for Build Integration](#underlying-java-classes-for-build-integration)

## Introduction
Gradle integration is provided as part of Rest.li.  Pegasus simplifies use of Rest.li's code generators and compatibility checking by fully integrating them into the build system. (Note 'pegasus' is also the code name for the Rest.li project).

The underlying Java classes that enable code generation and validation are part of the Rest.li source and could be used to integrate with other build tools.

Adding the plugin is simple. First add a buildscript dependency on the `com.linkedin.pegasus:pegasus-plugin` artifact and then use `apply plugin 'pegasus'` in your build.gradle files.  This topic is explained in detail below.

**Gradle 1.8+ is required.**

## An Example

As an example,  let's consider a simple Rest.li project with three modules:

* An `/api` module containing pegasus schema definitions in the `src/main/pegasus` directory.  This is where java client bindings for the service will be generated. (The client-bindings are sometimes not a separate project, but they are put into the `/api` project along with the schemas.)
* A `/server` module containing resources defined in java classes in the `src/main/java` directory under the `com.linkedin.restli.example.impl` namespace  (E.g., com.linkedin.restli.example.impl.RestLiExampleBasicServer.java).
* An example java client that uses the client-bindings.

### Root build.gradle

/build.gradle:

    apply plugin: 'idea'
    apply plugin: 'eclipse'

    project.ext.externalDependency = [
      'pegasusVersion' : '<version>'
    ]
    
    buildscript {
      repositories {
        mavenLocal()
        mavenCentral()
      }
      dependencies {
        classpath group: 'com.linkedin.pegasus', name: 'gradle-plugins', version: '<version>'
      }
    }
    
    subprojects {
      apply plugin: 'maven'
    
      afterEvaluate {
        if (project.plugins.hasPlugin('java')) {
          sourceCompatibility = JavaVersion.VERSION_1_6
        }
    
        // add the standard pegasus dependencies wherever the plugin is used
        if (project.plugins.hasPlugin('pegasus')) {
          dependencies {
            dataTemplateCompile spec.product.pegasus.data
            restClientCompile spec.product.pegasus.restliClient
    
            // needed for Gradle 1.9+
            restClientCompile spec.product.pegasus.restliCommon
          }
        }
      }
    
      repositories {
        mavenLocal()
        mavenCentral()
      }
    }

/settings.gradle:

    include 'api'
    include 'server'
    include 'client'

### build.gradle for Data API Project

/api/build.gradle:

    apply plugin: 'pegasus'

In `/api`, pegasus data schemas (`.pdl` files) should be added under `/src/main/pegasus`.  E.g.
`/src/main/pegasus/com/linkedin/restli/example/Hello.pdl`.

The pegasus plugin will detect the presence of `.pdl` files and automatically use the `dataTemplateGenerator` task to
generate Java bindings for them.  In this example,  a `Hello.java` class would be generated.

The `dataTemplateCompile` task automatically adds pegasus schemas that `Hello.pdl` depends on, in this case, `Hello.pdl`
depends only on the core data libraries of pegasus, but projects containing other `.pdl` files could be depended on.

Pegasus will detect when a project contains interface definitions (called IDL and located in `.restspec.json` files) in
it's `/src/mainGeneratedRest/idl` directory (usually copied in from an idl extraction task from the server, see below)
and will generate java bindings.   For example, `HelloBuilder.java` is generated from the idl of the hello resource
(`/src/main/idl/com/linkedin/restli/example/impl/Hello.restspec.json) and it written to the
`/src/mainGeneratedRest/java' directory of the `/api` project.

### build.gradle for Server project

/server/build.gradle:

    apply plugin: 'java'
    apply plugin: 'pegasus'
    
    ext.apiProject = project(':api')
    pegasus.main.idlOptions.addIdlItem(['com.linkedin.restli.example.impl']) // optional, if not set, all packages are scanned for resource classes
    
    dependencies {
      compile project(path: ':api', configuration: 'dataTemplate')
      compile "com.linkedin.pegasus:restli-server:<version>"
      // ...
    }

In `/server`, pegasus "Resource" java classes should be defined and should be in the package(s) referred to by
`pegasus.main.idlOptions`.  E.g.  `/src/main/java/com/linkedin/restli/example/impl.HelloResource.java`.

Pegasus will extract an interface definition (.restspec.json) from the resource class and write it to
`/src/mainGeneratedRest/idl` directory.

Once the idl has been generated, it will be copied to the project identified by `ext.apiProject`.  In this example, it
will be copied to `/api/src/main/idl`.  Before it is copied, `api/src/main/idl` is scanned for pre-existing idl.  If any
is found,  it is compared with the replacement idl that will be copied in and a compatibility checker is run that will
return errors if the replacement idl is not backward compatible with the existing idl.  The compatibility checks can be
disabled by setting (but be warned, compatibility errors mean that a server running the new interface definition is now
incompatible with clients running older versions, and should not be pushed to production systems).  If the compatibility
checks pass, the idl is copied into the client directories.  Once copied, new 'Client Bindings' may be generated for the
client, see below.

The compile dependency on `:api` is required if the HelloResource.java depends on `Hello.pdl` and it's generated binding
Hello.java.   Note that the dependency includes a 'configuration' identifying this as a 'dataTemplate' dependency.

### build.gradle for Example Java Client

/client/build.gradle:

    apply plugin: 'java'
    
    dependencies {
      compile project(path: ':api', configuration: 'restClient')
    }

Once rest client bindings in the api project have been generated,  it is trivial for a engineer to depend on the api project and use the generated client bindings to make calls to the new rest.li interface from any remote service.

One must add a compile dependency the 'api' project (or depend on it's published artifacts, more about this below) and be sure to set the dependency configuration to 'restClient'.  Once this is done, it's easy to use the `HelloBuilder` class to construct a request.

## Compatibility

To manage compatibility checking use the rest.model.compatibility flag.   There are 4 different options:  `off`, `equivalent`, `backwards` and `ignore`.

By default, the compatibility strategy is `backwards`. It will only fail on backwards incompatible changes and is the recommended setting to run during normal development.

If you are building rest.li services in a continuous integration environment, we suggest that you set builds to run on `equivalent`, meaning that ALL changes to an interface will cause a build failure. This will ensure that checked in code exactly corresponds with the interface.

If set to `off`, the compatibility check is skipped entirely. `ignore` will run the compatibility checker but will not fail for backward incompatible changes (and will print out the incompatibilities).    

If desired, you may set a local default compatibility level. To do so, modify or create a ~/.gradle/gradle.properties to include:

**~/.gradle/gradle.properties:**

    Prest.model.compatibility=<desired compatibility level here>

For example, to run a build ignoring backward incompatable interface changes (WARNING: remember that backward incompatible changes could break your clients):

    gradle build -Prest.model.compatibility=ignore

To acknowledge a backwards compatible interface change use:

    gradle build -Prest.model.compatibility=backwards

For additional details on compatibility checking, see [Resource Compatibility Checking](/rest.li/modeling/compatibility_check).

## Publishing Maven Artifacts

Often, the client bindings need to be accessible to developers outside the project workspace where the service is developed.  

To publish rest client bindings to any maven repo first modify the api project's gradle to look like:

/api/build.gradle:

    // ... /api/build.gradle code from above ...
    
    artifacts {
      archives mainRestClientJar
      archives mainDataTemplateJar
    } 
    
    configure(install.repositories.mavenInstaller) {
      addFilter('rest-client') {artifact, file ->
        artifact.name == 'api-rest-client'
      }.artifactId = 'rest-client'
      
      addFilter('data-template') {artifact, file ->
        artifact.name == 'api-data-template'
      }.artifactId = 'data-template'
      // artifact names for 'data-model', 'avro-schema' and 'rest-model' may be added as well if needed
    }

The `artifacts` section tells gradle to build jar files for the rest client bindings and the data templates.

The configure part instructs gradle to publish both artifacts into maven. Set names for each. (By default, gradle names the artifact publish to maven to `api`. Since there are two artifacts, they need to be given distinct names.)

Next, update the root build.gradle file to include project information withing the subprojects section:

/build.gradle

    // ... /build.gradle code from above ...
    
    subprojects {
      // ...
    
      project.group = 'org.example'
      project.version = '0.1'
    }

Once the api build.gradle is updated, one can publish the maven artifacts. To publish to the maven local repo, simply run:

    gradle install

to publish to a remove maven repository follow the [gradle documentation](http://www.gradle.org/docs/current/userguide/artifact_management.html) 

Once published, other projects may import the client bindings by depending on the two maven artifacts. For example:

    dependencies {
      compile "org.example:rest-client:0.1"
      compile "org.example:data-template:0.1"
    }

## Pegasus Plugin in Detail

The gradle tasks for pegasus are provided by the 'pegasus' plugin.  The source for this plugin is in
`PegasusGeneratorV2Plugin.groovy`.  This plugin defines custom of gradle `SourceDirectorySet`s for the 'idl', 'pegasus'
source types and `tasks` for the rest.li code generators.  It also defines custom published artifact "configurations"
and dependencies on between these custom published artifact "configurations".

### Source Directory Sets

The plugin recognizes a number of source directories in rest.li projects.  When any of these directories are detected (and they contain at least one source file), the plugin dynamically adds tasks the gradle build dependency tree for these directories. 

In this section we below refers to gradle `sourceSets`.  The most common sourceSets are `main` and `test`.

#### `src/{sourceSet}/pegasus`

Used by 'api' modules.

Contains data schemas (`.pdl`) files.  If data schema files are present in this directory, the
`generate{sourceSet}DataTemplate` tasks (e.g. `generateMainDataTemplate`) will generate java data templates
(RecordTemplate java classes) in the `src/{sourceSet}GeneratedDataTemplate` directory.  

The data schemas files are published into a `*-data-template.jar` artifact.  If ivy is used this artifact is published with the module name and under the 'data-template' classification.

The generated java data templates (RecordTemplate java classes) are are published as a `-data-model.jar` artifact.   If ivy is used this artifact is published with the module name and under the 'data-model' classification.

#### `src/{sourceSet}GeneratedRest/idl`

Used by 'server' modules.

These files are generated by the `generateRestModel` task, for modules containing {*Resource.java} files (which must be
in a package referenced by `pegasus.{sourceSet}.idlOptions.addIdlItem(namespaces)`).   One important aspect of idl is
that by convention they are generated by a 'server' module (and written to the `src/{sourceSet}GeneratedRest/idl`) and
then are copied to the `/src/{sourceSet}/idl` directory of an api module (via the `ext.apiProject` property).  

No artifacts are published directly from the server for these files,  see `src/{sourceSet}/idl` for details on how they are published from the 'api' project.

#### `src/{sourceSet}/idl`

Used by 'api' modules.

Contains published idl (.restspec.json) files.  These files represent the interface definition of the rest.li resources
provided by some service.   They should be checked in to source control.   They are copied into the idl directory from
server module by the `publishRestliIdl` task.  For this copy to happen the server module must contain a `ext.apiProject`
property referencing this 'api' module.  As part of this copy, idl compatibility validation will be run (see above for
details).

The idl is published as a `*-rest-model.jar` artifact.  If ivy is used this artifact is published with the module name and under the 'rest-model' classification.

#### `src/{sourceSet}GeneratedAvroSchema/avro`

Used by 'api' modules.

Avro schema files (.avsc) generated from pegasus data schema files (`.pdl`) by the `generateAvroSchema` task.

### Generator Tasks

All the following tasks are automatically added by the 'pegasus' gradle plugin into the gradle task dependency
hierarchy.  They run automatically and in the correct order run as part of 'gradle build', 'gradle jar' and 'gradle
compileJava' when the plugin detects that they are needed.

#### `generateRestModel` 

Generates .restspec.json files from java files annotated as rest.li resources in the namespaces that have been added to
the idl list using `pegasus.{sourceSet}.idlOptions.addIdlItem()`. Writes these `.restspec.json` files into the
`src/{sourceSet}GeneratedRest/idl` directory.  This tasks is depended on by the `publishRestliIdl` task.

#### `publishRestliIdl`

Copies idl (restspec.json) from server to api project (or whatever the ext.apiProject property is set to).  These files
are normally located in the `src/mainGeneratedRest/idl` directory in the server project and the `src/main/idl` direcotry
in the api project.  This tasks runs compatibility validation (see above).  While not strictly a 'generate' task, it is
a essential part of the generator flow.  It is depended on by the jar task.  

#### `publishRestliSnapshot`

Works the same as `publishRestliIdl` except that it copies "snapshot.json" files usually located in `src/mainGeneratedRest/snapshot` from the server project to the `src/main/snapshot` directory in the api project.

Snapshot files are used for compatibility checking whereas idl files are the formal interface definition and are used to generate client bindings.

#### `generate{sourceSet}GeneratedRestRestClient`

Generates java client bindings (`*Builders.java` classes) into the `src/{sourceSet}GeneratedRest/java`.  It depends on
the .restspec.json  files in `src/{sourceSet}/idl` directory and the pegasus schemas (`.pdl` files) in
`src/{sourceSet}/pegasus` as well as from 'dataModel' dependencies (in ivy, these are dependencies from the "data-model"
classification).   Depended on by the compileJava task.

#### `generateDataTemplate`

Generates java data template bindings (RecordTemplate java classes).  It depends on the pegasus schemas (`.pdl` files)
in `src/{sourceSet}/pegasus` as well as from 'dataModel' dependencies (in ivy, these are dependencies from the
"data-model" classification).  Depended on by the compileJava task.

#### `generateAvroSchema`

Generates avro schemas (.avsc files) from the pegasus schemas (`.pdl` files) in `src/{sourceSet}/pegasus`.  Requires the
same 'dataModel' dependencies as required by the pegasus schemas (in ivy, these are dependencies from the "data-model"
classification).  Depended on by generateDataTemplate task.

To run this task, the `avroSchemaGenerator` "configuration" must be configured with rest.li's `data-avro-generator` artifact.  This is done by adding the follow dependency:

```groovy

dependencies {
  avroSchemaGenerator "com.linkedin.pegasus:data-avro-generator:<pegasus-version>"
}
```
And then adding the following configuration in build.gradle to enable avro schema generation:
```groovy
pegasus.main.generationModes = [PegasusGenerationMode.AVRO]
```

### Published artifacts and their classifications

### `*-data-model.jar` artifact

Contains data schema files, generated by the `generateDataModel` task.   This is only generated from a project if it contains one or more schema files in it's `src/{sourceSet}/pegasus` directory.

* Ivy coordinates: use module's group, name and version, use 'data-model' as classification
* Maven coordinates: use module's group and version.  Use whatever name was configured for the mavenInstaller, which by convention should be '{modulename}-data-model' (see above section about publish maven artifacts).

#### `*-data-template.jar` artifact

Contains java generated bindings (`.class` files) for accessing the pegasus schemas (`.pdl` files) in the module's
`src/{sourceSet}/pegasus` directory.  This artifact is generated by the `generateDataTemplate` task.  This artifact is
only generated from a project if it contains one or more schema files in it's `src/{sourceSet}/pegasus` directory.

This artifact will also define dependencies in its `.pom` or `.ivy` file to data-template artifacts it depends on (these are specified as `dataTemplate` dependencies in the module's build.gradle).

* Ivy coordinates: use module's group, name and version, use 'data-template' as classification
* Maven coordinates: use module's group and version.  Use whatever name was configured for the mavenInstaller, which by convention should be '{modulename}-data-template' (see above section about publish maven artifacts).

#### `*-avro-schema.jar` artifact

Contains .avro schema files for the pegasus schemas (`.pdl` files) in this module's `src/{sourceSet}/pegasus` directory.  This .avro files are generated by the `generateAvroSchema` task.

This artifact will also define dependencies in it's `.pom` or `.ivy` file to avro-schemas artifacts it depends on (these are specified as `dataTemplate` dependencies in the module's build.gradle).

* Ivy coordinates: use module's group, name and version, use 'avro-schema' as classification
* Maven coordinates: use module's group and version.  Use whatever name was configured for the mavenInstaller, which by convention should be '{modulename}-avro-schema' (see above section about publish maven artifacts).

#### `*-rest-model.jar` artifact

Contains .idl (`restspec.json`) files for the idl in the module's `src/{sourceSet}/idl' directory.  These .idl files are generated by the generateRestModel task from a server then copied to an api project by the publishRestliIdl task (via the ext.apiProject property).  

* Ivy coordinates: use module's group, name and version, use 'rest-model' as classification
* Maven coordinates: use module's group and version.  Use whatever name was configured for the mavenInstaller, which by convention should be '{modulename}-rest-model' (see above section about publish maven artifacts).

#### `*-rest-client.jar`

Contains rest client java bindings (`*Builders.java` classes) generated from the idl of the source module.

This artifact will also define dependencies in its `.pom` or `.ivy` file to java data template binding artifacts
(`*-data-model.jar`) it requires, including the one for the module itself and for any other pegasus schemas it depends
on (these are specified as `dataModel` dependencies in the module's build.gradle).

* Ivy coordinates: use module's group, name and version, use 'rest-client' as classification
* Maven coordinates: use module's group and version.  Use whatever name was configured for the mavenInstaller, which by convention should be '{modulename}-rest-client' (see above section about publish maven artifacts).

### Dependency types

There are two types of pegasus plugin dependency types.  The first type is one required by the plugin for running code
generators and compiling code.  The second type is those developers can use to define different sorts dependencies
between the various source languages, primarily pegasus schemas (`.pdl` files).

#### Dependencies used by build tooling

`restTools` - required by 'api' and 'server' modules to generate rest client bindings (*Builders.java files), run compatibility checks, and use rest.li document generation (docgen).   The dependency must refer to a compatible version of the pegasus:rest-tools artifact.

`dataTemplateCompile` - Required by 'api' modules to do data template compilation.  The dependency must refer to a compatible version of the pegasus:data artifact.

`dataTemplateGenerator` - Required by 'api' modules to do data template generation. The dependency must refer to a compatible version of the pegasus:generator artifact.

`restClientCompile` - Required by 'api' modules to compile client java bindings (*Builders.java files) to .class files. The dependency must refer to a compatible version of the pegasus:restli-client artifact.

Example build.gradle for an 'api' module:

    ...
    dependencies {
      compile "com.linkedin.pegasus:restli-client:<version>"

      dataTemplateCompile "com.linkedin.pegasus:data:<version>"
      dataTemplateGenerator "com.linkedin.pegasus:generator:<version>"
      restTools "com.linkedin.pegasus:restli-tools:<version>"
      restClientCompile "com.linkedin.pegasus:restli-client:<version>"
    }

#### Pegasus Schema Dependencies

`dataTemplate` - Adds a dependency on the pegasus schemas from another module or artifact.  This is required when the current module's data schema files refer to schema types that reside in another module or artifact.

Example build.gradle:

    ...
    dependencies {
      // for ivy:
      dataTemplate group: 'org.example', name: 'common-pegasus-schemas', version: '1.0', classifier: 'dataTemplate'
      // for maven, remove the classifier and change the name to match the artifact name of the published dataTemplate, by convention it should be '{modulename}-data-template}'
      ...
    }

## Underlying Java Classes for Build Integration

This is provided for reference only.  A understanding of these classes is not required to use pegasus.  These classes would be useful primarily if one were deeply integrating pegasus with a build system not already supported by pegasus.

### Avro Schema Generator

Generate Avro avsc files from Pegasus data schemas (`.pdl` files):

    java [-Dgenerator.resolver.path=<dataSchemaRelativePath>] \
      [-Dgenerator.avro.optional.default=<optionalDefault>] \
      [-Dgenerator.avro.namespace.override=<overrideNamespace>] \
      -cp <CLASSPATH> com.linkedin.data.avro.generator.AvroSchemaGenerator \
      <outputDir> [<inputFileOrDir> ...]

* dataSchemaRelativePath - Path to `.pdl` files. (e.g., `/src/main/pegasus`).
* optionalDefault - Specifies how an optional field with a default value should be translated (see [Converting Rest.li to Avro](/rest.li/Rest_li_Avro_conversions#converting-restli-to-avro)).
* overrideNamespace - If `true`, each translated `.avsc` file will have its namespace prepended with `"avro."` (see [Converting Rest.li to Avro](/rest.li/Rest_li_Avro_conversions#converting-restli-to-avro)).
* CLASSPATH - `com.linkedin.pegasus:data:[CURRENT_VERSION]` AND `com.linkedin.pegasus:data-avro:[CURRENT_VERSION]` artifacts and all their dependencies.
* outputDir - output directory for generated `.avsc` files.
* inputFileOrDir - file name of a Pegasus data schema file, a directory containing Pegasus data schema files, or a fully qualified schema name.

Build integration: for builds requiring avro schemas, assembly (creation of jar) should depend on this task

### Pegasus Data Template Generator

Generates Java data templates (`.java` files) from Pegasus Data Model schemas (`.pdl` files):

    java [-Dgenerator.resolver.path=<dataSchemaRelativePath>] -cp <CLASSPATH> \
      com.linkedin.pegasus.generator.PegasusDataTemplateGenerator \
      <outputDir> [<inputFileOrDir> ...]

* dataSchemaRelativePath - Path to data schema files. (e.g., `/src/main/pegasus`).
* CLASSPATH - `com.linkedin.pegasus:generator:[CURRENT_VERSION]` artifact and all its dependencies.
* outputDir - output directory for generated java source files
* inputFileOrDir - file name of a Pegasus data schema file, a directory containing Pegasus data schema files, or a fully qualified schema name

### Generate Rest Model IDL

Serializes a set of resource models to a RESTspec IDL file:

    java -cp <CLASSPATH> com.linkedin.restli.tools.idlgen.RestLiResourceModelExporterCmdLineApp \
      -outdir <outputDirPath> -sourcepath <sourcePath> -resourcepackages <resourcePackages>

* CLASSPATH - `com.linkedin.pegasus:restli-tools:[CURRENT_VERSION]` artifact and all its dependencies. Compiled classes within the java packages referred to by `resourcePackages`
* outputDirPath - Directory in which to output the generated IDL files (default=current working dir)
* sourcePath - Space-delimited list of directories in which to find resource Java source files
* resourcePackages - Space-delimited list of packages to scan for resource classes

Build integration: assembly (creation of jar) should depend on this task. This task depends on compilation of classes within the java packages referred to by `resourcePackages`.

### Validate and Publish IDL

Copies IDL (.restspec.json) files to client module and check backwards compatibility between pairs of idl (.restspec.json) files. The check result messages are categorized:

    java [-Dgenerator.resolver.path=<dataSchemaRelativePath>] -cp CLASSPATH \
      com.linkedin.restli.tools.idlcheck.RestLiResourceModelCompatibilityChecker \
      [--compat OFF|IGNORE|BACKWARDS|EQUIVALENT] [pairs of <prevRestspecPath currRestspecPath>]

* dataSchemaRelativePath - Path to data schema files required by the interface definition (e.g.  /src/main/pegasus).
* CLASSPATH - `com.linkedin.pegasus:restli-tools:[CURRENT_VERSION]` artifact and all it's dependencies.
* prevRestspecPath - 
* currRestspecPath - 

Build integration: assembly (creation of jar) should depend on this task. If compatibility checker passes, all
.restspec.json files should be copied from the server module to the module where client bindings are generated.  This
task depends on the `Generate Rest Model IDL` task.  A property named `rest.model.compatibility` should be overridable
by the developer (allowing them to set it to `ignore` or `backwards`) and should default to 'equivalent' if they do not
provide it.

### Rest Client Generation
Generates Java request builders from Rest.li idl:

    java [-Dgenerator.resolver.path=<dataSchemaRelativePath>] \
         [-Dgenerator.rest.generate.datatemplates=<true|false>] \
      -cp <CLASSPATH> com.linkedin.restli.tools.clientgen.RestRequestBuilderGenerator \
      <targetDirectoryPath> [<sourceFileOrDir> ...]

* dataSchemaRelativePath - Path to data schema files required by the interface definition.
* generator.rest.generate.datatemplates - false unless task should also generate java data template bindings
* CLASSPATH - `com.linkedin.pegasus:restli-tools:[CURRENT_VERSION]` artifact and all its dependencies.
* targetDirectoryPath - path to target root java source directory
* sourceFileOrDir - paths to IDL files or directories

Build integration: Compilation of java source should depend on this task.

### Config Build Script

To construct these build tasks, it can help to add a utility task that constructs a list of all the source paths used for `data template generation`, `avro schema generation`, `rest model generation` and `rest client generation`.

### Clean Generated

No java class for this.  All directories written to by `data template generation`, `avro schema generation`, `rest model generation` and `rest client generation` should be deleted.

Build integration: clean task should depend on this
