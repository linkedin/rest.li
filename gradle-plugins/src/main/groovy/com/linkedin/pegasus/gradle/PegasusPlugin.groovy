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


import com.linkedin.pegasus.gradle.tasks.ChangedFileReportTask
import com.linkedin.pegasus.gradle.tasks.CheckIdlTask
import com.linkedin.pegasus.gradle.tasks.CheckRestModelTask
import com.linkedin.pegasus.gradle.tasks.CheckSnapshotTask
import com.linkedin.pegasus.gradle.tasks.GenerateAvroSchemaTask
import com.linkedin.pegasus.gradle.tasks.GenerateDataTemplateTask
import com.linkedin.pegasus.gradle.tasks.GenerateRestClientTask
import com.linkedin.pegasus.gradle.tasks.GenerateRestModelTask
import com.linkedin.pegasus.gradle.tasks.PublishRestModelTask
import org.gradle.BuildResult
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.idea.IdeaPlugin


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
 * "src/mainGeneratedDataTemplate/java". In addition to
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
 * In the api project, the task 'generateAvroSchema' generates the avro schema (.avsc)
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
 * java source directory, e.g. "src/mainGeneratedRest/java".
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
 * "gradle -Prest.model.compatibility=<strategy> ..." The following levels are supported:
 * <ul>
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

class PegasusPlugin implements Plugin<Project>
{
  public static boolean debug = false

  //
  // Constants for generating sourceSet names and corresponding directory names
  // for generated code
  //
  private static final String DATA_TEMPLATE_GEN_TYPE = 'DataTemplate'
  private static final String REST_GEN_TYPE = 'Rest'
  private static final String AVRO_SCHEMA_GEN_TYPE = 'AvroSchema'

  public static final String DATA_TEMPLATE_FILE_SUFFIX = '.pdsc'
  public static final String IDL_FILE_SUFFIX = '.restspec.json'
  public static final String SNAPSHOT_FILE_SUFFIX = '.snapshot.json'
  public static final String SNAPSHOT_COMPAT_REQUIREMENT = 'rest.model.compatibility'
  public static final String IDL_COMPAT_REQUIREMENT = 'rest.idl.compatibility'

  private static final String TEST_DIR_REGEX = '^(integ)?[Tt]est'
  private static final String SNAPSHOT_NO_PUBLISH = 'rest.model.noPublish'
  private static final String IDL_NO_PUBLISH = 'rest.idl.noPublish'
  private static final String SKIP_IDL_CHECK = 'rest.idl.skipCheck'
  private static final String SUPPRESS_REST_CLIENT_RESTLI_2 = 'rest.client.restli2.suppress'

  private static final String GENERATOR_CLASSLOADER_NAME = 'pegasusGeneratorClassLoader'

  private static boolean _runOnce = false

  private static final StringBuffer _restModelCompatMessage = new StringBuffer()
  private static final Collection<String> _needCheckinFiles = new ArrayList<String>()
  private static final Collection<String> _needBuildFolders = new ArrayList<String>()
  private static final Collection<String> _possibleMissingFilesInEarlierCommit = new ArrayList<String>()

  private static final Object STATIC_PROJECT_EVALUATED_LOCK = new Object()

  private Class<? extends Plugin> _thisPluginType = getClass().asSubclass(Plugin)
  private Task _generateSourcesJarTask = null
  private Task _generateJavadocTask = null
  private Task _generateJavadocJarTask = null

  void setPluginType(Class<? extends Plugin> pluginType)
  {
    _thisPluginType = pluginType
  }

  void setSourcesJarTask(Task sourcesJarTask)
  {
    _generateSourcesJarTask = sourcesJarTask
  }

  void setJavadocJarTask(Task javadocJarTask)
  {
    _generateJavadocJarTask = javadocJarTask
  }

