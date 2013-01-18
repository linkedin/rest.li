/*
   Copyright (c) 2012 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.linkedin.pegasus.gradle

import com.linkedin.pegasus.gradle.PegasusOptions.Mode
import org.gradle.BuildResult
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Copy
import org.gradle.api.plugins.JavaPlugin

/**
 * Pegasus code generation plugin.
 * The supported project layout for this plugin is as follows:
 *
 * <pre>
 *   --- api/
 *   |   --- build.gradle
 *   |   --- src/
 *   |       --- &lt;sourceSet&gt;/
 *   |       |   --- idl/
 *   |       |   |   --- &lt;published idl (.restspec.json) files&gt;
 *   |       |   --- java/
 *   |       |   |   --- &lt;packageName&gt;/
 *   |       |   |       --- &lt;common java files&gt;
 *   |       |   --- pegasus/
 *   |       |       --- &lt;packageName&gt;/
 *   |       |           --- &lt;data schema (.pdsc) files&gt;
 *   |       --- &lt;sourceSet&gt;GeneratedDataTemplate/
 *   |       |   --- java/
 *   |       |       --- &lt;packageName&gt;/
 *   |       |           --- &lt;data template source files generated from data schema (.pdsc) files&gt;
 *   |       --- &lt;sourceSet&gt;GeneratedAvroSchema/
 *   |       |   --- avro/
 *   |       |       --- &lt;packageName&gt;/
 *   |       |           --- &lt;avsc avro schema files (.avsc) generated from pegasus schema files&gt;
 *   |       --- &lt;sourceSet&gt;GeneratedRest/
 *   |           --- java/
 *   |               --- &lt;packageName&gt;/
 *   |                   --- &lt;rest client source (.java) files generated from published idl&gt;
 *   --- impl/
 *   |   --- build.gradle
 *   |   --- src/
 *   |       --- &lt;sourceSet&gt;/
 *   |       |   --- java/
 *   |       |       --- &lt;packageName&gt;/
 *   |       |           --- &lt;resource class source (.java) files&gt;
 *   |       --- &lt;sourceSet&gt;GeneratedRest/
 *   |           --- idl/
 *   |               --- &lt;generated idl (.restspec.json) files&gt;
 *   --- &lt;other projects&gt;/
 * </pre>
 * <ul>
 *   <li>
 *    <i>api</i>: contains all the files which are commonly depended by the server and
 *    client implementation. The common files include the data schema (.pdsc) files,
 *    the idl (.restspec.json) files and potentially Java interface files used by both sides.
 *  </li>
 *  <li>
 *    <i>impl</i>: contains the resource class for server implementation.
 *  </li>
 * </ul>
 * <p>Performs the following functions:</p>
 *
 * <p><b>Generate data model and data template jars for each source set.</b></p>
 *
 * <p><i>Overview:</i></p>
 *
 * <p>
 * In the api project, the plugin generates the data template source (.java) files from the
 * data schema (.pdsc) files, and furthermore compiles the source files and packages them
 * to jar files. Details of jar contents will be explained in following paragraphs.
 * In general, data schema files should exist only in api projects.
 * </p>
 *
 * <p>
 * Configure the server and client implementation projects to depend on the
 * api project's dataTemplate configuration to get access to the generated data templates
 * from within these projects. This allows api classes to be built first so that implementation
 * projects can consume them. We recommend this structure to avoid circular dependencies
 * (directly or indirectly) among implementation projects.
 * </p>
 *
 * <p><i>Detail:</i></p>
 *
 * <p>
 * Generates data template source (.java) files from data schema (.pdsc) files,
 * compiles the data template source (.java) files into class (.class) files,
 * creates a data model jar file and a data template jar file.
 * The data model jar file contains the source data schema (.pdsc) files.
 * The data template jar file contains both the source data schema (.pdsc) files
 * and the generated data template class (.class) files.
 * </p>
 *
 * <p>
 * In the data template generation phase, the plugin creates a new target source set
 * for the generated files. The new target source set's name is the input source set name's
 * suffixed with "GeneratedDataTemplate", e.g. "mainGeneratedDataTemplate".
 * The plugin invokes PegasusDataTemplateGenerator to generate data template source (.java) files
 * for all data schema (.pdsc) files present in the input source set's pegasus
 * directory, e.g. "src/main/pegasus". The generated data template source (.java) files
 * will be in the new target source set's java source directory, e.g.
 * "src/mainGeneratedDataTemplate/java". The dataTemplateGenerator configuration
 * specifies the classpath for loading PegasusDataTemplateGenerator. In addition to
 * the data schema (.pdsc) files in the pegasus directory, the dataModel configuration
 * specifies resolver path for the PegasusDataTemplateGenerator. The resolver path
 * provides the data schemas and previously generated data template classes that
 * may be referenced by the input source set's data schemas. In most cases, the dataModel
 * configuration should contain data template jars.
 * </p>
 *
 * <p>
 * The next phase is the data template compilation phase, the plugin compiles the generated
 * data template source (.java) files into class files. The dataTemplateCompile configuration
 * specifies the pegasus jars needed to compile these classes. The compileClasspath of the
 * target source set is a composite of the dataModel configuration which includes the data template
 * classes that were previously generated and included in the dependent data template jars,
 * and the dataTemplateCompile configuration.
 * This configuration should specify a dependency on the Pegasus data jar.
 * </p>
 *
 * <p>
 * The following phase is creating the the data model jar and the data template jar.
 * This plugin creates the data model jar that includes the contents of the
 * input source set's pegasus directory, and sets the jar file's classification to
 * "data-model". Hence, the resulting jar file's name should end with "-data-model.jar".
 * It adds the data model jar as an artifact to the dataModel configuration.
 * This jar file should only contain data schema (.pdsc) files.
 * </p>
 *
 * <p>
 * This plugin also create the data template jar that includes the contents of the input
 * source set's pegasus directory and the java class output directory of the
 * target source set. It sets the jar file's classification to "data-template".
 * Hence, the resulting jar file's name should end with "-data-template.jar".
 * It adds the data template jar file as an artifact to the dataTemplate configuration.
 * This jar file contains both data schema (.pdsc) files and generated data template
 * class (.class) files.
 * </p>
 *
 * <p>
 * This plugin will ensure that data template source files are generated before
 * compiling the input source set and before the idea and eclipse tasks. It
 * also adds the generated classes to the compileClasspath of the input source set.
 * </p>
 *
 * <p>
 * The configurations that apply to generating the data model and data template jars
 * are as follow:
 * <ul>
 *   <li>
 *     The dataTemplateGenerator configuration specifies the classpath for
 *     PegasusDataTemplateGenerator. In most cases, it should be the Pegasus generator jar.
 *   </li>
 *   <li>
 *     The dataTemplateCompile configuration specifies the classpath for compiling
 *     the generated data template source (.java) files. In most cases,
 *     it should be the Pegasus data jar.
 *     (The default compile configuration is not used for compiling data templates because
 *     it is not desirable to include non data template dependencies in the data template jar.)
 *     The configuration should not directly include data template jars. Data template jars
 *     should be included in the dataModel configuration.
 *   </li>
 *   <li>
 *     The dataModel configuration provides the value of the "generator.resolver.path"
 *     system property that is passed to PegasusDataTemplateGenerator. In most cases,
 *     this configuration should contain only data template jars. The data template jars
 *     contain both data schema (.pdsc) files and generated data template (.class) files.
 *     PegasusDataTemplateGenerator will not generate data template (.java) files for
 *     classes that can be found in the resolver path. This avoids redundant generation
 *     of the same classes, and inclusion of these classes in multiple jars.
 *     The dataModel configuration is also used to publish the data model jar which
 *     contains only data schema (.pdsc) files.
 *   </li>
 *   <li>
 *     The testDataModel configuration is similar to the dataModel configuration
 *     except it is used when generating data templates from test source sets.
 *     It extends from the dataModel configuration. It is also used to publish
 *     the data model jar from test source sets.
 *   </li>
 *   <li>
 *     The dataTemplate configuration is used to publish the data template
 *     jar which contains both data schema (.pdsc) files and the data template class
 *     (.class) files generated from these data schema (.pdsc) files.
 *   </li>
 *   <li>
 *     The testDataTemplate configuration is similar to the dataTemplate configuration
 *     except it is used when publishing the data template jar files generated from
 *     test source sets.
 *   </li>
 * </ul>
 * </p>
 *
 * <p>Performs the following functions:</p>
 *
 * <p><b>Generate avro schema jars for each source set.</b></p>
 *
 * <p><i>Overview:</i></p>
 *
 * <p>
 * In the api project, the task 'avroSchemaGenerator' generates the avro schema (.avsc)
 * files from pegasus schema (.pdsc) files. In general, data schema files should exist
 * only in api projects.
 * </p>
 *
 * <p>
 * Configure the server and client implementation projects to depend on the
 * api project's avroSchema configuration to get access to the generated avro schemas 
 * from within these projects.
 * </p>
 *
 * <p>
 * This plugin also create the avro schema jar that includes the contents of the input
 * source set's avro directory and the avsc schema files.
 * The resulting jar file's name should end with "-avro-schema.jar".
 * </p>
 *
 * <p><b>Generate rest model and rest client jars for each source set.</b></p>
 *
 * <p><i>Overview:</i></p>
 *
 * <p>
 * In the api project, generates rest client source (.java) files from the idl,
 * compiles the rest client source (.java) files to rest client class (.class) files
 * and puts them in jar files. In general, the api project should be only place that
 * contains the publishable idl files. If the published idl changes an existing idl
 * in the api project, the plugin will emit message indicating this has occurred and
 * suggest that the entire project be rebuilt if it is desirable for clients of the
 * idl to pick up the newly published changes.
 * </p>
 *
 * <p>
 * In the impl project, generates the idl (.restspec.json) files from the input
 * source set's resource class files, then compares them against the existing idl
 * files in the api project for compatibility checking. If incompatible changes are
 * found, the build fails (unless certain flag is specified, see below). If the
 * generated idl passes compatibility checks (see compatibility check levels below),
 * publishes the generated idl (.restspec.json) to the api project.
 * </p>
 *
 * <p><i>Detail:</i></p>
 *
 * <p><b>rest client generation phase</b>: in api project</p>
 *
 * <p>
 * In this phase, the rest client source (.java) files are generated from the
 * api project idl (.restspec.json) files using RestRequestBuilderGenerator.
 * The generated rest client source files will be in the new target source set's
 * java source directory, e.g. "src/mainGeneratedRest/java". The restTools
 * configuration specifies the classpath for loading RestRequestBuilderGenerator.
 * </p>
 *
 * <p>
 * RestRequestBuilderGenerator requires access to the data schemas referenced
 * by the idl. The dataModel configuration specifies the resolver path needed
 * by RestRequestBuilderGenerator to access the data schemas referenced by
 * the idl that is not in the source set's pegasus directory.
 * This plugin automatically includes the data schema (.pdsc) files in the
 * source set's pegasus directory in the resolver path.
 * In most cases, the dataModel configuration should contain data template jars.
 * The data template jars contains both data schema (.pdsc) files and generated
 * data template class (.class) files. By specifying data template jars instead
 * of data model jars, redundant generation of data template classes is avoided
 * as classes that can be found in the resolver path are not generated.
 * </p>
 *
 * <p><b>rest client compilation phase</b>: in api project</p>
 *
 * <p>
 * In this phase, the plugin compiles the generated rest client source (.java)
 * files into class files. The restClientCompile configuration specifies the
 * pegasus jars needed to compile these classes. The compile classpath is a
 * composite of the dataModel configuration which includes the data template
 * classes that were previously generated and included in the dependent data template
 * jars, and the restClientCompile configuration.
 * This configuration should specify a dependency on the Pegasus restli-client jar.
 * </p>
 *
 * <p>
 * The following stage is creating the the rest model jar and the rest client jar.
 * This plugin creates the rest model jar that includes the
 * generated idl (.restspec.json) files, and sets the jar file's classification to
 * "rest-model". Hence, the resulting jar file's name should end with "-rest-model.jar".
 * It adds the rest model jar as an artifact to the restModel configuration.
 * This jar file should only contain idl (.restspec.json) files.
 * </p>
 *
 * <p>
 * This plugin also create the rest client jar that includes the generated
 * idl (.restspec.json) files and the java class output directory of the
 * target source set. It sets the jar file's classification to "rest-client".
 * Hence, the resulting jar file's name should end with "-rest-client.jar".
 * It adds the rest client jar file as an artifact to the restClient configuration.
 * This jar file contains both idl (.restspec.json) files and generated rest client
 * class (.class) files.
 * </p>
 *
 * <p><b>idl generation phase</b>: in server implementation project</p>
 *
 * <p>
 * Before entering this phase, the plugin will ensure that generating idl will
 * occur after compiling the input source set. It will also ensure that IDEA
 * and Eclipse tasks runs after  rest client source (.java) files are generated.
 * </p>
 *
 * <p>
 * In this phase, the plugin creates a new target source set for the generated files.
 * The new target source set's name is the input source set name's* suffixed with
 * "GeneratedRest", e.g. "mainGeneratedRest". The plugin invokes
 * RestLiResourceModelExporter to generate idl (.restspec.json) files for each
 * IdlItem in the input source set's pegasus IdlOptions. The generated idl files
 * will be in target source set's idl directory, e.g. "src/mainGeneratedRest/idl".
 * For example, the following adds an IdlItem to the source set's pegasus IdlOptions.
 * This line should appear in the impl project's build.gradle. If no IdlItem is added,
 * this source set will be excluded from generating idl and checking idl compatibility,
 * even there are existing idl files.
 * <pre>
 *   pegasus.main.idlOptions.addIdlItem(["com.linkedin.restli.examples.groups.server"])
 * </pre>
 * </p>
 *
 * <p>
 * After the idl generation phase, each included idl file is checked for compatibility against
 * those in the api project. In case the current interface breaks compatibility,
 * by default the build fails and reports all compatibility errors and warnings. Otherwise,
 * the build tasks in the api project later will package the resource classes into jar files.
 * User can change the compatibility requirement between the current and published idl by
 * setting the "rest.model.compatibility" project property, i.e.
 * "ligradle -Prest.model.compatibility=<strategy> ..." The following levels are supported:
 * <ul>
 *   <li><b>off</b>: idl compatibility check will not be performed at all. This is only suggested to be
 *   used in test environments.</li>
 *   <li><b>ignore</b>: idl compatibility check will occur but its result will be ignored.
 *   The result will be aggregated and printed at the end of the build.</li>
 *   <li><b>backwards</b>: build fails if there are backwards incompatible changes in idl.
 *   Build continues if there are only compatible changes.</li>
 *   <li><b>equivalent (default)</b>: build fails if there is any functional changes (compatible or
 *   incompatible) in the current idl. Only docs and comments are allowed to be different.</li>
 * </ul>
 * The plugin needs to know where the api project is. It searches the api project in the
 * following steps. If all searches fail, the build fails.
 * <ol>
 *   <li>
 *     Use the specified project from the impl project build.gradle file. The <i>ext.apiProject</i>
 *     property explicitly assigns the api project. E.g.
 *     <pre>
 *       ext.apiProject = project(':groups:groups-server-api')
 *     </pre>
 *     If multiple such statements exist, the last will be used. Wrong project path causes Gradle
 *     evaluation error.
 *   </li>
 *   <li>
 *     If no <i>ext.apiProject</i> property is defined, the plugin will try to guess the
 *     api project name with the following conventions. The search stops at the first successful match.
 *     <ol>
 *       <li>
 *         If the impl project name ends with the following suffixes, substitute the suffix with "-api".
 *           <ol>
 *             <li>-impl</li>
 *             <li>-service</li>
 *             <li>-server</li>
 *             <li>-server-impl</li>
 *           </ol>
 *         This list can be overridden by inserting the following line to the project build.gradle:
 *         <pre>
 *           ext.apiProjectSubstitutionSuffixes = ['-new-suffix-1', '-new-suffix-2']
 *         </pre>
 *         Alternatively, this setting could be applied globally to all projects by putting it in
 *         the <i>subprojects</i> section of the root build.gradle.</b>
 *       </li>
 *       <li>
 *         Append "-api" to the impl project name.
 *       </li>
 *     </ol>
 *   </li>
 * </ol>
 * The plugin invokes RestLiResourceModelCompatibilityChecker to check compatibility.
 * </p>
 *
 * <p>
 * The idl files in the api project are not generated by the plugin, but rather
 * "published" from the impl project. The publishRestModel task is used to copy the
 * idl files to the api project. This task is invoked automatically if the idls are
 * verified to be "safe". "Safe" is determined by the "rest.model.compatibility"
 * property. Because this task is skipped if the idls are functionally equivalent
 * (not necessarily identical, e.g. differ in doc fields), if the default "equivalent"
 * compatibility level is used, no file will be copied. If such automatic publishing
 * is intended to be skip, set the "rest.model.skipPublish" property to true.
 * Note that all the properties are per-project and can be overridden in each project's
 * build.gradle file.
 * </p>
 *
 * <p>
 * Please always keep in mind that if idl publishing is happened, a subsequent whole-project
 * rebuild is necessary to pick up the changes. Otherwise, the Hudson job will fail and
 * the source code commit will fail.
 * </p>
 *
 * <p>
 * The configurations that apply to generating the rest model and rest client jars
 * are as follow:
 * <ul>
 *   <li>
 *     The restTools configuration specifies the classpath for
 *     RestRequestBuilderGenerator, RestLiResourceModelExporter and
 *     RestLiResourceModelCompatibilityChecker.
 *     In most cases, it should be the Pegasus restli-tools jar.
 *   </li>
 *   <li>
 *     The restClientCompile configuration specifies the classpath for compiling
 *     the generated rest client source (.java) files. In most cases,
 *     it should be the Pegasus restli-client jar.
 *     (The default compile configuration is not used for compiling rest client because
 *     it is not desirable to include non rest client dependencies, such as
 *     the rest server implementation classes, in the data template jar.)
 *     The configuration should not directly include data template jars. Data template jars
 *     should be included in the dataModel configuration.
 *   </li>
 *   <li>
 *     The dataModel configuration provides the value of the "generator.resolver.path"
 *     system property that is passed to RestRequestBuilderGenerator.
 *     This configuration should contain only data template jars. The data template jars
 *     contain both data schema (.pdsc) files and generated data template (.class) files.
 *     The RestRequestBuilderGenerator will only generate rest client classes.
 *     The dataModel configuration is also included in the compile classpath for the
 *     generated rest client source files. The dataModel configuration does not
 *     include generated data template classes, then the Java compiler may not able to
 *     find the data template classes referenced by the generated rest client.
 *   </li>
 *   <li>
 *     The testDataModel configuration is similar to the dataModel configuration
 *     except it is used when generating rest client source files from
 *     test source sets.
 *   </li>
 *   <li>
 *     The restModel configuration is used to publish the rest model jar
 *     which contains generated idl (.restspec.json) files.
 *   </li>
 *   <li>
 *     The testRestModel configuration is similar to the restModel configuration
 *     except it is used to publish rest model jar files generated from
 *     test source sets.
 *   </li>
 *   <li>
 *     The restClient configuration is used to publish the rest client jar
 *     which contains both generated idl (.restspec.json) files and
 *     the rest client class (.class) files generated from from these
 *     idl (.restspec.json) files.
 *   </li>
 *   <li>
 *     The testRestClient configuration is similar to the restClient configuration
 *     except it is used to publish rest client jar files generated from
 *     test source sets.
 *   </li>
 * </ul>
 * </p>
 *
 * <p>
 * This plugin considers test source sets whose names begin with 'test' or 'integTest' to be
 * test source sets.
 * </p>
 */