  @Override
  void apply(Project project)
  {
    project.plugins.apply(JavaPlugin)
    project.plugins.apply(IdeaPlugin)
    project.plugins.apply(EclipsePlugin)

    // this HashMap will have a PegasusOptions per sourceSet
    project.ext.set('pegasus', new HashMap<String, PegasusOptions>())
    // this map will extract PegasusOptions.GenerationMode to project property
    project.ext.set('PegasusGenerationMode', PegasusOptions.GenerationMode.values().collectEntries {[it.name(), it]})

    synchronized (STATIC_PROJECT_EVALUATED_LOCK)
    {
      if (!_runOnce)
      {
        project.gradle.projectsEvaluated { Gradle gradle ->
          gradle.rootProject.subprojects { Project subproject ->
            ['dataTemplateGenerator', 'restTools', 'avroSchemaGenerator'].each { String configurationName ->
              final Configuration conf = subproject.configurations.findByName(configurationName)
              if (conf != null && !conf.isEmpty())
              {
                subproject.getLogger().warn('*** Project ' + subproject.path + ' declares dependency to unused configuration "' + configurationName + '". This configuration is deprecated and you can safely remove the dependency. ***')
              }
            }
          }
        }

        project.gradle.buildFinished { BuildResult result ->
          final StringBuilder endOfBuildMessage = new StringBuilder()

          if (_restModelCompatMessage.length() > 0)
          {
            endOfBuildMessage.append(_restModelCompatMessage)
          }

          if (_needCheckinFiles.size() > 0)
          {
            endOfBuildMessage.append(createModifiedFilesMessage(_needCheckinFiles, _needBuildFolders))
          }

          if (_possibleMissingFilesInEarlierCommit.size() > 0)
          {
            endOfBuildMessage.append(createPossibleMissingFilesMessage(_possibleMissingFilesInEarlierCommit))
          }

          if (endOfBuildMessage.length() > 0)
          {
            result.gradle.rootProject.logger.quiet(endOfBuildMessage.toString())
          }
        }

        _runOnce = true
      }
    }

    project.configurations {
      // configuration for getting the required classes to make pegasus call main methods
      pegasusPlugin {}

      // configuration for compiling generated data templates
      dataTemplateCompile {
        visible = false
      }

      // configuration for running rest client generator
      restClientCompile {
        visible = false
      }

      // configuration for running data template generator
      // DEPRECATED! This configuration is no longer used. Please stop using it.
      dataTemplateGenerator {
        visible = false
      }

      // configuration for running rest client generator
      // DEPRECATED! This configuration is no longer used. Please stop using it.
      restTools {
        visible = false
      }

      // configuration for running Avro schema generator
      // DEPRECATED! To skip avro schema generation, use PegasusOptions.generationModes
      avroSchemaGenerator {
        visible = false
      }

      // configuration for depending on data schemas and potentially generated data templates
      // and for publishing jars containing data schemas to the project artifacts for including in the ivy.xml
      dataModel
      testDataModel {
        extendsFrom dataModel
      }

      // configuration for depending on data schemas and potentially generated data templates
      // and for publishing jars containing data schemas to the project artifacts for including in the ivy.xml
      avroSchema
      testAvroSchema {
        extendsFrom avroSchema
      }

      // configuration for publishing jars containing data schemas and generated data templates
      // to the project artifacts for including in the ivy.xml
      //
      // published data template jars depends on the configurations used to compile the classes
      // in the jar, this includes the data models/templates used by the data template generator
      // and the classes used to compile the generated classes.
      dataTemplate {
        extendsFrom dataTemplateCompile
        extendsFrom dataModel
      }
      testDataTemplate {
        extendsFrom dataTemplate
        extendsFrom testDataModel
      }

      // configuration for publishing jars containing rest idl and generated client builders
      // to the project artifacts for including in the ivy.xml
      //
      // published client builder jars depends on the configurations used to compile the classes
      // in the jar, this includes the data models/templates (potentially generated by this
      // project and) used by the data template generator and the classes used to compile
      // the generated classes.
      restClient {
        extendsFrom restClientCompile
        extendsFrom dataTemplate
      }
      testRestClient {
        extendsFrom restClient
        extendsFrom testDataTemplate
      }
    }

    Properties properties = new Properties();
    InputStream inputStream = getClass().getResourceAsStream("/pegasus-version.properties");
    if (null != inputStream)
    {
      properties.load(inputStream);
      String version = properties.getProperty("pegasus.version");

      project.dependencies.add('pegasusPlugin', 'com.linkedin.pegasus:data:' + version)
      project.dependencies.add('pegasusPlugin', 'com.linkedin.pegasus:data-avro-generator:' + version)
      project.dependencies.add('pegasusPlugin', 'com.linkedin.pegasus:generator:' + version)
      project.dependencies.add('pegasusPlugin', 'com.linkedin.pegasus:restli-tools:' + version)
    }
    else
    {
      project.logger.lifecycle(
          "Unable to add pegasus dependencies to {}. Please be sure that " + "'com.linkedin.pegasus:data', 'com.linkedin.pegasus:data-avro-generator', 'com.linkedin.pegasus:generator', 'com.linkedin.pegasus:restli-tools'" + " are available on the configuration pegasusPlugin",
          project.getPath())
    }
    project.dependencies.add('pegasusPlugin', 'org.slf4j:slf4j-simple:1.7.2')

    // this call has to be here because:
    // 1) artifact cannot be published once projects has been evaluated, so we need to first
    // create the tasks and artifact handler, then progressively append sources
    // 2) in order to append sources progressively, the source and documentation tasks and artifacts must be
    // configured/created before configuring and creating the code generation tasks.

    configureGeneratedSourcesAndJavadoc(project)

    final ChangedFileReportTask changedFileReportTask = project.tasks.create('changedFilesReport',
                                                                             ChangedFileReportTask)
    project.tasks.check.dependsOn(changedFileReportTask)

    project.sourceSets.all { SourceSet sourceSet ->

      if (sourceSet.name =~ '[Gg]enerated')
      {
        return
      }

      checkAvroSchemaExist(project, sourceSet)

      // the idl Generator input options will be inside the PegasusOptions class. Users of the
      // plugin can set the inputOptions in their build.gradle
      project.pegasus[sourceSet.name] = new PegasusOptions()

      // rest model generation could fail on incompatibility
      // if it can fail, fail it early
      configureRestModelGeneration(project, sourceSet)

      configureDataTemplateGeneration(project, sourceSet)

      configureAvroSchemaGeneration(project, sourceSet)

      configureRestClientGeneration(project, sourceSet)

      Task cleanGeneratedDirTask = project.task(sourceSet.getTaskName('clean', 'GeneratedDir')) << {
        deleteGeneratedDir(project, sourceSet, REST_GEN_TYPE)
        deleteGeneratedDir(project, sourceSet, AVRO_SCHEMA_GEN_TYPE)
        deleteGeneratedDir(project, sourceSet, DATA_TEMPLATE_GEN_TYPE)
      }
      // make clean depends on deleting the generated directories
      project.tasks.clean.dependsOn(cleanGeneratedDirTask)
    }

    project.ext.set(GENERATOR_CLASSLOADER_NAME, this.class.classLoader)
  }

  protected void configureGeneratedSourcesAndJavadoc(Project project)
  {
    _generateJavadocTask = project.task('generateJavadoc', type: Javadoc)

    if (_generateSourcesJarTask == null)
    {
      //
      // configuration for publishing jars containing sources for generated classes
      // to the project artifacts for including in the ivy.xml
      //
      project.configurations {
        generatedSources
        testGeneratedSources {
          extendsFrom generatedSources
        }
      }

      _generateSourcesJarTask = project.task('generateSourcesJar', type: Jar) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = 'Generates a jar file containing the sources for the generated Java classes.'

        classifier = 'sources'
      }

      project.artifacts {
        generatedSources _generateSourcesJarTask
      }
    }

    if (_generateJavadocJarTask == null)
    {
      //
      // configuration for publishing jars containing Javadoc for generated classes
      // to the project artifacts for including in the ivy.xml
      //
      project.configurations {
        generatedJavadoc
        testGeneratedJavadoc {
          extendsFrom generatedJavadoc
        }
      }

      _generateJavadocJarTask = project.task('generateJavadocJar', type: Jar, dependsOn: _generateJavadocTask) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = 'Generates a jar file containing the Javadoc for the generated Java classes.'

        classifier = 'javadoc'
        from _generateJavadocTask.destinationDir
      }