class PegasusGeneratorV2Plugin implements Plugin<Project> {

  Project project

  void apply(Project project) {
    this.project = project
    Clock.measure(project, "${getClass().simpleName}.apply") {
      applyTo(project)
    }
  }

  void afterEvaluate(Closure cl) {
    project.afterEvaluate(Clock.measuredAction(project, "${getClass().getSimpleName()}.afterEvaluate", cl))
  }
  public static boolean debug = false

  //
  // Constants for generating sourceSet names and corresponding directory names
  // for generated code
  //
  private final static String DATA_TEMPLATE_GEN_TYPE = 'DataTemplate'
  private final static String REST_GEN_TYPE = 'Rest'
  private final static String AVRO_SCHEMA_GEN_TYPE = 'AvroSchema'

  private final static String DATA_TEMPLATE_FILE_SUFFIX = '.pdsc'
  private final static String IDL_FILE_SUFFIX = '.restspec.json'
  private final static String TEST_DIR_REGEX = '^(integ)?[Tt]est'

  private final static String IDL_COMPAT_REQUIREMENT_NAME = 'rest.model.compatibility'
  private final static String IDL_NO_PUBLISH = 'rest.model.noPublish'

  private static boolean _runOnce = false

  private static StringBuilder _idlCompatMessage = new StringBuilder()
  private static StringBuilder _idlPublishReminder = new StringBuilder()

  void applyTo(Project project) {
    project.plugins.apply(JavaPlugin.class)

    // this HashMap will have a PegasusOptions per sourceSet
    project.ext.set('pegasus', new HashMap<String, PegasusOptions>())

    project.convention.plugins.templateGen = new PegasusGeneratorConvention()

    // this apply() function will be called for each project
    // the code in the block must run only once
    if (!_runOnce)
    {
      project.gradle.projectsEvaluated { Gradle gradle ->
        runOnceAllProjects(gradle)
      }

      project.gradle.buildFinished { BuildResult result ->
        final StringBuilder endOfBuildMessage = new StringBuilder()

        if (_idlCompatMessage.length() > 0)
        {
          endOfBuildMessage.append(_idlCompatMessage)
        }

        if (_idlPublishReminder.length() > 0)
        {
          endOfBuildMessage.append(_idlPublishReminder)
        }

        if (endOfBuildMessage.length() > 0)
        {
          result.gradle.rootProject.logger.quiet(endOfBuildMessage.toString())
        }
      }

      _runOnce = true
    }

    //
    // configuration for running data template generator
    //
    def dataTemplateGeneratorConfig = project.configurations.add('dataTemplateGenerator')

    //
    // configuration for running rest client generator
    //
    def restToolsConfig = project.configurations.add('restTools')

    //
    // configuration for running Avro schema generator
    //
    def avroSchemaGeneratorConfig = project.configurations.add('avroSchemaGenerator')

    //
    // configuration for compiling generated data templates
    //
    def dataTemplateCompileConfig = project.configurations.add('dataTemplateCompile')

    //
    // configuration for compiling generated rest clients
    //
    def restClientCompileConfig = project.configurations.add('restClientCompile')

    //
    // configuration for depending on data schemas and potentially generated data templates
    // and for publishing jars containing data schemas to the project artifacts for including in the ivy.xml
    //
    def dataModelConfig = project.configurations.add('dataModel')
    def testDataModelConfig = project.configurations.add('testDataModel') {
      extendsFrom(dataModelConfig)
    }

    //
    // configuration for depending on data schemas and potentially generated data templates
    // and for publishing jars containing data schemas to the project artifacts for including in the ivy.xml
    //
    def avroSchemaConfig = project.configurations.add('avroSchema')
    def testAvroSchemaConfig = project.configurations.add('testAvroSchema') {
      extendsFrom(avroSchemaConfig)
    }

    //
    // configuration for depending on rest idl and potentially generated client builders
    // and for publishing jars containing rest idl to the project artifacts for including in the ivy.xml
    //
    def restModelConfig = project.configurations.add('restModel')
    def testRestModelConfig = project.configurations.add('testRestModel') {
      extendsFrom(restModelConfig)
    }

    //
    // configuration for publishing jars containing data schemas and generated data templates
    // to the project artifacts for including in the ivy.xml
    //
    def dataTemplateConfig = project.configurations.add('dataTemplate')
    def testDataTemplateConfig = project.configurations.add('testDataTemplate') {
      extendsFrom(dataTemplateConfig)
    }

    //
    // configuration for publishing jars containing rest idl and generated client builders
    // to the project artifacts for including in the ivy.xml
    //
    def restClientConfig = project.configurations.add('restClient')
    def testRestClientConfig = project.configurations.add('testRestClient') {
      extendsFrom(restClientConfig)
    }

    //
    // published data template jars depends on the configurations used to compile the classes
    // in the jar, this includes the data models/templates used by the data template generator
    // and the classes used to compile the generated classes.
    //
    dataTemplateConfig.extendsFrom(dataTemplateCompileConfig)
    dataTemplateConfig.extendsFrom(dataModelConfig)
    testDataTemplateConfig.extendsFrom(testDataModelConfig)

    //
    // published client builder jars depends on the configurations used to compile the classes
    // in the jar, this includes the data models/templates (potentially generated by this
    // project and) used by the data template generator and the classes used to compile
    // the generated classes.
    //
    restClientConfig.extendsFrom(restClientCompileConfig)
    restClientConfig.extendsFrom(dataTemplateConfig)
    testRestClientConfig.extendsFrom(testDataTemplateConfig)

    project.sourceSets.all { SourceSet sourceSet ->

      if (sourceSet.name =~ '[Gg]enerated') {
        return
      }

      // determine whether running in avro or pegasus model
      def mode = determineMode(project, sourceSet)

      // the idl Generator input options will be inside the PegasusOptions class. Users of the
      // plugin can set the inputOptions in their build.gradle
      project.pegasus[sourceSet.name] = new PegasusOptions()
      project.pegasus[sourceSet.name].mode = mode

      // rest model generation could fail on incompatibility
      // if it can fail, fail it early
      configureRestModelGeneration(project, sourceSet)

      configureDataTemplateGeneration(project, sourceSet)

      configureAvroSchemaGeneration(project, sourceSet)

      configureRestClientGeneration(project, sourceSet)

      Task cleanGeneratedDirTask = project.task(sourceSet.getTaskName('clean', 'GeneratedDir')) << {
        project.delete(getGeneratedSourceDirName(project, sourceSet, DATA_TEMPLATE_GEN_TYPE))
        project.delete(getGeneratedSourceDirName(project, sourceSet, REST_GEN_TYPE))
        project.delete(getGeneratedSourceDirName(project, sourceSet, AVRO_SCHEMA_GEN_TYPE))
      }
      // make clean depends on deleting the generated directories
      project.tasks.clean.dependsOn(cleanGeneratedDirTask)
    }

    /*
    project.tasks.withType(GenerateDataTemplate) {
      project.tasks.idea.dependsOn(it)
      project.tasks.eclipse.dependsOn(it)
    }
    project.tasks.withType(GenerateRestClient) {
      project.tasks.idea.dependsOn(it)
      project.tasks.eclipse.dependsOn(it)
    }
    */
  }