      project.artifacts {
        generatedJavadoc _generateJavadocJarTask
      }
    }
    else
    {
      _generateJavadocJarTask.from(_generateJavadocTask.destinationDir)
      _generateJavadocJarTask.dependsOn(_generateJavadocTask)
    }
  }

  private static void deleteGeneratedDir(Project project, SourceSet sourceSet, String dirType)
  {
    final String generatedDirPath = getGeneratedDirPath(project, sourceSet, dirType)
    project.logger.info("Delete generated directory ${generatedDirPath}")
    project.delete(generatedDirPath)
  }

  private static Class<? extends Enum> getCompatibilityLevelClass(Project project)
  {
    final ClassLoader generatorClassLoader = (ClassLoader) project.property(GENERATOR_CLASSLOADER_NAME)
    final Class<? extends Enum> compatLevelClass =
      generatorClassLoader.loadClass('com.linkedin.restli.tools.idlcheck.CompatibilityLevel').asSubclass(Enum.class)
    return compatLevelClass;
  }

  private static addGeneratedDir(Project project, SourceSet sourceSet, Collection<Configuration> configurations)
  {
    // stupid if block needed because of stupid assignment required to update source dirs
    if (isTestSourceSet(sourceSet))
    {
      Set<File> sourceDirs = project.ideaModule.module.testSourceDirs
      sourceDirs.addAll(sourceSet.java.srcDirs)
      // this is stupid but assignment is required
      project.ideaModule.module.testSourceDirs = sourceDirs
      if (debug)
      {
        System.out.println("Added ${sourceSet.java.srcDirs} to project.ideaModule.module.testSourceDirs ${project.ideaModule.module.testSourceDirs}")
      }
    }
    else
    {
      Set<File> sourceDirs = project.ideaModule.module.sourceDirs
      sourceDirs.addAll(sourceSet.java.srcDirs)
      // this is stupid but assignment is required
      project.ideaModule.module.sourceDirs = sourceDirs
      if (debug)
      {
        System.out.println("Added ${sourceSet.java.srcDirs} to project.ideaModule.module.sourceDirs ${project.ideaModule.module.sourceDirs}")
      }
    }
    Collection compilePlus = project.ideaModule.module.scopes.COMPILE.plus
    compilePlus.addAll(configurations)
    project.ideaModule.module.scopes.COMPILE.plus = compilePlus
  }

  private static void checkAvroSchemaExist(Project project, SourceSet sourceSet)
  {
    final String sourceDir = "src${File.separatorChar}${sourceSet.name}"
    final File avroSourceDir = project.file("${sourceDir}${File.separatorChar}avro")
    if (avroSourceDir.exists())
    {
      project.logger.lifecycle("${project.name}'s ${sourceDir} has non-empty avro directory. pegasus plugin does not process avro directory")
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
  private static String getGeneratedDirPath(Project project, SourceSet sourceSet, String genType)
  {
    final String override = getOverridePath(project, sourceSet, 'overrideGeneratedDir')
    final String sourceSetName = getGeneratedSourceSetName(sourceSet, genType)
    final String base
    if (override == null)
    {
      base = 'src'
    }
    else
    {
      base = override
    }

    return "${base}${File.separatorChar}${sourceSetName}"
  }

  private static String getDataSchemaPath(Project project, SourceSet sourceSet)
  {
    final String override = getOverridePath(project, sourceSet, 'overridePegasusDir')
    if (override == null)
    {
      return "src${File.separatorChar}${sourceSet.name}${File.separatorChar}pegasus"
    }
    else
    {
      return override
    }
  }

  private static String getSnapshotPath(Project project, SourceSet sourceSet)
  {
    final String override = getOverridePath(project, sourceSet, 'overrideSnapshotDir')
    if (override == null)
    {
      return "src${File.separatorChar}${sourceSet.name}${File.separatorChar}snapshot"
    }
    else
    {
      return override
    }
  }

  private static String getIdlPath(Project project, SourceSet sourceSet)
  {
    final String override = getOverridePath(project, sourceSet, 'overrideIdlDir')
    if (override == null)
    {
      return "src${File.separatorChar}${sourceSet.name}${File.separatorChar}idl"
    }
    else
    {
      return override
    }
  }

  private static String getOverridePath(Project project, SourceSet sourceSet, String overridePropertyName)
  {
    final String sourceSetPropertyName = "${sourceSet.name}.${overridePropertyName}"
    String override = getNonEmptyProperty(project, sourceSetPropertyName)

    if (override == null && sourceSet.name.equals('main'))
    {
      override = getNonEmptyProperty(project, overridePropertyName)
    }

    return override
  }

  private static FileTree getSuffixedFiles(Project project, Object baseDir, String suffix)
  {
    return SharedFileUtils.getSuffixedFiles(project, baseDir, suffix);
  }

  private static boolean isTestSourceSet(SourceSet sourceSet)
  {
    return (boolean) (sourceSet.name =~ TEST_DIR_REGEX)
  }

  private static Configuration getDataModelConfig(Project project, SourceSet sourceSet)
  {
    return (isTestSourceSet(sourceSet) ? project.configurations.testDataModel : project.configurations.dataModel)
  }

  private static boolean isTaskSuccessful(Task task)
  {
    return task.state.executed && !task.state.skipped && task.state.failure == null
  }

  protected void configureRestModelGeneration(Project project, SourceSet sourceSet)
  {
    if (sourceSet.allSource.empty)
    {
      project.logger.info("No source files found for sourceSet " + sourceSet.name + ".  Skipping idl generation.")
      return
    }

    // afterEvaluate needed so that api project can be overridden via ext.apiProject
    project.afterEvaluate {
      // find api project here instead of in each project's plugin configuration
      // this allows api project relation options (ext.api*) to be specified anywhere in the build.gradle file
      // alternatively, pass closures to task configuration, and evaluate the closures when task is executed
      Project apiProject = getCheckedApiProject(project)

      // make sure the api project is evaluated. Important for configure-on-demand mode.
      if (apiProject)
      {
        project.evaluationDependsOn(apiProject.path)
      }

      if (apiProject && !apiProject.plugins.hasPlugin(_thisPluginType))
      {
        apiProject = null
      }

      if (apiProject == null)
      {
        return
      }

      final Task jarTask = project.tasks.findByName(sourceSet.getJarTaskName())
      if (jarTask == null || !(jarTask instanceof Jar))
      {
        return
      }

      final String snapshotCompatPropertyName = findProperty(FileCompatibilityType.SNAPSHOT)
      if (project.hasProperty(snapshotCompatPropertyName) && 'off'.equalsIgnoreCase((String) project.property(snapshotCompatPropertyName)))
      {
        project.logger.lifecycle("Project ${project.path} snapshot compatibility level \"OFF\" is deprecated. Default to \"IGNORE\".")
      }

      // generate the rest model
      final String destinationDirPrefix = getGeneratedDirPath(project, sourceSet, REST_GEN_TYPE) + File.separatorChar
      final FileCollection restModelResolverPath = apiProject.files(getDataSchemaPath(project, sourceSet)) + getDataModelConfig(apiProject, sourceSet)

      final Task generateRestModelTask = project.task(sourceSet.getTaskName('generate', 'restModel'),
                                                      type: GenerateRestModelTask) {
        dependsOn project.tasks[sourceSet.getClassesTaskName()]
        inputDirs = sourceSet.allSource.srcDirs - sourceSet.resources.srcDirs
        // we need all the artifacts from runtime for any private implementation classes the server code might need.
        snapshotDestinationDir = project.file(destinationDirPrefix + 'snapshot')
        idlDestinationDir = project.file(destinationDirPrefix + 'idl')
        idlOptions = project.pegasus[sourceSet.name].idlOptions
        resolverPath = restModelResolverPath
        codegenClasspath = project.configurations.pegasusPlugin + project.configurations.runtime + sourceSet.runtimeClasspath

        doFirst {
          deleteGeneratedDir(project, sourceSet, REST_GEN_TYPE)
        }
      }

      final File apiSnapshotDir = apiProject.file(getSnapshotPath(apiProject, sourceSet))
      final File apiIdlDir = apiProject.file(getIdlPath(apiProject, sourceSet))
      apiSnapshotDir.mkdirs()
      if (!isPropertyTrue(project, SKIP_IDL_CHECK))
      {
        apiIdlDir.mkdirs();
      }

      final CheckRestModelTask checkRestModelTask = project.task(sourceSet.getTaskName('check', 'RestModel'),
                                                                 type: CheckRestModelTask,
                                                                 dependsOn: generateRestModelTask) {
        currentSnapshotFiles = SharedFileUtils.getSnapshotFiles(project, destinationDirPrefix)
        previousSnapshotDirectory = apiSnapshotDir
        currentIdlFiles = SharedFileUtils.getIdlFiles(project, destinationDirPrefix)
        previousIdlDirectory = apiIdlDir
        codegenClasspath = project.configurations.pegasusPlugin

        onlyIf {
          !isPropertyTrue(project, SKIP_IDL_CHECK)
        }
      }

      final CheckSnapshotTask checkSnapshotTask = project.task(sourceSet.getTaskName('check', 'Snapshot'),
                                                               type: CheckSnapshotTask,
                                                               dependsOn: generateRestModelTask) {
        currentSnapshotFiles = SharedFileUtils.getSnapshotFiles(project, destinationDirPrefix)
        previousSnapshotDirectory = apiSnapshotDir
        codegenClasspath = project.configurations.pegasusPlugin

        onlyIf {
          isPropertyTrue(project, SKIP_IDL_CHECK)
        }
      }

      final CheckIdlTask checkIdlTask = project.task(sourceSet.getTaskName('check', 'Idl'),
                                                     type: CheckIdlTask,
                                                     dependsOn: generateRestModelTask) {
        currentIdlFiles = SharedFileUtils.getIdlFiles(project, destinationDirPrefix)
        previousIdlDirectory = apiIdlDir
        resolverPath = restModelResolverPath
        codegenClasspath = project.configurations.pegasusPlugin

        onlyIf {
          !isPropertyTrue(project, SKIP_IDL_CHECK) && PropertyUtil.findCompatLevel(project, FileCompatibilityType.IDL) != 'OFF'
        }
      }

      // rest model publishing involves cross-project reference
      // configure after all projects have been evaluated
      // the file copy can be turned off by "rest.model.noPublish" flag
      final Task publishRestliSnapshotTask = project.task(sourceSet.getTaskName('publish', 'RestliSnapshot'),
                                                          type: PublishRestModelTask,
                                                          dependsOn: [checkRestModelTask, checkSnapshotTask, checkIdlTask]) {
        from SharedFileUtils.getSnapshotFiles(project, destinationDirPrefix)
        into apiSnapshotDir
        suffix = SNAPSHOT_FILE_SUFFIX

        onlyIf {
          project.logger.info("IDL_NO_PUBLISH: " + isPropertyTrue(project, IDL_NO_PUBLISH) + "\n" +
                              "SNAPSHOT_NO_PUBLISH: " + isPropertyTrue(project, SNAPSHOT_NO_PUBLISH) + "\n" +
                              "checkRestModelTask:" +
                              " Executed: " + checkRestModelTask.state.executed +
                              ", Not Skipped: " + !checkRestModelTask.state.skipped +
                              ", No Failure: " + (checkRestModelTask.state.failure == null) +
                              ", Is Not Equivalent: " + !checkRestModelTask.isEquivalent + "\n" +
                              "checkSnapshotTask:" +
                              " Executed: " + checkSnapshotTask.state.executed +
                              ", Not Skipped: " + !checkSnapshotTask.state.skipped +
                              ", No Failure: " + (checkSnapshotTask.state.failure == null) +
                              ", Is Not Equivalent: " + !checkSnapshotTask.isEquivalent + "\n")

          !isPropertyTrue(project, SNAPSHOT_NO_PUBLISH) &&
          (
            (
               isPropertyTrue(project, SKIP_IDL_CHECK) &&
               isTaskSuccessful(checkSnapshotTask) &&
               !(checkSnapshotTask.isEquivalent)
            ) ||
            (
              !isPropertyTrue(project, SKIP_IDL_CHECK) &&
              isTaskSuccessful(checkRestModelTask) &&
              !(checkRestModelTask.isEquivalent)
            )
          )
        }
      }

      final Task publishRestliIdlTask = project.task(sourceSet.getTaskName('publish', 'RestliIdl'),
                                                     type: PublishRestModelTask,
                                                     dependsOn: [checkRestModelTask, checkIdlTask, checkSnapshotTask]) {
        from SharedFileUtils.getIdlFiles(project, destinationDirPrefix)
        into apiIdlDir
        suffix = IDL_FILE_SUFFIX

        onlyIf {
          project.logger.info("SKIP_IDL: " + isPropertyTrue(project, SKIP_IDL_CHECK) + "\n" +
                              "IDL_NO_PUBLISH: " + isPropertyTrue(project, IDL_NO_PUBLISH) + "\n" +
                              "SNAPSHOT_NO_PUBLISH: " + isPropertyTrue(project, SNAPSHOT_NO_PUBLISH) + "\n" +
                              "checkRestModelTask:" +
                              " Executed: " + checkRestModelTask.state.executed +
                              ", Not Skipped: " + !checkRestModelTask.state.skipped +
                              ", No Failure: " + (checkRestModelTask.state.failure == null) +
                              ", Is Not Equivalent: " + !checkRestModelTask.isEquivalent + "\n" +
                              "checkIdlTask:" +
                              " Executed: " + checkIdlTask.state.executed +
                              ", Not Skipped: " + !checkIdlTask.state.skipped +
                              ", No Failure: " + (checkIdlTask.state.failure == null) +
                              ", Is Not Equivalent: " + !checkIdlTask.isEquivalent + "\n" +
                              "checkSnapshotTask:" +
                              " Executed: " + checkSnapshotTask.state.executed +
                              ", Not Skipped: " + !checkSnapshotTask.state.skipped +
                              ", No Failure: " + (checkSnapshotTask.state.failure == null) +
                              ", Is RestSpec Not Equivalent: " + !checkSnapshotTask.isEquivalent + "\n")
          !isPropertyTrue(project, IDL_NO_PUBLISH) &&
          (
            (
               isPropertyTrue(project, SKIP_IDL_CHECK) &&
               isTaskSuccessful(checkSnapshotTask) &&
               !(checkSnapshotTask.isEquivalent)
            ) ||
            (
               !isPropertyTrue(project, SKIP_IDL_CHECK) &&
               (
                  (isTaskSuccessful(checkRestModelTask) && !(checkRestModelTask.isEquivalent)) ||
                  (isTaskSuccessful(checkIdlTask) && !(checkIdlTask.isEquivalent))
               )
            )
          )
        }
      }
      project.logger.info("API project selected for $publishRestliIdlTask.path is $apiProject.path")

      jarTask.from(SharedFileUtils.getIdlFiles(project, destinationDirPrefix))
      // add generated .restspec.json files as resources to the jar
      jarTask.dependsOn(publishRestliSnapshotTask, publishRestliIdlTask)

      ChangedFileReportTask changedFileReportTask = (ChangedFileReportTask) project.getTasks().
          getByName('changedFilesReport')
      changedFileReportTask.idlFiles = SharedFileUtils.getIdlFiles(project, destinationDirPrefix)
      changedFileReportTask.snapshotFiles = SharedFileUtils.getSnapshotFiles(project, destinationDirPrefix)
      changedFileReportTask.mustRunAfter(checkRestModelTask, checkIdlTask, checkSnapshotTask, publishRestliIdlTask)
    }
  }

  protected void configureAvroSchemaGeneration(Project project, SourceSet sourceSet)
  {
    final File dataSchemaDir = project.file(getDataSchemaPath(project, sourceSet))
    final File avroDir = project.file(getGeneratedDirPath(project, sourceSet, AVRO_SCHEMA_GEN_TYPE) + File.separatorChar + 'avro')

    // generate avro schema files from data schema
    final Task generateAvroSchemaTask = project.task(sourceSet.getTaskName('generate', 'avroSchema'),
                                                     type: GenerateAvroSchemaTask) {
      inputDir = dataSchemaDir
      destinationDir = avroDir
      resolverPath = getDataModelConfig(project, sourceSet)
      codegenClasspath = project.configurations.pegasusPlugin

      onlyIf {
        inputDir.exists() && (project.pegasus[sourceSet.name].hasGenerationMode(PegasusOptions.GenerationMode.AVRO) || !project.configurations.avroSchemaGenerator.empty)
      }

      doFirst {
        deleteGeneratedDir(project, sourceSet, AVRO_SCHEMA_GEN_TYPE)
      }
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

  protected void configureDataTemplateGeneration(Project project, SourceSet sourceSet)
  {
    final File dataSchemaDir = project.file(getDataSchemaPath(project, sourceSet))
    final File generatedDataTemplateDir = project.file(getGeneratedDirPath(project, sourceSet, DATA_TEMPLATE_GEN_TYPE) + File.separatorChar + 'java')

    // generate data template source files from data schema
    final Task generateDataTemplatesTask = project.task(sourceSet.getTaskName('generate', 'dataTemplate'),
                                                        type: GenerateDataTemplateTask) {
      inputDir = dataSchemaDir
      destinationDir = generatedDataTemplateDir
      resolverPath = getDataModelConfig(project, sourceSet)
      codegenClasspath = project.configurations.pegasusPlugin

      onlyIf {
        inputDir.exists() && project.pegasus[sourceSet.name].hasGenerationMode(PegasusOptions.GenerationMode.PEGASUS)
      }

      doFirst {
        deleteGeneratedDir(project, sourceSet, DATA_TEMPLATE_GEN_TYPE)
      }
    }

    _generateSourcesJarTask.from(generateDataTemplatesTask.destinationDir)
    _generateSourcesJarTask.dependsOn(generateDataTemplatesTask)

    _generateJavadocTask.source(generateDataTemplatesTask.destinationDir)
    _generateJavadocTask.classpath += project.configurations.dataTemplateCompile + generateDataTemplatesTask.resolverPath
    _generateJavadocTask.dependsOn(generateDataTemplatesTask)

    // create new source set for generated java source and class files
    String targetSourceSetName = getGeneratedSourceSetName(sourceSet, DATA_TEMPLATE_GEN_TYPE)
    SourceSet targetSourceSet = project.sourceSets.create(targetSourceSetName) {
      java {
        srcDir generatedDataTemplateDir
      }
      compileClasspath = getDataModelConfig(project, sourceSet) + project.configurations.dataTemplateCompile
    }

    // idea plugin needs to know about new generated java source directory and its dependencies
    addGeneratedDir(project, targetSourceSet,
                    [getDataModelConfig(project, sourceSet), project.configurations.dataTemplateCompile])

    // make sure that java source files have been generated before compiling them
    final Task compileTask = project.tasks[targetSourceSet.compileJavaTaskName]
    compileTask.dependsOn(generateDataTemplatesTask)

    // create data template jar file
    Task dataTemplateJarTask = project.task(sourceSet.name + 'DataTemplateJar',
                                            type: Jar,
                                            dependsOn: compileTask) {
      from (dataSchemaDir) {
        eachFile {
          it.path = 'pegasus' + File.separatorChar + it.path.toString()
        }
      }
      from (targetSourceSet.output)
      appendix = getAppendix(sourceSet, 'data-template')
      description = 'Generate a data template jar'
    }

    // add the data model and date template jars to the list of project artifacts.
    if (!isTestSourceSet(sourceSet))
    {
      project.artifacts {
        dataTemplate dataTemplateJarTask
      }
    }
    else
    {
      project.artifacts {
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
    project.dependencies.add(compileConfigName, project.files(dataTemplateJarTask.archivePath))

    if (debug)
    {
      System.out.println('configureDataTemplateGeneration sourceSet ' + sourceSet.name)
      System.out.println("${compileConfigName}.allDependenices : " + project.configurations[compileConfigName].allDependencies)
      System.out.println("${compileConfigName}.extendsFrom: " + project.configurations[compileConfigName].extendsFrom)
      System.out.println("${compileConfigName}.transitive: " + project.configurations[compileConfigName].transitive)
    }

    project.tasks[sourceSet.compileJavaTaskName].dependsOn(dataTemplateJarTask)
  }

  // Generate rest client from idl files generated from java source files in the specified source set.
  //
  // This generates rest client source files from idl file generated from java source files
  // in the source set. The generated rest client source files will be in a new source set.
  // It also compiles the rest client source files into classes, and creates both the
  // rest model and rest client jar files.
  //
  protected void configureRestClientGeneration(Project project, SourceSet sourceSet)
  {
    // idl directory for api project
    final File idlDir = project.file(getIdlPath(project, sourceSet))
    if (SharedFileUtils.getSuffixedFiles(project, idlDir, IDL_FILE_SUFFIX).empty)
    {
      return
    }

    final File generatedRestClientDir = project.file(getGeneratedDirPath(project, sourceSet, REST_GEN_TYPE) + File.separatorChar + 'java')

    // always include imported data template jars in compileClasspath of rest client
    FileCollection dataModels = getDataModelConfig(project, sourceSet)

    // if data templates generated from this source set, add the generated data template jar to compileClasspath
    // of rest client.
    String dataTemplateSourceSetName = getGeneratedSourceSetName(sourceSet, DATA_TEMPLATE_GEN_TYPE)
    Task dataTemplateJarTask = null
    if (project.sourceSets.findByName(dataTemplateSourceSetName) != null)
    {
      if (debug)
      {
        System.out.println("sourceSet ${sourceSet.name} has generated sourceSet ${dataTemplateSourceSetName}")
      }
      dataTemplateJarTask = project.tasks[sourceSet.name + 'DataTemplateJar']
      dataModels += project.files(dataTemplateJarTask.archivePath)
    }

    // create source set for generated rest model, rest client source and class files.
    String targetSourceSetName = getGeneratedSourceSetName(sourceSet, REST_GEN_TYPE)
    SourceSet targetSourceSet = project.sourceSets.create(targetSourceSetName) {
      java {
        srcDir generatedRestClientDir
      }
      compileClasspath = dataModels + project.configurations.restClientCompile
    }
    project.plugins.withType(EclipsePlugin) {
      project.eclipse.classpath.plusConfigurations += [project.configurations.restClientCompile]
    }

    // idea plugin needs to know about new rest client source directory and its dependencies
    addGeneratedDir(project, targetSourceSet,
                    [getDataModelConfig(project, sourceSet), project.configurations.restClientCompile])

    // generate the rest client source files
    Task generateRestClientTask = project.task(targetSourceSet.getTaskName('generate', 'restClient'),
                                               type: GenerateRestClientTask, dependsOn: project.configurations.dataTemplate) {
      inputDir = idlDir
      resolverPath = dataModels
      runtimeClasspath = project.configurations.dataModel + project.configurations.dataTemplate.artifacts.files
      codegenClasspath = project.configurations.pegasusPlugin
      destinationDir = generatedRestClientDir
      isRestli2FormatSuppressed = project.hasProperty(SUPPRESS_REST_CLIENT_RESTLI_2)
    }

    if (dataTemplateJarTask != null)
    {
      generateRestClientTask.dependsOn(dataTemplateJarTask)
    }

    _generateSourcesJarTask.from(generateRestClientTask.destinationDir)
    _generateSourcesJarTask.dependsOn(generateRestClientTask)

    _generateJavadocTask.source(generateRestClientTask.destinationDir)
    _generateJavadocTask.classpath += project.configurations.restClientCompile + generateRestClientTask.resolverPath
    _generateJavadocTask.dependsOn(generateRestClientTask)

    // make sure rest client source files have been generated before compiling them
    Task compileGeneratedRestClientTask = project.tasks[targetSourceSet.compileJavaTaskName]
    compileGeneratedRestClientTask.dependsOn(generateRestClientTask)
    compileGeneratedRestClientTask.options.compilerArgs += '-Xlint:-deprecation'

    // create the rest client jar file
    def restClientJarTask = project.task(sourceSet.name + 'RestClientJar',
                                         type: Jar,
                                         dependsOn: compileGeneratedRestClientTask) {
      from (idlDir) {
        eachFile {
          project.logger.lifecycle('Add interface file: ' + it.toString())
          it.path = 'idl' + File.separatorChar + it.path.toString()
        }
        includes = ['*' + IDL_FILE_SUFFIX]
      }
      from (targetSourceSet.output)
      appendix = getAppendix(sourceSet, 'rest-client')
      description = 'Generate rest client jar'
    }

    // add the rest model jar and the rest client jar to the list of project artifacts.
    if (!isTestSourceSet(sourceSet))
    {
      project.artifacts {
        restClient restClientJarTask
      }
    }
    else
    {
      project.artifacts {
        testRestClient restClientJarTask
      }
    }
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

    List subsSuffixes = ['-impl', '-service', '-server', '-server-impl']
    if (project.ext.has('apiProjectSubstitutionSuffixes'))
    {
      subsSuffixes = project.ext.apiProjectSubstitutionSuffixes
    }

    for (String suffix : subsSuffixes)
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

  private static Project getCheckedApiProject(Project project)
  {
    final Project apiProject = getApiProject(project)

    if (apiProject == project)
    {
      throw new GradleException("The API project of ${project.path} must not be itself.")
    }

    return apiProject
  }

  /**
   * return the property value if the property exists and is not empty (-Pname=value)
   * return null if property does not exist or the property is empty (-Pname)
   *
   * @param project the project where to look for the property
   * @param propertyName the name of the property
   */
  public static String getNonEmptyProperty(Project project, String propertyName)
  {
    if (!project.hasProperty(propertyName))
    {
      return null
    }

    final String propertyValue = project.property(propertyName).toString()
    if (propertyValue.empty)
    {
      return null
    }

    return propertyValue
  }

  /**
   * Return true if the given property exists and its value is true
   *
   * @param project the project where to look for the property
   * @param propertyName the name of the property
   */
  public static boolean isPropertyTrue(Project project, String propertyName)
  {
    return project.hasProperty(propertyName) && Boolean.valueOf(project.property(propertyName).toString())
  }

  private static String createModifiedFilesMessage(Collection<String> nonEquivExpectedFiles,
                                                   Collection<String> foldersToBeBuilt)
  {
    StringBuilder builder = new StringBuilder();
    builder.append("\nRemember to checkin the changes to the following new or modified files:\n")
    for (String file : nonEquivExpectedFiles)
    {
      builder.append("  ")
      builder.append(file)
      builder.append("\n")
    }

    if (!foldersToBeBuilt.isEmpty())
    {
      builder.append("\nThe file modifications include service interface changes, you can build the the following projects " + "to re-generate the client APIs accordingly:\n")
      for (String folder : foldersToBeBuilt)
      {
        builder.append("  ")
        builder.append(folder)
        builder.append("\n")
      }
    }

    return builder.toString();
  }

  private static String createPossibleMissingFilesMessage(Collection<String> missingFiles)
  {
    StringBuilder builder = new StringBuilder()
    builder.append("If this is the result of an automated build, then you may have forgotten to check in some snapshot or idl files:\n")
    for (String file : missingFiles)
    {
      builder.append("  ")
      builder.append(file)
      builder.append("\n")
    }

    return builder.toString();
  }

  // returns nothing but modifies the passed in StringBuilder
  private static void finishMessage(Project project, StringBuilder currentMessage, FileCompatibilityType type)
  {
    String property = findProperty(type)
    final Enum compatLevel = findCompatLevel(project, type)
    final Class<? extends Enum> compatLevelClass = getCompatibilityLevelClass(project)

    final StringBuilder endMessage = new StringBuilder("\nThis check was run on compatibility level ${compatLevel}\n")

    if (compatLevel == compatLevelClass.EQUIVALENT)
    {
      endMessage.append("You may add \"-P${property}=backwards\" to the build command to allow backwards compatible changes in interface.\n")
    }
    if (compatLevel == compatLevelClass.BACKWARDS || compatLevel == compatLevelClass.EQUIVALENT)
    {
      endMessage.append("You may use \"-P${property}=ignore\" to ignore compatibility errors.\n")
    }

    endMessage.append("Documentation: https://github.com/linkedin/rest.li/wiki/Snapshots-and-Resource-Compatibility-Checking")

    currentMessage.append(endMessage)
  }

  private static String findProperty(FileCompatibilityType type)
  {
    final String property;
    switch (type)
    {
      case FileCompatibilityType.SNAPSHOT:
        property = SNAPSHOT_COMPAT_REQUIREMENT
        break;
      case FileCompatibilityType.IDL:
        property = IDL_COMPAT_REQUIREMENT
        break
    }
    return property
  }
}