  private static void runOnceAllProjects(Gradle gradle)
  {
    for (Project proj: gradle.rootProject.subprojects)
    {
      if (!proj.hasProperty('sourceSets'))
      {
        continue
      }

      // find api project here instead of in each project's plugin configuration
      // this allows api project relation options (ext.api*) to be specified anywhere in the build.gradle file
      // alternatively, pass closures to task configuration, and evaluate the closures when task is executed
      final Project apiProject = getApiProject(proj)
      if (apiProject != null && !apiProject.plugins.hasPlugin(PegasusGeneratorV2Plugin.class))
      {
        
        apiProject = null
      }

      proj.sourceSets.all { SourceSet sourceSet ->
        final Task generateRestModelTask = proj.tasks.findByPath(sourceSet.getTaskName('generate', 'restModel'))
        if (generateRestModelTask == null)
        {
          return
        }

        if (generateRestModelTask.idlOptions.idlItems.empty)
        {
          return
        }
        else if (apiProject == null)
        {
          throw new Exception("${proj.path}: idl files are generated but no api project is assigned for this project.")
        }

        // rest model publishing involves cross-project reference
        // configure after all projects have been evaluated

        // publish the generated rest model to api project
        // it always does compatibility check before copying files
        // the file copy can be turned off by "rest.model.noPublish" flag
        final Task publishRestModelTask = proj.task(sourceSet.getTaskName('publish', 'restModel'),
                                                    type: PublishIdl,
                                                    dependsOn: generateRestModelTask) {
          from getSuffixedFiles(project, generateRestModelTask.destinationDir, IDL_FILE_SUFFIX)
          // apiIdlDir is annotated as @InputDirectory, whose existence is validated before executing the task
          into apiProject.mkdir(getIdlRelativePath(sourceSet))
          resolverPath = apiProject.files(getDataSchemaRelativePath(project, sourceSet)) + getDataModelConfig(apiProject, sourceSet)
          toolsClasspath = proj.configurations.restTools
        }

        // tasks which declare no output should always assume outputs UP-TO-DATE
        publishRestModelTask.outputs.upToDateWhen { true }

        final Task jarTask = proj.tasks[sourceSet.getTaskName('', 'jar')]
        final SourceSet restSourceSet = proj.sourceSets.findByName(getGeneratedSourceSetName(sourceSet, REST_GEN_TYPE))
        if(restSourceSet != null) {
          jarTask.from(restSourceSet.resources) // add any .restspec.json files as resources to the jar
        }
        jarTask.dependsOn(publishRestModelTask)
      }
    }
  }

  private static addGeneratedDir(Project project, SourceSet sourceSet, Collection<Configuration> configurations)
  {
    // stupid if block needed because of stupid assignment required to update source dirs
    /*if (isTestSourceSet(sourceSet))
    {
      Set<File> sourceDirs = project.ideaModule.module.testSourceDirs
      sourceDirs.addAll(sourceSet.java.srcDirs)
      // this is stupid but assignment is required
      project.ideaModule.module.testSourceDirs = sourceDirs
      if (debug) System.out.println("Added ${sourceSet.java.srcDirs} to project.ideaModule.module.testSourceDirs ${project.ideaModule.module.testSourceDirs}")
    }
    else
    {
      Set<File> sourceDirs = project.ideaModule.module.sourceDirs
      sourceDirs.addAll(sourceSet.java.srcDirs)
      // this is stupid but assignment is required
      project.ideaModule.module.sourceDirs = sourceDirs
      if (debug) System.out.println("Added ${sourceSet.java.srcDirs} to project.ideaModule.module.sourceDirs ${project.ideaModule.module.sourceDirs}")
    }
    Collection compilePlus = project.ideaModule.module.scopes.COMPILE.plus
    compilePlus.addAll(configurations)
    project.ideaModule.module.scopes.COMPILE.plus = compilePlus
    */
  }

  private static PegasusOptions.Mode determineMode(Project project, SourceSet sourceSet)
  {
    def sourceDir = "src${File.separatorChar}${sourceSet.name}"
    def avroSourceDir = project.file("$sourceDir${File.separatorChar}avro")
    if (avroSourceDir.exists()) {
      project.logger.lifecycle("${project.name}'s ${sourceDir} has avro directory, pegasus plugin does not process avro directory")
    }
    return Mode.PEGASUS
  }

  private static String getDataSchemaRelativePath(Project project, SourceSet sourceSet)
  {
    if (project.hasProperty('overridePegasusDir') && project.overridePegasusDir) {
      return project.overridePegasusDir
    }
    return "src${File.separatorChar}${sourceSet.name}${File.separatorChar}pegasus"
  }

  private static String getIdlRelativePath(SourceSet sourceSet)
  {
    return "src${File.separatorChar}${sourceSet.name}${File.separatorChar}idl"
  }

  private static FileTree getSuffixedFiles(Project project, Object baseDir, String suffix)
  {
    return project.fileTree(dir: baseDir, includes: ["**${File.separatorChar}*${suffix}".toString()]);
  }

  private static boolean isTestSourceSet(SourceSet sourceSet)
  {
    return (boolean)(sourceSet.name =~ TEST_DIR_REGEX)
  }

  private static Configuration getDataModelConfig(Project project, SourceSet sourceSet)
  {
    return (isTestSourceSet(sourceSet) ? project.configurations.testDataModel : project.configurations.dataModel)
  }

  private void configureRestModelGeneration(Project project, SourceSet sourceSet)
  {
    if (sourceSet.allJava.empty)
    {
      project.logger.info("No Java file is found for sourceSet " + sourceSet.name)
      return
    }

    // generate the rest model
    final Task generateRestModelTask = project.task(sourceSet.getTaskName('generate', 'restModel'),
                                                    type: GenerateIdl,
                                                    dependsOn: project.tasks[sourceSet.getCompileJavaTaskName()]) {
      inputDirs = sourceSet.java.srcDirs
      // we need all the artifacts from runtime for any private implementation classes the server
      // code might need.
      classpath = project.configurations.runtime + sourceSet.runtimeClasspath
      generatorClasspath = project.configurations.restTools
      destinationDir = project.file(getGeneratedSourceDirName(project, sourceSet, REST_GEN_TYPE) + File.separatorChar + 'idl')
      idlOptions = project.pegasus[sourceSet.name].idlOptions
    }
  }

  private void configureAvroSchemaGeneration(Project project, SourceSet sourceSet)
  {
    final File dataSchemaDir = project.file(getDataSchemaRelativePath(project, sourceSet))
    final FileTree dataSchemaFiles = getSuffixedFiles(project, dataSchemaDir, DATA_TEMPLATE_FILE_SUFFIX)

    if (dataSchemaFiles.empty)
    {
      project.logger.info("Skipping configuration of avro schema generation in $project because there are no data schema files.")
      return
    }

    // generate avro schema files from data schema
    File avroDir = project.file(getGeneratedSourceDirName(project, sourceSet, AVRO_SCHEMA_GEN_TYPE) + File.separatorChar + 'avro')
    final Task generateAvroSchemaTask = project.task(sourceSet.getTaskName('generate', 'avroSchema'), type: GenerateAvroSchema) {
      inputDir = dataSchemaDir
      classpath = project.configurations.avroSchemaGenerator
      destinationDir = avroDir
      inputDataSchemaFiles = dataSchemaFiles
      resolverPath = getDataModelConfig(project, sourceSet)
    }

    project.tasks[sourceSet.compileJavaTaskName].dependsOn(generateAvroSchemaTask)

    // create avro schema jar file

    Task avroSchemaJarTask = project.task(sourceSet.name + 'AvroSchemaJar', type: Jar) {
      // add path prefix to each file in the data schema directory
      from (avroDir) {
        eachFile {
          it.path = 'avro' + File.separatorChar + it.path.toString()
        }
      }
      appendix = getAppendix(sourceSet, 'avro-schema')
      description = 'Generate an avro schema jar'
    }

    if (!isTestSourceSet(sourceSet))
    {
      project.artifacts {
        avroSchema avroSchemaJarTask
      }
    }
    else
    {
      project.artifacts {
        testAvroSchema avroSchemaJarTask
      }
    }
  }

  private void configureDataTemplateGeneration(Project project, SourceSet sourceSet)
  {
    final File dataSchemaDir = project.file(getDataSchemaRelativePath(project, sourceSet))
    final FileTree dataSchemaFiles = getSuffixedFiles(project, dataSchemaDir, DATA_TEMPLATE_FILE_SUFFIX)

    Task generateDataTemplatesTask = null
    if (!dataSchemaFiles.empty) {
      // generate data template source files from data schema
      generateDataTemplatesTask = project.task(sourceSet.getTaskName('generate', 'dataTemplate'), type: GenerateDataTemplate) {
        inputDir = dataSchemaDir
        classpath = project.configurations.dataTemplateGenerator
        destinationDir = project.file(getGeneratedSourceDirName(project, sourceSet, DATA_TEMPLATE_GEN_TYPE) + File.separatorChar + 'java')
        inputDataSchemaFiles = dataSchemaFiles
        resolverPath = getDataModelConfig(project, sourceSet)
      }
    }

    if (generateDataTemplatesTask != null) {

      // create new source set for generated java source and class files
      String targetSourceSetName = getGeneratedSourceSetName(sourceSet, DATA_TEMPLATE_GEN_TYPE)
      SourceSet targetSourceSet = project.sourceSets.add(targetSourceSetName) {
        java {
          srcDir "src${File.separatorChar}${targetSourceSetName}${File.separatorChar}java"
        }
        compileClasspath = getDataModelConfig(project, sourceSet) + project.configurations.dataTemplateCompile
      }

      // idea plugin needs to know about new generated java source directory and its dependencies
      addGeneratedDir(project, targetSourceSet, [ getDataModelConfig(project, sourceSet), project.configurations.dataTemplateCompile ])

      // make sure that java source files have been generated before compiling them
      Task[] generateTasks = [ generateDataTemplatesTask ]
      Task compileTargetSourceSetJava = project.tasks[targetSourceSet.compileJavaTaskName]
      generateTasks.each { task ->
        if (task != null) {
          compileTargetSourceSetJava.dependsOn(task)
        }
      }

      // create data model jar file
      Task dataModelJarTask = project.task(sourceSet.name + 'DataModelJar', type: Jar) {
        // add path prefix to each file in the data schema directory
        from (dataSchemaDir) {
          eachFile {
            it.path = 'pegasus' + File.separatorChar + it.path.toString()
          }
          // TODO: exclude the directories, because they aren't being put under pegasus
          // there isn't a clean way to do this currently with gradle, its a bug with gradle
          // because the from iterates through directories and files but the eachFile only
          // iterates through files. Gradle needs to expose something to iterate through both.
        }
        appendix = getAppendix(sourceSet, 'data-model')
        description = 'Generate a data model jar'
      }

      // create data template jar file
      Task dataTemplateJarTask = project.task(sourceSet.name + 'DataTemplateJar',
                                              type: Jar,
                                              dependsOn: targetSourceSet.compileJavaTaskName) {
        from (dataSchemaDir) {
          eachFile {
            it.path = 'pegasus' + File.separatorChar + it.path.toString()
          }
          // TODO: exclude the directories, because they aren't being put under pegasus
          // there isn't a clean way to do this currently with gradle, its a bug with gradle
          // because the from iterates through directories and files but the eachFile only
          // iterates through files. Gradle needs to expose something to iterate through both.
        }
        from (targetSourceSet.output)
        appendix = getAppendix(sourceSet, 'data-template')
        description = 'Generate a data template jar'
      }

      // TODO: output Javadoc for generated classes?

      // add the data model and date template jars to the list of project artifacts.
      if (!isTestSourceSet(sourceSet))
      {
        project.artifacts {
          dataModel dataModelJarTask
          dataTemplate dataTemplateJarTask
        }
      }
      else
      {
        project.artifacts {
          testDataModel dataModelJarTask
          testDataTemplate dataTemplateJarTask
        }
      }

      // include additional dependencies into the appropriate configuration used to compile the input source set
      // must include the generated data template classes and their dependencies the configuration
      String compileConfigName = (isTestSourceSet(sourceSet)) ? 'testCompile' : 'compile'
      project.configurations {
        "${compileConfigName}" {
          extendsFrom(getDataModelConfig(project, sourceSet))
          extendsFrom(project.configurations.dataTemplateCompile)
        }
      }
      project.dependencies.add(compileConfigName, new DefaultSelfResolvingDependency(project.files(dataTemplateJarTask.archivePath)))

      if (debug)
      {
        System.out.println('configureDataTemplateGeneration sourceSet ' + sourceSet.name)
        System.out.println('dataTemplateGenerator.allDependencies : ' + project.configurations.dataTemplateGenerator.allDependencies)
        System.out.println("${compileConfigName}.allDependenices : " + project.configurations[compileConfigName].allDependencies)
        System.out.println("${compileConfigName}.extendsFrom: " + project.configurations[compileConfigName].extendsFrom)
        System.out.println("${compileConfigName}.transitive: " + project.configurations[compileConfigName].transitive)
      }

      project.tasks[sourceSet.compileJavaTaskName].dependsOn(dataTemplateJarTask)
    }
  }

  // Generate rest client from idl files generated from java source files in the specified source set.
  //
  // This generates rest client source files from idl file generated from java source files
  // in the source set. The generated rest client source files will be in a new source set.
  // It also compiles the rest client source files into classes, and creates both the
  // rest model and rest client jar files.
  //
  private void configureRestClientGeneration(Project project, SourceSet sourceSet)
  {
    // idl directory for api project
    def idlDir = project.file(getIdlRelativePath(sourceSet))

    // always include imported data template jars in compileClasspath of rest client
    FileCollection dataModels = getDataModelConfig(project, sourceSet)

    // if data templates generated from this source set, add the generated data template jar to compileClasspath
    // of rest client.
    String dataTemplateSourceSetName = getGeneratedSourceSetName(sourceSet, DATA_TEMPLATE_GEN_TYPE)
    if (project.sourceSets.findByName(dataTemplateSourceSetName) != null)
    {
      if (debug) System.out.println("sourceSet ${sourceSet.name} has generated sourceSet ${dataTemplateSourceSetName}")
      def dataTemplateJarTask = project.tasks[sourceSet.name + 'DataTemplateJar']
      dataModels += project.files(dataTemplateJarTask.archivePath)
    }

    // create source set for generated rest model, rest client source and class files.
    String targetSourceSetName = getGeneratedSourceSetName(sourceSet, REST_GEN_TYPE)
    SourceSet targetSourceSet = project.sourceSets.add(targetSourceSetName) {
      java {
        srcDir "src${File.separatorChar}${targetSourceSetName}${File.separatorChar}java"
      }
      resources {
        srcDir "src${File.separatorChar}${targetSourceSetName}${File.separatorChar}idl"
      }
      compileClasspath = dataModels + project.configurations.restClientCompile
    }
    project.plugins.withType(EclipsePlugin) {
      project.eclipse.classpath.plusConfigurations += project.configurations.restClientCompile
    }

    // idea plugin needs to know about new rest client source directory and its dependencies
    addGeneratedDir(project, targetSourceSet, [ getDataModelConfig(project, sourceSet), project.configurations.restClientCompile ])

    // generate the rest client source files
    def generateRestClientTask = project.task(targetSourceSet.getTaskName('generate', 'restClient'),
                                              type: GenerateRestClient,
                                              dependsOn: dataModels) {
      clientIdlInput = project.files(idlDir)
      resolverPath = dataModels
      classpath = project.configurations.restTools
      destinationDir = project.file(getGeneratedSourceDirName(project, sourceSet, REST_GEN_TYPE) + File.separatorChar + 'java')
    }

    // make sure rest client source files have been generated before compiling them
    Task compileGeneratedRestClientTask = project.tasks[targetSourceSet.compileJavaTaskName]
    compileGeneratedRestClientTask.dependsOn(generateRestClientTask)

    // create the rest model jar file
    def restModelJarTask = project.task(sourceSet.name + 'RestModelJar', type: Jar) {
      from (idlDir) {
        eachFile {
          project.logger.lifecycle('Add idl file: ' + it.toString() )
          it.path = 'idl' + File.separatorChar + it.path.toString()
        }
        includes = ['*' + IDL_FILE_SUFFIX]
      }
      appendix = getAppendix(sourceSet, 'rest-model')
      description = 'Generate rest model jar'
      // unfortunately, even though this code does what I intended (does not generate the idl jar
      // if the destination dir is empty, that causes a problem in the hudson job (not reproducible
      // in my local env) while trying to compute the checksum for these non-existent jars
      //onlyIf {
      //  project.fileTree(generateRestModelTask.destinationDir).getFiles().size() > 0
      //}
    }

    // create the rest client jar file
    def restClientJarTask = project.task(sourceSet.name + 'RestClientJar',
                                         type: Jar,
                                         dependsOn: compileGeneratedRestClientTask) {
      from (idlDir) {
        eachFile {
          project.logger.lifecycle('Add idl file: ' + it.toString() )
          it.path = 'idl' + File.separatorChar + it.path.toString()
        }
        includes = ['*' + IDL_FILE_SUFFIX]
      }
      from (targetSourceSet.output)
      appendix = getAppendix(sourceSet, 'rest-client')
      description = 'Generate rest client jar'
      // unfortunately, even though this code does what I intended (does not generate the idl jar
      // if the destination dir is empty, that causes a problem in the hudson job (not reproducible
      // in my local env) while trying to compute the checksum for these non-existent jars
      //onlyIf {
      //  project.fileTree(compileGeneratedRestClientTask.destinationDir).getFiles().size() > 0
      //}
    }

    // add the rest model jar and the rest client jar to the list of project artifacts.
    if (!isTestSourceSet(sourceSet))
    {
      project.artifacts {
        restModel restModelJarTask
        restClient restClientJarTask
      }
    }
    else
    {
      project.artifacts {
        testRestModel restModelJarTask
        testRestClient restClientJarTask
      }
    }
  }

  // Compute the name of the source set that will contain a type of an input generated code.
  // e.g. genType may be 'DataTemplate' or 'Rest'
  private static String getGeneratedSourceSetName(SourceSet sourceSet, String genType)
  {
    return "${sourceSet.name}Generated${genType}"
  }

  // Compute the directory name that will contain a type generated code of an input source set.
  // e.g. genType may be 'DataTemplate' or 'Rest'
  private static String getGeneratedSourceDirName(Project project, SourceSet sourceSet, String genType)
  {
    final String sourceSetName = getGeneratedSourceSetName(sourceSet, genType)
    return "${project.projectDir}${File.separatorChar}src${File.separatorChar}${sourceSetName}"
  }

  // Return the appendix for generated jar files.
  // The source set name is not included for the main source set.
  private static String getAppendix(SourceSet sourceSet, String suffix)
  {
    return (sourceSet.name.equals('main') ? suffix : "${sourceSet.name}-${suffix}")
  }

  private static Project getApiProject(Project project)
  {
    if (project.ext.has('apiProject'))
    {
      return project.ext.apiProject
    }

    List subsSuffixes = [ '-impl', '-service', '-server', '-server-impl' ]
    if (project.ext.has('apiProjectSubstitutionSuffixes'))
    {
      subsSuffixes = project.ext.apiProjectSubstitutionSuffixes
    }

    for (String suffix: subsSuffixes)
    {
      if (project.path.endsWith(suffix))
      {
        final String searchPath = project.path.substring(0, project.path.length() - suffix.length()) + '-api'
        final Project apiProject = project.findProject(searchPath)
        if (apiProject != null)
        {
          return apiProject
        }
      }
    }

    return project.findProject(project.path + '-api')
  }

  /**
   * GenerateDataTemplate
   *
   * Generate the data template source files from data schema files.
   *
   * To use this plugin, add these three lines to your build.gradle:
   * <pre>
   * apply plugin: 'pegasus'
   * </pre>
   *
   * The plugin will scan the source set's pegasus directory, e.g. "src/main/pegasus"
   * for data schema (.pdsc) files.
   */
  static class GenerateDataTemplate extends DefaultTask {

    /**
     * Directory to write the generated data template source files.
     */
    @OutputDirectory File destinationDir
    /**
     * Directory containing the data schema files.
     */
    @InputDirectory File inputDir
    /**
     * Classpath for loading the PegasusDataTemplateGenerator class.
     */
    @InputFiles FileCollection classpath
    /**
     * The input arguments to PegasusDataTemplateGenerator class (excludes the destination directory).
     */
    @InputFiles FileTree inputDataSchemaFiles
    /**
     * The resolver path.
     */
    @InputFiles FileCollection resolverPath

    @TaskAction
    protected void generate()
    {
      if (classpath.empty)
      {
        project.logger.warn('There are Pegasus schema files. If you need corresponding data template files, define "dataTemplateGenerator" configuration.')
        return
      }

      project.logger.info('Generating data templates ...')
      destinationDir.mkdirs()
      project.logger.lifecycle("There are ${inputDataSchemaFiles.files.size()} data schema input files. Using input root folder: ${inputDir}")

      URL[] classpathUrls = classpath.collect { it.toURL() } as URL[]
      URLClassLoader classLoader = new URLClassLoader(classpathUrls, null)
      def dataTemplateGenerator = classLoader.loadClass('com.linkedin.pegasus.generator.PegasusDataTemplateGenerator').newInstance()

      final String resolverPathStr = (resolverPath + project.files(inputDir)).collect { it.path }.join(File.pathSeparator)
      System.setProperty('generator.resolver.path', resolverPathStr)
      dataTemplateGenerator.run(destinationDir.path, inputDataSchemaFiles.collect { it.path } as String[])
    }
  }

  /**
   * GenerateAvroSchema
   *
   * Generate the Avro schema (.avsc) files from data schema files.
   *
   * To use this plugin, add these three lines to your build.gradle:
   * <pre>
   * apply plugin: 'pegasus'
   * </pre>
   *
   * The plugin will scan the source set's pegasus directory, e.g. "src/main/pegasus"
   * for data schema (.pdsc) files.
   */
  static class GenerateAvroSchema extends DefaultTask {

    /**
     * Directory to write the generated Avro schema files.
     */
    @OutputDirectory File destinationDir
    /**
     * Directory containing the data schema files.
     */
    @InputDirectory File inputDir
    /**
     * Classpath for loading the AvroSchemaGenerator class.
     */
    @InputFiles FileCollection classpath
    /**
     * The input arguments to AvroSchemaGenerator class (excludes the destination directory).
     */
    @InputFiles FileTree inputDataSchemaFiles
    /**
     * The resolver path.
     */
    @InputFiles FileCollection resolverPath

    @TaskAction
    protected void generate()
    {
      if (classpath.empty)
      {
        project.logger.info('There are Pegasus schema files. If you need corresponding Avro schema files, define "avroSchemaGenerator" configuration.')
        return
      }

      project.logger.info('Generating Avro schemas ...')
      destinationDir.mkdirs()
      project.logger.lifecycle("There are ${inputDataSchemaFiles.files.size()} data schema input files. Using input root folder: ${inputDir}")

      URL[] classpathUrls = classpath.collect { it.toURL() } as URL[]
      URLClassLoader classLoader = new URLClassLoader(classpathUrls, null)
      def avroSchemaGenerator = classLoader.loadClass('com.linkedin.data.avro.generator.AvroSchemaGenerator').newInstance()

      final String resolverPathStr = (resolverPath + project.files(inputDir)).collect { it.path }.join(File.pathSeparator)
      System.setProperty('generator.resolver.path', resolverPathStr)
      avroSchemaGenerator.run(destinationDir.path, inputDataSchemaFiles.collect { it.path } as String[])
    }
  }

  /**
   * GenerateIdl
   *
   * Generate the idl file from the annotated java classes. This also requires access to the
   * classes that were used to compile these java classes.
   * Projects with no IdlItem will be excluded from this task
   *
   * As prerequisite of this task, add these lines to your build.gradle:
   * <pre>
   * apply plugin: 'pegasus'
   * pegasus.&lt;sourceSet&gt;.idlOptions.addIdlItem(['&lt;packageName&gt;'])
   * </pre>
   */
  static class GenerateIdl extends DefaultTask {

    @InputFiles Set<File> inputDirs
    @InputFiles FileCollection classpath
    @InputFiles FileCollection generatorClasspath
    @OutputDirectory File destinationDir
    PegasusOptions.IdlOptions idlOptions

    @TaskAction
    protected void generate()
    {
      // runOnceAllProjects() should exclude projects with empty IdlItems
      assert(!idlOptions.idlItems.empty)

      if (generatorClasspath.empty)
      {
        throw new Exception('Missing restTools configuration.')
      }

      final String[] inputDirPaths = inputDirs.collect { it.path }
      project.logger.debug("GenerateIdl using directories ${inputDirPaths}")
      project.logger.debug("IdlGeneratorCompile using destination dir ${destinationDir.path}")

      // handle multiple idl generations in the same project, see pegasus rest-framework-server-examples
      // for example.
      // [<packageName>] is the array of packages that should be searched for annotated java classes.
      //
      // pegasus.<sourceSet>.idlOptions.addIdlItem(['<packageName>'])
      // for example:
      // pegasus.main.idlOptions.addIdlItem(['com.linkedin.groups.server.rest.impl', 'com.linkedin.greetings.server.rest.impl'])
      // They will still be placed in the same jar, though

      final ClassLoader prevContextClassLoader = Thread.currentThread().contextClassLoader
      final URL[] classpathUrls = (classpath + generatorClasspath).collect { it.toURI().toURL() } as URL[]
      final URLClassLoader classLoader = new URLClassLoader(classpathUrls, null)
      Thread.currentThread().contextClassLoader = classLoader

      final def idlGenerator = classLoader.loadClass('com.linkedin.restli.tools.idlgen.RestLiResourceModelExporter').newInstance()
      for (PegasusOptions.IdlItem idlItem: idlOptions.idlItems)
      {
        final String apiName = idlItem.apiName
        if (apiName.length() == 0)
        {
          project.logger.info('Generating idl for unnamed api ...')
        }
        else
        {
          project.logger.info("Generating idl for api: ${apiName} ...")
        }

        // RestLiResourceModelExporter will load classes from the passed packages
        // we need to add the classpath to the thread's context class loader
        idlGenerator.export(apiName, classpathUrls as String[], inputDirPaths, idlItem.packageNames, destinationDir.path)
      }

      Thread.currentThread().contextClassLoader = prevContextClassLoader
    }
  }

  /**
   * PublishIdl
   *
   * Check idl compatibility between current project and the api project.
   * If check succeeds and not equivalent, copy all idl files to the api project.
   * This task overwrites existing api idl files.
   *
   * As prerequisite of this task, the api project needs to be designated. There are multiple ways to do this.
   * Please refer to the documentation section for detail.
   */
  static class PublishIdl extends Copy
  {
    @InputFiles FileCollection resolverPath
    @InputFiles FileCollection toolsClasspath

    @Override
    protected void copy()
    {
      if (source.empty)
      {
        throw new Exception('No idl file is found. Skip publishing idl.')
      }

      if (check())
      {
        project.logger.info('idl files are equivalent. No need to publish')
        return
      }

      if (project.hasProperty(PegasusGeneratorV2Plugin.IDL_NO_PUBLISH) && Boolean.valueOf(PegasusGeneratorV2Plugin.IDL_NO_PUBLISH.property(property).toString()))
      {
        return
      }

      project.logger.lifecycle('Publishing idl to api project ...')

      final FileTree apiIdlFiles = PegasusGeneratorV2Plugin.getSuffixedFiles(project, destinationDir, PegasusGeneratorV2Plugin.IDL_FILE_SUFFIX)
      final int apiIdlFileCount = apiIdlFiles.files.size()

      super.copy()

      // FileTree is lazily evaluated, so that it scans for files only when the contents of the file tree are queried
      if (apiIdlFileCount != 0 && apiIdlFileCount != apiIdlFiles.files.size())
      {
        project.logger.warn('idl file count changed after publish. You may have duplicate idl with different filenames.')
      }

      if (PegasusGeneratorV2Plugin._idlPublishReminder.length() == 0)
      {
        PegasusGeneratorV2Plugin._idlPublishReminder.append("\n")
                                                    .append("idl files have been changed during the build. You must run the following command line at project root to pick up the changes:\n")
                                                    .append("  ligradle build\n")
      }
    }

    /**
     * @return is idl equivalent?
     */
    private boolean check()
    {
      if (toolsClasspath.empty)
      {
        throw new Exception('Missing restTools configuration.')
      }

      final URL[] classpathUrls = toolsClasspath.collect { it.toURL() } as URL[]
      final URLClassLoader classLoader = new URLClassLoader(classpathUrls, null)
      final Class<? extends Enum> compatLevelClass;
      final boolean isNewVersion;

      try
      {
        compatLevelClass = classLoader.loadClass('com.linkedin.restli.tools.idlcheck.CompatibilityLevel').asSubclass(Enum.class)
        isNewVersion = true
      }
      catch (ClassNotFoundException)
      {
        compatLevelClass = IDLCompatLevel.class;
        isNewVersion = false
      }
      assert(compatLevelClass.isEnum())

      final Enum idlCompatLevel
      if (project.hasProperty(PegasusGeneratorV2Plugin.IDL_COMPAT_REQUIREMENT_NAME))
      {
        try
        {
          idlCompatLevel = Enum.valueOf(compatLevelClass, project.property(PegasusGeneratorV2Plugin.IDL_COMPAT_REQUIREMENT_NAME).toString().toUpperCase())
        }
        catch (IllegalArgumentException e)
        {
          throw new Exception("Unrecognized idl compatibility level property.", e)
        }
      }
      else
      {
        idlCompatLevel = compatLevelClass.getEnumConstants().last()
      }

      if (idlCompatLevel.ordinal() == 0)
      {
        project.logger.info('idl compatiblity checking is turned off.')
        return false
      }

      project.logger.info('Checking idl compatibility with api ...')

      final Class<?> compatCheckerClass = classLoader.loadClass('com.linkedin.restli.tools.idlcheck.RestLiResourceModelCompatibilityChecker')
      final String resolverPathStr = resolverPath.collect { it.path }.join(File.pathSeparator)
      System.setProperty('generator.resolver.path', resolverPathStr)

      final StringBuilder allCheckMessage = new StringBuilder()
      boolean isIdlCompatible = true

      source.each {
        project.logger.info('Checking idl file: ' + it.path)

        final String apiIdlFilePath = "${destinationDir.path}${File.separatorChar}${it.name}"
        final def idlChecker = compatCheckerClass.newInstance()

        if (isNewVersion)
        {
          isIdlCompatible &= idlChecker.check(apiIdlFilePath, it.path, idlCompatLevel)
          allCheckMessage.append(idlChecker.summary)
        }
        else
        {
          idlChecker.check(apiIdlFilePath, it.path)
          final def unableToChecks = idlChecker.unableToChecks
          final def incompatibles = idlChecker.incompatibles
          final def compatibles = idlChecker.compatibles

          final StringBuilder fileSummaryBuilder = new StringBuilder()
          ['Unable to checks': unableToChecks, 'Incompatible changes': incompatibles, 'Compatible changes': compatibles].each { infoName, infoArray ->
            if (!infoArray.empty)
            {
              fileSummaryBuilder.append(infoName + ':\n')
              infoArray.eachWithIndex { info, i ->
                fileSummaryBuilder.append("  ${i+1}) ${info}\n")
              }
            }
          }

          if (fileSummaryBuilder.length() > 0)
          {
            allCheckMessage.append("\nidl compatibility report between published \"${apiIdlFilePath}\" and current \"${it.path}\":\n")
                    .append(fileSummaryBuilder)
            isIdlCompatible &= ((unableToChecks.empty || idlCompatLevel.ordinal() < IDLCompatLevel.BACKWARDS.ordinal()) &&
                    (incompatibles.empty || idlCompatLevel.ordinal() < IDLCompatLevel.BACKWARDS.ordinal()) &&
                    (compatibles.empty || idlCompatLevel.ordinal() < IDLCompatLevel.EQUIVALENT.ordinal()))
          }
        }
      }

      if (allCheckMessage.length() == 0)
      {
        return true
      }

      allCheckMessage.append("\n")
                     .append("You may add \"-P${PegasusGeneratorV2Plugin.IDL_COMPAT_REQUIREMENT_NAME}=backwards\" to the build command to allow backwards compatible changes in idl.\n")
                     .append("You may use \"-P${PegasusGeneratorV2Plugin.IDL_COMPAT_REQUIREMENT_NAME}=ignore\" to ignore idl compatibility errors.\n")
                     .append("You can ignore the \"Unable to check\" errors if the idl file has never been published.")

      if (!isIdlCompatible)
      {
        throw new Exception(allCheckMessage.toString())
      }
      else
      {
        PegasusGeneratorV2Plugin._idlCompatMessage.append(allCheckMessage)
      }

      return false
    }

    private enum IDLCompatLevel
    {
      OFF,
      IGNORE,
      BACKWARDS,
      EQUIVALENT
    }
  }

  /**
   * GenerateRestClient
   *
   * This task will generate the rest client source files.
   *
   * As pre-requisite of this task,, add these lines to your build.gradle:
   * <pre>
   * apply plugin: 'pegasus'
   * pegasus.<sourceSet>.clientOptions.addClientItem('<restModelFilePath>', '<defaultPackage>', <keepDataTemplates>)
   * </pre>
   * keepDataTemplates is a boolean that isn't used right now, but might be implemented in the future.
   *
   * dependencies (for other projects in the same multi-product):
   * <pre>
   * dependencies {
   *   dataModel project(path: ':janus:janus-impl', configuration: 'dataTemplate')
   *   restModel project(path: ':janus:janus-impl', configuration: 'restModel')
   *   restTools spec.product.pegasus.restliTools
   * }
   * </pre>
   * (may need other deps)
   */
  static class GenerateRestClient extends DefaultTask {

    @InputFiles FileCollection clientIdlInput
    @InputFiles FileCollection resolverPath
    @InputFiles FileCollection classpath
    @OutputDirectory File destinationDir

    @TaskAction
    protected void generate()
    {
      PegasusOptions.ClientOptions pegasusClientOptions = new PegasusOptions.ClientOptions()

      // idl input could include rest model jar files
      clientIdlInput.each { input ->
        if (input.isDirectory())
        {
          for (File f: PegasusGeneratorV2Plugin.getSuffixedFiles(project, input, PegasusGeneratorV2Plugin.IDL_FILE_SUFFIX))
          {
            if (!pegasusClientOptions.hasRestModelFileName(f.name))
            {
              pegasusClientOptions.addClientItem(f.name, '', false)
              project.logger.lifecycle("Add idl file: ${f.path}")
            }
          }
        }
      }
      if (pegasusClientOptions.clientItems.empty)
      {
        return
      }

      if (classpath.empty)
      {
        project.logger.warn('There are idl files. If you need REST client builders, define "restTools" configuration.')
        return
      }

      project.logger.info('Generating REST client builders ...')

      String tempDirStr = temporaryDir.toString() + File.separatorChar + 'clientInput'
      File tempDir = new File(tempDirStr)

      // unjar the artifact that contains the idl
      clientIdlInput.each { input ->
        if (input.isDirectory())
        {
          project.copy {
            from input
            into new File(tempDirStr, 'idl')
          }
        }
        else
        {
          project.logger.lifecycle("Input idl jar: ${input}")
          project.copy {
            from project.zipTree(input)
            into tempDir
          }
        }
      }

      final String resolverPathStr = (resolverPath + project.files("$tempDirStr${File.separatorChar}pegasus"))
              .collect { it.path }.join(File.pathSeparator)
      System.setProperty('generator.resolver.path', resolverPathStr)
      System.setProperty('generator.rest.generate.datatemplates', 'false')

      final URL[] classpathUrls = classpath.collect { it.toURL() } as URL[]
      final URLClassLoader classLoader = new URLClassLoader(classpathUrls, null)
      def stubGenerator = classLoader.loadClass('com.linkedin.restli.tools.clientgen.RestRequestBuilderGenerator').newInstance()

      destinationDir.mkdirs()
      for (PegasusOptions.ClientItem clientItem: pegasusClientOptions.clientItems)
      {
        project.logger.lifecycle("Generating rest client source files for: ${clientItem.restModelFileName}")
        project.logger.lifecycle("Destination directory: ${destinationDir}")
        project.logger.lifecycle("Classpath: ${classpath.files}")

        final String restModelFilePath = "${tempDirStr}${File.separatorChar}idl${File.separatorChar}${clientItem.restModelFileName}";
        System.setProperty('generator.default.package', clientItem.defaultPackage)
        stubGenerator.run(destinationDir.path, restModelFilePath)
      }
    }
  }
}
