/*
 * Copyright (c) 2019 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.pegasus.gradle;

import com.linkedin.pegasus.gradle.PegasusOptions.IdlOptions;
import com.linkedin.pegasus.gradle.tasks.ChangedFileReportTask;
import com.linkedin.pegasus.gradle.tasks.CheckIdlTask;
import com.linkedin.pegasus.gradle.tasks.CheckRestModelTask;
import com.linkedin.pegasus.gradle.tasks.CheckSnapshotTask;
import com.linkedin.pegasus.gradle.tasks.GenerateAvroSchemaTask;
import com.linkedin.pegasus.gradle.tasks.GenerateDataTemplateTask;
import com.linkedin.pegasus.gradle.tasks.GenerateRestClientTask;
import com.linkedin.pegasus.gradle.tasks.GenerateRestModelTask;
import com.linkedin.pegasus.gradle.tasks.PublishRestModelTask;
import com.linkedin.pegasus.gradle.tasks.TranslateSchemasTask;
import com.linkedin.pegasus.gradle.tasks.ValidateExtensionSchemaTask;
import com.linkedin.pegasus.gradle.tasks.ValidateSchemaAnnotationTask;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;


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

public class PegasusPlugin implements Plugin<Project>
{
  public static boolean debug = false;

  //
  // Constants for generating sourceSet names and corresponding directory names
  // for generated code
  //
  private static final String DATA_TEMPLATE_GEN_TYPE = "DataTemplate";
  private static final String REST_GEN_TYPE = "Rest";
  private static final String AVRO_SCHEMA_GEN_TYPE = "AvroSchema";

  public static final String DATA_TEMPLATE_FILE_SUFFIX = ".pdsc";
  public static final String PDL_FILE_SUFFIX = ".pdl";
  // gradle property to opt OUT schema annotation validation, by default this feature is enabled.
  private static final String DISABLE_SCHEMA_ANNOTATION_VALIDATION = "schema.annotation.validation.disable";
  // gradle property to opt in for destroying stale files from the build directory,
  // by default it is disabled, because it triggers hot-reload (even if it results in a no-op)
  private static final String DESTROY_STALE_FILES_ENABLE = "enableDestroyStaleFiles";
  public static final Collection<String> DATA_TEMPLATE_FILE_SUFFIXES = new ArrayList<>();

  public static final String IDL_FILE_SUFFIX = ".restspec.json";
  public static final String SNAPSHOT_FILE_SUFFIX = ".snapshot.json";
  public static final String SNAPSHOT_COMPAT_REQUIREMENT = "rest.model.compatibility";
  public static final String IDL_COMPAT_REQUIREMENT = "rest.idl.compatibility";

  private static final Pattern TEST_DIR_REGEX = Pattern.compile("^(integ)?[Tt]est");
  private static final String SNAPSHOT_NO_PUBLISH = "rest.model.noPublish";
  private static final String IDL_NO_PUBLISH = "rest.idl.noPublish";
  private static final String SKIP_IDL_CHECK = "rest.idl.skipCheck";
  private static final String SUPPRESS_REST_CLIENT_RESTLI_2 = "rest.client.restli2.suppress";
  private static final String SUPPRESS_REST_CLIENT_RESTLI_1 = "rest.client.restli1.suppress";

  private static final String GENERATOR_CLASSLOADER_NAME = "pegasusGeneratorClassLoader";

  private static final String CONVERT_TO_PDL_REVERSE = "convertToPdl.reverse";
  private static final String CONVERT_TO_PDL_KEEP_ORIGINAL = "convertToPdl.keepOriginal";
  private static final String CONVERT_TO_PDL_SKIP_VERIFICATION = "convertToPdl.skipVerification";
  private static final String CONVERT_TO_PDL_PRESERVE_SOURCE_CMD = "convertToPdl.preserveSourceCmd";

  // Below variables are used to collect data across all pegasus projects (sub-projects) and then print information
  // to the user at the end after build is finished.
  private static StringBuffer _restModelCompatMessage = new StringBuffer();
  private static final Collection<String> _needCheckinFiles = new ArrayList<>();
  private static final Collection<String> _needBuildFolders = new ArrayList<>();
  private static final Collection<String> _possibleMissingFilesInEarlierCommit = new ArrayList<>();

  private static final String RUN_ONCE = "runOnce";
  private static final Object STATIC_PROJECT_EVALUATED_LOCK = new Object();

  private static final List<String> UNUSED_CONFIGURATIONS = Arrays.asList(
      "dataTemplateGenerator", "restTools", "avroSchemaGenerator");
  // Directory in the dataTemplate jar that holds schemas translated from PDL to PDSC.
  private static final String TRANSLATED_SCHEMAS_DIR = "legacyPegasusSchemas";
  // Enable the use of argFiles for the tasks that support them
  private static final String ENABLE_ARG_FILE = "pegasusPlugin.enableArgFile";

  @SuppressWarnings("unchecked")
  private Class<? extends Plugin<Project>> _thisPluginType = (Class<? extends Plugin<Project>>)
      getClass().asSubclass(Plugin.class);

  private Task _generateSourcesJarTask;
  private Javadoc _generateJavadocTask;
  private Task _generateJavadocJarTask;

  public void setPluginType(Class<? extends Plugin<Project>> pluginType)
  {
    _thisPluginType = pluginType;
  }

  public void setSourcesJarTask(Task sourcesJarTask)
  {
    _generateSourcesJarTask = sourcesJarTask;
  }

  public void setJavadocJarTask(Task javadocJarTask)
  {
    _generateJavadocJarTask = javadocJarTask;
  }

  @Override
  public void apply(Project project)
  {
    project.getPlugins().apply(JavaPlugin.class);
    project.getPlugins().apply(IdeaPlugin.class);
    project.getPlugins().apply(EclipsePlugin.class);

    // this HashMap will have a PegasusOptions per sourceSet
    project.getExtensions().getExtraProperties().set("pegasus", new HashMap<String, PegasusOptions>());
    // this map will extract PegasusOptions.GenerationMode to project property
    project.getExtensions().getExtraProperties().set("PegasusGenerationMode",
        Arrays.stream(PegasusOptions.GenerationMode.values())
            .collect(Collectors.toMap(PegasusOptions.GenerationMode::name, Function.identity())));

    synchronized (STATIC_PROJECT_EVALUATED_LOCK)
    {
      // Check if this is the first time the block will run. Pegasus plugin can run multiple times in a build if
      // multiple sub-projects applied the plugin.
      if (!project.getRootProject().hasProperty(RUN_ONCE)
          || !Boolean.parseBoolean(String.valueOf(project.getRootProject().property(RUN_ONCE))))
      {
        project.getGradle().projectsEvaluated(gradle ->
          gradle.getRootProject().subprojects(subproject ->
            UNUSED_CONFIGURATIONS.forEach(configurationName -> {
              Configuration conf = subproject.getConfigurations().findByName(configurationName);
              if (conf != null && !conf.isEmpty()) {
                subproject.getLogger().warn("*** Project {} declares dependency to unused configuration \"{}\". "
                    + "This configuration is deprecated and you can safely remove the dependency. ***",
                    subproject.getPath(), configurationName);
              }
            })
          )
        );

        // Re-initialize the static variables as they might have stale values from previous run. With Gradle 3.0 and
        // gradle daemon enabled, the plugin class might not be loaded for every run.
        DATA_TEMPLATE_FILE_SUFFIXES.clear();
        DATA_TEMPLATE_FILE_SUFFIXES.add(DATA_TEMPLATE_FILE_SUFFIX);
        DATA_TEMPLATE_FILE_SUFFIXES.add(PDL_FILE_SUFFIX);

        _restModelCompatMessage = new StringBuffer();
        _needCheckinFiles.clear();
        _needBuildFolders.clear();
        _possibleMissingFilesInEarlierCommit.clear();

        project.getGradle().buildFinished(result ->
        {
          StringBuilder endOfBuildMessage = new StringBuilder();
          if (_restModelCompatMessage.length() > 0)
          {
            endOfBuildMessage.append(_restModelCompatMessage);
          }

          if (!_needCheckinFiles.isEmpty())
          {
            endOfBuildMessage.append(createModifiedFilesMessage(_needCheckinFiles, _needBuildFolders));
          }

          if (!_possibleMissingFilesInEarlierCommit.isEmpty())
          {
            endOfBuildMessage.append(createPossibleMissingFilesMessage(_possibleMissingFilesInEarlierCommit));
          }

          if (endOfBuildMessage.length() > 0)
          {
            result.getGradle().getRootProject().getLogger().quiet(endOfBuildMessage.toString());
          }
        });

        // Set an extra property on the root project to indicate the initialization is complete for the current build.
        project.getRootProject().getExtensions().getExtraProperties().set(RUN_ONCE, true);
      }
    }

    ConfigurationContainer configurations = project.getConfigurations();

    // configuration for getting the required classes to make pegasus call main methods
    configurations.maybeCreate("pegasusPlugin");

    // configuration for compiling generated data templates
    Configuration dataTemplateCompile = configurations.maybeCreate("dataTemplateCompile");
    dataTemplateCompile.setVisible(false);

    // configuration for running rest client generator
    Configuration restClientCompile = configurations.maybeCreate("restClientCompile");
    restClientCompile.setVisible(false);

    // configuration for running data template generator
    // DEPRECATED! This configuration is no longer used. Please stop using it.
    Configuration dataTemplateGenerator = configurations.maybeCreate("dataTemplateGenerator");
    dataTemplateGenerator.setVisible(false);

    // configuration for running rest client generator
    // DEPRECATED! This configuration is no longer used. Please stop using it.
    Configuration restTools = configurations.maybeCreate("restTools");
    restTools.setVisible(false);

    // configuration for running Avro schema generator
    // DEPRECATED! To skip avro schema generation, use PegasusOptions.generationModes
    Configuration avroSchemaGenerator = configurations.maybeCreate("avroSchemaGenerator");
    avroSchemaGenerator.setVisible(false);

    // configuration for depending on data schemas and potentially generated data templates
    // and for publishing jars containing data schemas to the project artifacts for including in the ivy.xml
    Configuration dataModel = configurations.maybeCreate("dataModel");
    Configuration testDataModel = configurations.maybeCreate("testDataModel");
    testDataModel.extendsFrom(dataModel);

    // configuration for depending on data schemas and potentially generated data templates
    // and for publishing jars containing data schemas to the project artifacts for including in the ivy.xml
    Configuration avroSchema = configurations.maybeCreate("avroSchema");
    Configuration testAvroSchema = configurations.maybeCreate("testAvroSchema");
    testAvroSchema.extendsFrom(avroSchema);

    // configuration for depending on rest idl and potentially generated client builders
    // and for publishing jars containing rest idl to the project artifacts for including in the ivy.xml
    Configuration restModel = configurations.maybeCreate("restModel");
    Configuration testRestModel = configurations.maybeCreate("testRestModel");
    testRestModel.extendsFrom(restModel);

    // configuration for publishing jars containing data schemas and generated data templates
    // to the project artifacts for including in the ivy.xml
    //
    // published data template jars depends on the configurations used to compile the classes
    // in the jar, this includes the data models/templates used by the data template generator
    // and the classes used to compile the generated classes.
    Configuration dataTemplate = configurations.maybeCreate("dataTemplate");
    dataTemplate.extendsFrom(dataTemplateCompile, dataModel);
    Configuration testDataTemplate = configurations.maybeCreate("testDataTemplate");
    testDataTemplate.extendsFrom(dataTemplate, testDataModel);

    // configuration for processing and validating schema annotation during build time.
    //
    // The configuration contains dependencies to schema annotation handlers which would process schema annotations
    // and validate.
    Configuration schemaAnnotationHandler = configurations.maybeCreate("schemaAnnotationHandler");

    // configuration for parsing and validating extension schemas during build time.
    //
    // publish extension schemas into extension schema Jar once the validation passes.
    Configuration extensionSchema = configurations.maybeCreate("extensionSchema");

    // configuration for publishing jars containing rest idl and generated client builders
    // to the project artifacts for including in the ivy.xml
    //
    // published client builder jars depends on the configurations used to compile the classes
    // in the jar, this includes the data models/templates (potentially generated by this
    // project and) used by the data template generator and the classes used to compile
    // the generated classes.
    Configuration restClient = configurations.maybeCreate("restClient");
    restClient.extendsFrom(restClientCompile, dataTemplate);
    Configuration testRestClient = configurations.maybeCreate("testRestClient");
    testRestClient.extendsFrom(restClient, testDataTemplate);

    Properties properties = new Properties();
    InputStream inputStream = getClass().getResourceAsStream("/pegasus-version.properties");
    if (inputStream != null)
    {
      try
      {
        properties.load(inputStream);
      }
      catch (IOException e)
      {
        throw new GradleException("Unable to read pegasus-version.properties file.", e);
      }

      String version = properties.getProperty("pegasus.version");

      project.getDependencies().add("pegasusPlugin", "com.linkedin.pegasus:data:" + version);
      project.getDependencies().add("pegasusPlugin", "com.linkedin.pegasus:data-avro-generator:" + version);
      project.getDependencies().add("pegasusPlugin", "com.linkedin.pegasus:generator:" + version);
      project.getDependencies().add("pegasusPlugin", "com.linkedin.pegasus:restli-tools:" + version);
    }
    else
    {
      project.getLogger().lifecycle("Unable to add pegasus dependencies to {}. Please be sure that "
          + "'com.linkedin.pegasus:data', 'com.linkedin.pegasus:data-avro-generator', 'com.linkedin.pegasus:generator', 'com.linkedin.pegasus:restli-tools'"
          + " are available on the configuration pegasusPlugin",
          project.getPath());
    }
    project.getDependencies().add("pegasusPlugin", "org.slf4j:slf4j-simple:1.7.2");
    project.getDependencies().add("pegasusPlugin", project.files(System.getProperty("java.home") + "/../lib/tools.jar"));

    // this call has to be here because:
    // 1) artifact cannot be published once projects has been evaluated, so we need to first
    // create the tasks and artifact handler, then progressively append sources
    // 2) in order to append sources progressively, the source and documentation tasks and artifacts must be
    // configured/created before configuring and creating the code generation tasks.

    configureGeneratedSourcesAndJavadoc(project);

    ChangedFileReportTask changedFileReportTask = project.getTasks()
        .create("changedFilesReport", ChangedFileReportTask.class);

    project.getTasks().getByName("check").dependsOn(changedFileReportTask);

    SourceSetContainer sourceSets = project.getConvention()
        .getPlugin(JavaPluginConvention.class).getSourceSets();

    sourceSets.all(sourceSet ->
    {
      if (sourceSet.getName().toLowerCase(Locale.US).contains("generated"))
      {
        return;
      }

      checkAvroSchemaExist(project, sourceSet);

      // the idl Generator input options will be inside the PegasusOptions class. Users of the
      // plugin can set the inputOptions in their build.gradle
      @SuppressWarnings("unchecked")
      Map<String, PegasusOptions> pegasusOptions = (Map<String, PegasusOptions>) project
          .getExtensions().getExtraProperties().get("pegasus");

      pegasusOptions.put(sourceSet.getName(), new PegasusOptions());

      // rest model generation could fail on incompatibility
      // if it can fail, fail it early
      configureRestModelGeneration(project, sourceSet);

      configureConversionUtilities(project, sourceSet);

      GenerateDataTemplateTask generateDataTemplateTask = configureDataTemplateGeneration(project, sourceSet);

      configureAvroSchemaGeneration(project, sourceSet);

      configureRestClientGeneration(project, sourceSet);

      if (!isPropertyTrue(project, DISABLE_SCHEMA_ANNOTATION_VALIDATION))
      {
        configureSchemaAnnotationValidation(project, sourceSet, generateDataTemplateTask);
      }

      configureExtensionSchemaValidationAndPublishTasks(project, sourceSet);

      Task cleanGeneratedDirTask = project.task(sourceSet.getTaskName("clean", "GeneratedDir"));
      cleanGeneratedDirTask.doLast(new CacheableAction<>(task ->
      {
        deleteGeneratedDir(project, sourceSet, REST_GEN_TYPE);
        deleteGeneratedDir(project, sourceSet, AVRO_SCHEMA_GEN_TYPE);
        deleteGeneratedDir(project, sourceSet, DATA_TEMPLATE_GEN_TYPE);
      }));

      // make clean depends on deleting the generated directories
      project.getTasks().getByName("clean").dependsOn(cleanGeneratedDirTask);

      // Set data schema directories as resource roots
      configureDataSchemaResourcesRoot(project, sourceSet);
    });

    project.getExtensions().getExtraProperties().set(GENERATOR_CLASSLOADER_NAME, getClass().getClassLoader());
  }

  protected void configureSchemaAnnotationValidation(Project project,
                                                     SourceSet sourceSet,
                                                     GenerateDataTemplateTask generateDataTemplatesTask)
  {
    // Task would execute based on the following order.
    // generateDataTemplatesTask -> validateSchemaAnnotationTask

    // Create ValidateSchemaAnnotation task
    ValidateSchemaAnnotationTask validateSchemaAnnotationTask = project.getTasks()
        .create(sourceSet.getTaskName("validate", "schemaAnnotation"), ValidateSchemaAnnotationTask.class, task ->
                {
                  task.setInputDir(generateDataTemplatesTask.getInputDir());
                  task.setResolverPath(getDataModelConfig(project, sourceSet)); // same resolver path as generateDataTemplatesTask
                  task.setClassPath(project.getConfigurations() .getByName("schemaAnnotationHandler")
                                           .plus(project.getConfigurations().getByName("pegasusPlugin"))
                                           .plus(project.getConfigurations().getByName("runtime")));
                  task.setHandlerJarPath(project.getConfigurations() .getByName("schemaAnnotationHandler"));
                  if (isPropertyTrue(project, ENABLE_ARG_FILE))
                  {
                    task.setEnableArgFile(true);
                  }
                }
            );

    // validateSchemaAnnotationTask depend on generateDataTemplatesTask
    validateSchemaAnnotationTask.dependsOn(generateDataTemplatesTask);

    // Check depends on validateSchemaAnnotationTask.
    project.getTasks().getByName("check").dependsOn(validateSchemaAnnotationTask);
  }



  protected void configureGeneratedSourcesAndJavadoc(Project project)
  {
    _generateJavadocTask = project.getTasks().create("generateJavadoc", Javadoc.class);

    if (_generateSourcesJarTask == null)
    {
      //
      // configuration for publishing jars containing sources for generated classes
      // to the project artifacts for including in the ivy.xml
      //
      ConfigurationContainer configurations = project.getConfigurations();
      Configuration generatedSources = configurations.maybeCreate("generatedSources");
      Configuration testGeneratedSources = configurations.maybeCreate("testGeneratedSources");
      testGeneratedSources.extendsFrom(generatedSources);

      _generateSourcesJarTask = project.getTasks().create("generateSourcesJar", Jar.class, jarTask -> {
        jarTask.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP);
        jarTask.setDescription("Generates a jar file containing the sources for the generated Java classes.");
        jarTask.setClassifier("sources");
      });

      project.getArtifacts().add("generatedSources", _generateSourcesJarTask);
    }

    if (_generateJavadocJarTask == null)
    {
      //
      // configuration for publishing jars containing Javadoc for generated classes
      // to the project artifacts for including in the ivy.xml
      //
      ConfigurationContainer configurations = project.getConfigurations();
      Configuration generatedJavadoc = configurations.maybeCreate("generatedJavadoc");
      Configuration testGeneratedJavadoc = configurations.maybeCreate("testGeneratedJavadoc");
      testGeneratedJavadoc.extendsFrom(generatedJavadoc);

      _generateJavadocJarTask = project.getTasks().create("generateJavadocJar", Jar.class, jarTask -> {
        jarTask.dependsOn(_generateJavadocTask);
        jarTask.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP);
        jarTask.setDescription("Generates a jar file containing the Javadoc for the generated Java classes.");
        jarTask.setClassifier("javadoc");
        jarTask.from(_generateJavadocTask.getDestinationDir());
      });

      project.getArtifacts().add("generatedJavadoc", _generateJavadocJarTask);
    }
    else
    {
      // TODO: Tighten the types so that _generateJavadocJarTask must be of type Jar.
      ((Jar) _generateJavadocJarTask).from(_generateJavadocTask.getDestinationDir());
      _generateJavadocJarTask.dependsOn(_generateJavadocTask);
    }
  }

  protected void configureExtensionSchemaValidationAndPublishTasks(Project project, SourceSet sourceSet)
  {
    // extension schema  directory
    File extensionSchemaDir = project.file(getExtensionSchemaPath(project, sourceSet));

    if (SharedFileUtils.getSuffixedFiles(project, extensionSchemaDir, PDL_FILE_SUFFIX).isEmpty())
    {
      return;
    }

    ValidateExtensionSchemaTask validateExtensionSchemaTask =  project.getTasks()
        .create(sourceSet.getTaskName("validate", "ExtensionSchema"), ValidateExtensionSchemaTask.class, task -> {
          task.setInputDir(extensionSchemaDir);
          task.setResolverPath(getDataModelConfig(project, sourceSet).plus(project.files(getDataSchemaPath(project, sourceSet))));
          task.setClassPath(project.getConfigurations().getByName("pegasusPlugin"));
          if (isPropertyTrue(project, ENABLE_ARG_FILE))
          {
            task.setEnableArgFile(true);
          }
        });

    project.getTasks().getByName("check").dependsOn(validateExtensionSchemaTask);

    // Publish Extension Schemas into extensionSchema Jar
    Task extensionSchemaJarTask = project.getTasks().create(sourceSet.getName() + "ExtensionSchemaJar", Jar.class, task ->
    {
      task.from(extensionSchemaDir, copySpec ->
      {
        copySpec.eachFile(fileCopyDetails -> project.getLogger()
            .info("Add extensionSchema file: {}", fileCopyDetails));
        copySpec.setIncludes(Collections.singletonList('*' + PDL_FILE_SUFFIX));
      });
      task.setDescription("Generate extensionSchema jar");
    });
    extensionSchemaJarTask.dependsOn(validateExtensionSchemaTask);

    project.getArtifacts().add("extensionSchema", extensionSchemaJarTask);
  }

  private static void deleteGeneratedDir(Project project, SourceSet sourceSet, String dirType)
  {
    String generatedDirPath = getGeneratedDirPath(project, sourceSet, dirType);
    project.getLogger().info("Delete generated directory {}", generatedDirPath);
    project.delete(generatedDirPath);
  }

  private static <E extends Enum<E>> Class<E> getCompatibilityLevelClass(Project project)
  {
    ClassLoader generatorClassLoader = (ClassLoader) project.property(GENERATOR_CLASSLOADER_NAME);

    String className = "com.linkedin.restli.tools.idlcheck.CompatibilityLevel";
    try
    {
      @SuppressWarnings("unchecked")
      Class<E> enumClass = (Class<E>) generatorClassLoader.loadClass(className).asSubclass(Enum.class);
      return enumClass;
    }
    catch (ClassNotFoundException e)
    {
      throw new RuntimeException("Could not load class " + className);
    }
  }

  private static void addGeneratedDir(Project project, SourceSet sourceSet, Collection<Configuration> configurations)
  {
    project.getPlugins().withType(IdeaPlugin.class, ideaPlugin -> {
      IdeaModule ideaModule = ideaPlugin.getModel().getModule();
      // stupid if block needed because of stupid assignment required to update source dirs
      if (isTestSourceSet(sourceSet))
      {
        Set<File> sourceDirs = ideaModule.getTestSourceDirs();
        sourceDirs.addAll(sourceSet.getJava().getSrcDirs());
        // this is stupid but assignment is required
        ideaModule.setTestSourceDirs(sourceDirs);
        if (debug)
        {
          System.out.println("Added " + sourceSet.getJava().getSrcDirs() + " to IdeaModule testSourceDirs "
              + ideaModule.getTestSourceDirs());
        }
      }
      else
      {
        Set<File> sourceDirs = ideaModule.getSourceDirs();
        sourceDirs.addAll(sourceSet.getJava().getSrcDirs());
        // this is stupid but assignment is required
        ideaModule.setSourceDirs(sourceDirs);
        if (debug)
        {
          System.out.println("Added " + sourceSet.getJava().getSrcDirs() + " to  IdeaModule sourceDirs "
              + ideaModule.getSourceDirs());
        }
      }
      Collection<Configuration> compilePlus = ideaModule.getScopes().get("COMPILE").get("plus");
      compilePlus.addAll(configurations);
      ideaModule.getScopes().get("COMPILE").put("plus", compilePlus);
    });
  }

  private static void checkAvroSchemaExist(Project project, SourceSet sourceSet)
  {
    String sourceDir = "src" + File.separatorChar + sourceSet.getName();
    File avroSourceDir = project.file(sourceDir + File.separatorChar + "avro");
    if (avroSourceDir.exists())
    {
      project.getLogger().lifecycle("{}'s {} has non-empty avro directory. pegasus plugin does not process avro directory",
          project.getName(), sourceDir);
    }
  }

  // Compute the name of the source set that will contain a type of an input generated code.
  // e.g. genType may be 'DataTemplate' or 'Rest'
  private static String getGeneratedSourceSetName(SourceSet sourceSet, String genType)
  {
    return sourceSet.getName() + "Generated" + genType;
  }

  // Compute the directory name that will contain a type generated code of an input source set.
  // e.g. genType may be 'DataTemplate' or 'Rest'
  public static String getGeneratedDirPath(Project project, SourceSet sourceSet, String genType)
  {
    String override = getOverridePath(project, sourceSet, "overrideGeneratedDir");
    String sourceSetName = getGeneratedSourceSetName(sourceSet, genType);
    String base = override == null ? "src" : override;

    return base + File.separatorChar + sourceSetName;
  }

  private static String getDataSchemaPath(Project project, SourceSet sourceSet)
  {
    String override = getOverridePath(project, sourceSet, "overridePegasusDir");
    if (override == null)
    {
      return "src" + File.separatorChar + sourceSet.getName() + File.separatorChar + "pegasus";
    }
    else
    {
      return override;
    }
  }

  private static String getExtensionSchemaPath(Project project, SourceSet sourceSet)
  {
    String override = getOverridePath(project, sourceSet, "overrideExtensionSchemaDir");
    if(override == null)
    {
      return "src" + File.separatorChar + sourceSet.getName() + File.separatorChar + "extensions";
    }
    else
    {
      return override;
    }
  }

  private static String getSnapshotPath(Project project, SourceSet sourceSet)
  {
    String override = getOverridePath(project, sourceSet, "overrideSnapshotDir");
    if (override == null)
    {
      return "src" + File.separatorChar + sourceSet.getName() + File.separatorChar + "snapshot";
    }
    else
    {
      return override;
    }
  }

  private static String getIdlPath(Project project, SourceSet sourceSet)
  {
    String override = getOverridePath(project, sourceSet, "overrideIdlDir");
    if (override == null)
    {
      return "src" + File.separatorChar + sourceSet.getName() + File.separatorChar + "idl";
    }
    else
    {
      return override;
    }
  }

  private static String getOverridePath(Project project, SourceSet sourceSet, String overridePropertyName)
  {
    String sourceSetPropertyName = sourceSet.getName() + '.' + overridePropertyName;
    String override = getNonEmptyProperty(project, sourceSetPropertyName);

    if (override == null && sourceSet.getName().equals("main"))
    {
      override = getNonEmptyProperty(project, overridePropertyName);
    }

    return override;
  }

  private static boolean isTestSourceSet(SourceSet sourceSet)
  {
    return TEST_DIR_REGEX.matcher(sourceSet.getName()).find();
  }

  private static Configuration getDataModelConfig(Project project, SourceSet sourceSet)
  {
    return isTestSourceSet(sourceSet)
        ? project.getConfigurations().getByName("testDataModel")
        : project.getConfigurations().getByName("dataModel");
  }

  private static boolean isTaskSuccessful(Task task)
  {
    return task.getState().getExecuted() && !task.getState().getSkipped() && task.getState().getFailure() == null;
  }

  protected void configureRestModelGeneration(Project project, SourceSet sourceSet)
  {
    if (sourceSet.getAllSource().isEmpty())
    {
      project.getLogger().info("No source files found for sourceSet {}.  Skipping idl generation.", sourceSet.getName());
      return;
    }

    // afterEvaluate needed so that api project can be overridden via ext.apiProject
    project.afterEvaluate(p ->
    {
      // find api project here instead of in each project's plugin configuration
      // this allows api project relation options (ext.api*) to be specified anywhere in the build.gradle file
      // alternatively, pass closures to task configuration, and evaluate the closures when task is executed
      Project apiProject = getCheckedApiProject(project);

      // make sure the api project is evaluated. Important for configure-on-demand mode.
      if (apiProject != null)
      {
        project.evaluationDependsOn(apiProject.getPath());

        if (!apiProject.getPlugins().hasPlugin(_thisPluginType))
        {
          apiProject = null;
        }
      }

      if (apiProject == null)
      {
        return;
      }

      Task untypedJarTask = project.getTasks().findByName(sourceSet.getJarTaskName());
      if (!(untypedJarTask instanceof Jar))
      {
        return;
      }
      Jar jarTask = (Jar) untypedJarTask;

      String snapshotCompatPropertyName = findProperty(FileCompatibilityType.SNAPSHOT);
      if (project.hasProperty(snapshotCompatPropertyName) && "off".equalsIgnoreCase((String) project.property(snapshotCompatPropertyName)))
      {
        project.getLogger().lifecycle("Project {} snapshot compatibility level \"OFF\" is deprecated. Default to \"IGNORE\".",
            project.getPath());
      }

      // generate the rest model
      FileCollection restModelCodegenClasspath = project.getConfigurations().getByName("pegasusPlugin")
          .plus(project.getConfigurations().getByName("runtime"))
          .plus(sourceSet.getRuntimeClasspath());
      String destinationDirPrefix = getGeneratedDirPath(project, sourceSet, REST_GEN_TYPE) + File.separatorChar;
      FileCollection restModelResolverPath = apiProject.files(getDataSchemaPath(project, sourceSet))
          .plus(getDataModelConfig(apiProject, sourceSet));
      Set<File> watchedRestModelInputDirs = buildWatchedRestModelInputDirs(project, sourceSet);
      Set<File> restModelInputDirs = difference(sourceSet.getAllSource().getSrcDirs(),
          sourceSet.getResources().getSrcDirs());

      Task generateRestModelTask = project.getTasks()
          .create(sourceSet.getTaskName("generate", "restModel"), GenerateRestModelTask.class, task ->
          {
            task.dependsOn(project.getTasks().getByName(sourceSet.getClassesTaskName()));
            task.setCodegenClasspath(restModelCodegenClasspath);
            task.setWatchedCodegenClasspath(restModelCodegenClasspath
                .filter(file -> !"main".equals(file.getName()) && !"classes".equals(file.getName())));
            task.setInputDirs(restModelInputDirs);
            task.setWatchedInputDirs(watchedRestModelInputDirs.isEmpty()
                ? restModelInputDirs : watchedRestModelInputDirs);
            // we need all the artifacts from runtime for any private implementation classes the server code might need.
            task.setSnapshotDestinationDir(project.file(destinationDirPrefix + "snapshot"));
            task.setIdlDestinationDir(project.file(destinationDirPrefix + "idl"));

            @SuppressWarnings("unchecked")
            Map<String, PegasusOptions> pegasusOptions = (Map<String, PegasusOptions>) project
                .getExtensions().getExtraProperties().get("pegasus");
            task.setIdlOptions(pegasusOptions.get(sourceSet.getName()).idlOptions);

            task.setResolverPath(restModelResolverPath);
            if (isPropertyTrue(project, ENABLE_ARG_FILE))
            {
              task.setEnableArgFile(true);
            }

            task.doFirst(new CacheableAction<>(t -> deleteGeneratedDir(project, sourceSet, REST_GEN_TYPE)));
          });

      File apiSnapshotDir = apiProject.file(getSnapshotPath(apiProject, sourceSet));
      File apiIdlDir = apiProject.file(getIdlPath(apiProject, sourceSet));
      apiSnapshotDir.mkdirs();

      if (!isPropertyTrue(project, SKIP_IDL_CHECK))
      {
        apiIdlDir.mkdirs();
      }

      CheckRestModelTask checkRestModelTask = project.getTasks()
          .create(sourceSet.getTaskName("check", "RestModel"), CheckRestModelTask.class, task ->
          {
            task.dependsOn(generateRestModelTask);
            task.setCurrentSnapshotFiles(SharedFileUtils.getSnapshotFiles(project, destinationDirPrefix));
            task.setPreviousSnapshotDirectory(apiSnapshotDir);
            task.setCurrentIdlFiles(SharedFileUtils.getIdlFiles(project, destinationDirPrefix));
            task.setPreviousIdlDirectory(apiIdlDir);
            task.setCodegenClasspath(project.getConfigurations().getByName("pegasusPlugin"));
            task.setModelCompatLevel(PropertyUtil.findCompatLevel(project, FileCompatibilityType.SNAPSHOT));
            task.onlyIf(t -> !isPropertyTrue(project, SKIP_IDL_CHECK));

            task.doLast(new CacheableAction<>(t ->
            {
              if (!task.isEquivalent())
              {
                _restModelCompatMessage.append(task.getWholeMessage());
              }
            }));
          });

      CheckSnapshotTask checkSnapshotTask = project.getTasks()
          .create(sourceSet.getTaskName("check", "Snapshot"), CheckSnapshotTask.class, task -> {
            task.dependsOn(generateRestModelTask);
            task.setCurrentSnapshotFiles(SharedFileUtils.getSnapshotFiles(project, destinationDirPrefix));
            task.setPreviousSnapshotDirectory(apiSnapshotDir);
            task.setCodegenClasspath(project.getConfigurations().getByName("pegasusPlugin"));
            task.setSnapshotCompatLevel(PropertyUtil.findCompatLevel(project, FileCompatibilityType.SNAPSHOT));

            task.onlyIf(t -> isPropertyTrue(project, SKIP_IDL_CHECK));
          });

      CheckIdlTask checkIdlTask = project.getTasks()
          .create(sourceSet.getTaskName("check", "Idl"), CheckIdlTask.class, task ->
          {
            task.dependsOn(generateRestModelTask);
            task.setCurrentIdlFiles(SharedFileUtils.getIdlFiles(project, destinationDirPrefix));
            task.setPreviousIdlDirectory(apiIdlDir);
            task.setResolverPath(restModelResolverPath);
            task.setCodegenClasspath(project.getConfigurations().getByName("pegasusPlugin"));
            task.setIdlCompatLevel(PropertyUtil.findCompatLevel(project, FileCompatibilityType.IDL));
            if (isPropertyTrue(project, ENABLE_ARG_FILE))
            {
              task.setEnableArgFile(true);
            }


            task.onlyIf(t -> !isPropertyTrue(project, SKIP_IDL_CHECK)
                && !"OFF".equals(PropertyUtil.findCompatLevel(project, FileCompatibilityType.IDL)));
          });

      // rest model publishing involves cross-project reference
      // configure after all projects have been evaluated
      // the file copy can be turned off by "rest.model.noPublish" flag
      Task publishRestliSnapshotTask = project.getTasks()
          .create(sourceSet.getTaskName("publish", "RestliSnapshot"), PublishRestModelTask.class, task ->
          {
            task.dependsOn(checkRestModelTask, checkSnapshotTask, checkIdlTask);
            task.from(SharedFileUtils.getSnapshotFiles(project, destinationDirPrefix));
            task.into(apiSnapshotDir);
            task.setSuffix(SNAPSHOT_FILE_SUFFIX);

            task.onlyIf(t ->
            {
              project.getLogger().info(
                  "IDL_NO_PUBLISH: {}\n" +
                  "SNAPSHOT_NO_PUBLISH: {}\n" +
                  "checkRestModelTask:" +
                  " Executed: {}" +
                  ", Not Skipped: {}" +
                  ", No Failure: {}" +
                  ", Is Not Equivalent: {}\n" +
                  "checkSnapshotTask:" +
                  " Executed: {}" +
                  ", Not Skipped: {}" +
                  ", No Failure: {}" +
                  ", Is Not Equivalent: {}\n",
                  isPropertyTrue(project, IDL_NO_PUBLISH),
                  isPropertyTrue(project, SNAPSHOT_NO_PUBLISH),
                  checkRestModelTask.getState().getExecuted(),
                  !checkRestModelTask.getState().getSkipped(),
                  checkRestModelTask.getState().getFailure() == null,
                  !checkRestModelTask.isEquivalent(),
                  checkSnapshotTask.getState().getExecuted(),
                  !checkSnapshotTask.getState().getSkipped(),
                  checkSnapshotTask.getState().getFailure() == null,
                  !checkSnapshotTask.isEquivalent());

              return !isPropertyTrue(project, SNAPSHOT_NO_PUBLISH) &&
              (
                (
                   isPropertyTrue(project, SKIP_IDL_CHECK) &&
                   isTaskSuccessful(checkSnapshotTask) &&
                   !(checkSnapshotTask.isEquivalent())
                ) ||
                (
                  !isPropertyTrue(project, SKIP_IDL_CHECK) &&
                  isTaskSuccessful(checkRestModelTask) &&
                  !(checkRestModelTask.isEquivalent())
                )
              );
            });
          });

      Task publishRestliIdlTask = project.getTasks()
          .create(sourceSet.getTaskName("publish", "RestliIdl"), PublishRestModelTask.class, task -> {
            task.dependsOn(checkRestModelTask, checkIdlTask, checkSnapshotTask);
            task.from(SharedFileUtils.getIdlFiles(project, destinationDirPrefix));
            task.into(apiIdlDir);
            task.setSuffix(IDL_FILE_SUFFIX);

            task.onlyIf(t ->
            {
              project.getLogger().info(
                  "SKIP_IDL: {}\n" +
                  "IDL_NO_PUBLISH: {}\n" +
                  "SNAPSHOT_NO_PUBLISH: {}\n" +
                  "checkRestModelTask:" +
                  " Executed: {}" +
                  ", Not Skipped: {}" +
                  ", No Failure: {}" +
                  ", Is Not Equivalent: {}\n" +
                  "checkIdlTask:" +
                  " Executed: {}" +
                  ", Not Skipped: {}" +
                  ", No Failure: {}" +
                  ", Is Not Equivalent: {}\n" +
                  "checkSnapshotTask:" +
                  " Executed: {}" +
                  ", Not Skipped: {}" +
                  ", No Failure: {}" +
                  ", Is RestSpec Not Equivalent: {}\n",
                  isPropertyTrue(project, SKIP_IDL_CHECK),
                  isPropertyTrue(project, IDL_NO_PUBLISH),
                  isPropertyTrue(project, SNAPSHOT_NO_PUBLISH),
                  checkRestModelTask.getState().getExecuted(),
                  !checkRestModelTask.getState().getSkipped(),
                  checkRestModelTask.getState().getFailure() == null,
                  !checkRestModelTask.isEquivalent(),
                  checkIdlTask.getState().getExecuted(),
                  !checkIdlTask.getState().getSkipped(),
                  checkIdlTask.getState().getFailure() == null,
                  !checkIdlTask.isEquivalent(),
                  checkSnapshotTask.getState().getExecuted(),
                  !checkSnapshotTask.getState().getSkipped(),
                  checkSnapshotTask.getState().getFailure() == null,
                  !checkSnapshotTask.isEquivalent());

              return !isPropertyTrue(project, IDL_NO_PUBLISH) &&
              (
                (
                   isPropertyTrue(project, SKIP_IDL_CHECK) &&
                   isTaskSuccessful(checkSnapshotTask) &&
                   !(checkSnapshotTask.isRestSpecEquivalent())
                ) ||
                (
                   !isPropertyTrue(project, SKIP_IDL_CHECK) &&
                   (
                      (isTaskSuccessful(checkRestModelTask) && !(checkRestModelTask.isRestSpecEquivalent())) ||
                      (isTaskSuccessful(checkIdlTask) && !(checkIdlTask.isEquivalent()))
                   )
                )
              );
            });
          });

      project.getLogger().info("API project selected for {} is {}",
          publishRestliIdlTask.getPath(), apiProject.getPath());

      jarTask.from(SharedFileUtils.getIdlFiles(project, destinationDirPrefix));
      // add generated .restspec.json files as resources to the jar
      jarTask.dependsOn(publishRestliSnapshotTask, publishRestliIdlTask);

      ChangedFileReportTask changedFileReportTask = (ChangedFileReportTask) project.getTasks()
          .getByName("changedFilesReport");

      // Use the files from apiDir for generating the changed files report as we need to notify user only when
      // source system files are modified.
      changedFileReportTask.setIdlFiles(SharedFileUtils.getSuffixedFiles(project, apiIdlDir, IDL_FILE_SUFFIX));
      changedFileReportTask.setSnapshotFiles(SharedFileUtils.getSuffixedFiles(project, apiSnapshotDir,
          SNAPSHOT_FILE_SUFFIX));
      changedFileReportTask.mustRunAfter(publishRestliSnapshotTask, publishRestliIdlTask);
      changedFileReportTask.doLast(new CacheableAction<>(t ->
      {
        if (!changedFileReportTask.getNeedCheckinFiles().isEmpty())
        {
          project.getLogger().info("Adding modified files to need checkin list...");
          _needCheckinFiles.addAll(changedFileReportTask.getNeedCheckinFiles());
          _needBuildFolders.add(getCheckedApiProject(project).getPath());
        }
      }));
    });
  }

  protected void configureAvroSchemaGeneration(Project project, SourceSet sourceSet)
  {
    File dataSchemaDir = project.file(getDataSchemaPath(project, sourceSet));
    File avroDir = project.file(getGeneratedDirPath(project, sourceSet, AVRO_SCHEMA_GEN_TYPE)
        + File.separatorChar + "avro");

    // generate avro schema files from data schema
    Task generateAvroSchemaTask = project.getTasks()
        .create(sourceSet.getTaskName("generate", "avroSchema"), GenerateAvroSchemaTask.class, task -> {
          task.setInputDir(dataSchemaDir);
          task.setDestinationDir(avroDir);
          task.setResolverPath(getDataModelConfig(project, sourceSet));
          task.setCodegenClasspath(project.getConfigurations().getByName("pegasusPlugin"));
          if (isPropertyTrue(project, ENABLE_ARG_FILE))
          {
            task.setEnableArgFile(true);
          }

          task.onlyIf(t ->
          {
            if (task.getInputDir().exists())
            {
              @SuppressWarnings("unchecked")
              Map<String, PegasusOptions> pegasusOptions = (Map<String, PegasusOptions>) project
                  .getExtensions().getExtraProperties().get("pegasus");

              if (pegasusOptions.get(sourceSet.getName()).hasGenerationMode(PegasusOptions.GenerationMode.AVRO))
              {
                return true;
              }
            }

            return !project.getConfigurations().getByName("avroSchemaGenerator").isEmpty();
          });

          task.doFirst(new CacheableAction<>(t -> deleteGeneratedDir(project, sourceSet, AVRO_SCHEMA_GEN_TYPE)));
        });

    project.getTasks().getByName(sourceSet.getCompileJavaTaskName()).dependsOn(generateAvroSchemaTask);

    // create avro schema jar file

    Task avroSchemaJarTask = project.getTasks().create(sourceSet.getName() + "AvroSchemaJar", Jar.class, task ->
    {
      // add path prefix to each file in the data schema directory
      task.from(avroDir, copySpec ->
        copySpec.eachFile(fileCopyDetails ->
            fileCopyDetails.setPath("avro" + File.separatorChar + fileCopyDetails.getPath())));

      task.setAppendix(getAppendix(sourceSet, "avro-schema"));
      task.setDescription("Generate an avro schema jar");
    });

    if (!isTestSourceSet(sourceSet))
    {
      project.getArtifacts().add("avroSchema", avroSchemaJarTask);
    }
    else
    {
      project.getArtifacts().add("testAvroSchema", avroSchemaJarTask);
    }
  }

  protected void configureConversionUtilities(Project project, SourceSet sourceSet)
  {
    File dataSchemaDir = project.file(getDataSchemaPath(project, sourceSet));
    boolean reverse = isPropertyTrue(project, CONVERT_TO_PDL_REVERSE);
    boolean keepOriginal = isPropertyTrue(project, CONVERT_TO_PDL_KEEP_ORIGINAL);
    boolean skipVerification = isPropertyTrue(project, CONVERT_TO_PDL_SKIP_VERIFICATION);
    String preserveSourceCmd = getNonEmptyProperty(project, CONVERT_TO_PDL_PRESERVE_SOURCE_CMD);

    // Utility task for migrating between PDSC and PDL.
    project.getTasks().create(sourceSet.getTaskName("convert", "ToPdl"), TranslateSchemasTask.class, task ->
    {
      task.setInputDir(dataSchemaDir);
      task.setDestinationDir(dataSchemaDir);
      task.setResolverPath(getDataModelConfig(project, sourceSet));
      task.setCodegenClasspath(project.getConfigurations().getByName("pegasusPlugin"));
      task.setPreserveSourceCmd(preserveSourceCmd);
      if (reverse)
      {
        task.setSourceFormat(SchemaFileType.PDL);
        task.setDestinationFormat(SchemaFileType.PDSC);
      }
      else
      {
        task.setSourceFormat(SchemaFileType.PDSC);
        task.setDestinationFormat(SchemaFileType.PDL);
      }
      task.setKeepOriginal(keepOriginal);
      task.setSkipVerification(skipVerification);
      if (isPropertyTrue(project, ENABLE_ARG_FILE))
      {
        task.setEnableArgFile(true);
      }

      task.onlyIf(t -> task.getInputDir().exists());
      task.doLast(new CacheableAction<>(t ->
      {
        project.getLogger().lifecycle("Pegasus schema conversion complete.");
        project.getLogger().lifecycle("All pegasus schema files in " + dataSchemaDir + " have been converted");
        project.getLogger().lifecycle("You can use '-PconvertToPdl.reverse=true|false' to change the direction of conversion.");
      }));
    });

    // Helper task for reformatting existing PDL schemas by generating them again.
    project.getTasks().create(sourceSet.getTaskName("reformat", "Pdl"), TranslateSchemasTask.class, task ->
    {
      task.setInputDir(dataSchemaDir);
      task.setDestinationDir(dataSchemaDir);
      task.setResolverPath(getDataModelConfig(project, sourceSet));
      task.setCodegenClasspath(project.getConfigurations().getByName("pegasusPlugin"));
      task.setSourceFormat(SchemaFileType.PDL);
      task.setDestinationFormat(SchemaFileType.PDL);
      task.setKeepOriginal(true);
      task.setSkipVerification(true);
      if (isPropertyTrue(project, ENABLE_ARG_FILE))
      {
        task.setEnableArgFile(true);
      }

      task.onlyIf(t -> task.getInputDir().exists());
      task.doLast(new CacheableAction<>(t -> project.getLogger().lifecycle("PDL reformat complete.")));
    });
  }

  protected GenerateDataTemplateTask configureDataTemplateGeneration(Project project, SourceSet sourceSet)
  {
    File dataSchemaDir = project.file(getDataSchemaPath(project, sourceSet));
    File generatedDataTemplateDir = project.file(getGeneratedDirPath(project, sourceSet, DATA_TEMPLATE_GEN_TYPE)
        + File.separatorChar + "java");
    File publishableSchemasBuildDir = project.file(project.getBuildDir().getAbsolutePath()
        + File.separatorChar + sourceSet.getName() + "Schemas");
    File publishableLegacySchemasBuildDir = project.file(project.getBuildDir().getAbsolutePath()
        + File.separatorChar + sourceSet.getName() + "LegacySchemas");

    // generate data template source files from data schema
    GenerateDataTemplateTask generateDataTemplatesTask = project.getTasks()
        .create(sourceSet.getTaskName("generate", "dataTemplate"), GenerateDataTemplateTask.class, task ->
        {
          task.setInputDir(dataSchemaDir);
          task.setDestinationDir(generatedDataTemplateDir);
          task.setResolverPath(getDataModelConfig(project, sourceSet));
          task.setCodegenClasspath(project.getConfigurations().getByName("pegasusPlugin"));
          if (isPropertyTrue(project, ENABLE_ARG_FILE))
          {
            task.setEnableArgFile(true);
          }

          task.onlyIf(t ->
          {
            if (task.getInputDir().exists())
            {
              @SuppressWarnings("unchecked")
              Map<String, PegasusOptions> pegasusOptions = (Map<String, PegasusOptions>) project
                  .getExtensions().getExtraProperties().get("pegasus");

              return pegasusOptions.get(sourceSet.getName()).hasGenerationMode(PegasusOptions.GenerationMode.PEGASUS);
            }

            return false;
          });

          task.doFirst(new CacheableAction<>(t -> deleteGeneratedDir(project, sourceSet, DATA_TEMPLATE_GEN_TYPE)));
        });

    // TODO: Tighten the types so that _generateSourcesJarTask must be of type Jar.
    ((Jar) _generateSourcesJarTask).from(generateDataTemplatesTask.getDestinationDir());
    _generateSourcesJarTask.dependsOn(generateDataTemplatesTask);

    _generateJavadocTask.source(generateDataTemplatesTask.getDestinationDir());
    _generateJavadocTask.setClasspath(_generateJavadocTask.getClasspath()
        .plus(project.getConfigurations().getByName("dataTemplateCompile"))
        .plus(generateDataTemplatesTask.getResolverPath()));
    _generateJavadocTask.dependsOn(generateDataTemplatesTask);

    // Add extra dependencies for data model compilation
    project.getDependencies().add("dataTemplateCompile", "com.google.code.findbugs:jsr305:3.0.0");

    // create new source set for generated java source and class files
    String targetSourceSetName = getGeneratedSourceSetName(sourceSet, DATA_TEMPLATE_GEN_TYPE);

    SourceSetContainer sourceSets = project.getConvention()
        .getPlugin(JavaPluginConvention.class).getSourceSets();

    SourceSet targetSourceSet = sourceSets.create(targetSourceSetName, ss ->
    {
      ss.java(sourceDirectorySet -> sourceDirectorySet.srcDir(generatedDataTemplateDir));
      ss.setCompileClasspath(getDataModelConfig(project, sourceSet)
          .plus(project.getConfigurations().getByName("dataTemplateCompile")));
    });

    // idea plugin needs to know about new generated java source directory and its dependencies
    addGeneratedDir(project, targetSourceSet, Arrays.asList(
        getDataModelConfig(project, sourceSet),
        project.getConfigurations().getByName("dataTemplateCompile")));

    // make sure that java source files have been generated before compiling them
    Task compileTask = project.getTasks().getByName(targetSourceSet.getCompileJavaTaskName());
    compileTask.dependsOn(generateDataTemplatesTask);

    // we need to delete the build directory before staging files for translation/code generation, in case there were
    // left over files from a previous execution. This is not a problem if the input for translation/code generation
    // hasn't changed at all, because gradle will just realize the buildDir can be rebuilt from cache.
    Task destroyStaleFiles = project.getTasks().create(sourceSet.getName() + "DestroyStaleFiles");
    if (isPropertyTrue(project, DESTROY_STALE_FILES_ENABLE) && publishableSchemasBuildDir.exists())
    {
      destroyStaleFiles.getInputs().dir(publishableSchemasBuildDir);
      destroyStaleFiles.doLast(new CacheableAction<>(task -> project.delete(publishableSchemasBuildDir)));
    }

    // Dummy task to maintain backward compatibility, as this task was replaced by CopySchemas
    // TODO: Delete this task once use cases have had time to reference the new task
    Task copyPdscSchemasTask = project.getTasks().create(sourceSet.getName() + "CopyPdscSchemas");
    copyPdscSchemasTask.dependsOn(destroyStaleFiles);

    // Copy all schema files directly over for publication
    Task prepareSchemasForPublishTask = project.getTasks()
        .create(sourceSet.getName() + "CopySchemas", Copy.class, task ->
        {
          task.from(dataSchemaDir, copySpec -> DATA_TEMPLATE_FILE_SUFFIXES.forEach(suffix -> copySpec.include("**/*" + suffix)));
          task.into(publishableSchemasBuildDir);
        });
    prepareSchemasForPublishTask.dependsOn(copyPdscSchemasTask);

    Collection<Task> dataTemplateJarDepends = new ArrayList<>();
    dataTemplateJarDepends.add(compileTask);
    dataTemplateJarDepends.add(prepareSchemasForPublishTask);

    // Convert all PDL files back to PDSC for publication
    // TODO: Remove this conversion permanently once translated PDSCs are no longer needed.
    Task prepareLegacySchemasForPublishTask = project.getTasks()
        .create(sourceSet.getName() + "TranslateSchemas", TranslateSchemasTask.class, task ->
        {
          task.setInputDir(dataSchemaDir);
          task.setDestinationDir(publishableLegacySchemasBuildDir);
          task.setResolverPath(getDataModelConfig(project, sourceSet));
          task.setCodegenClasspath(project.getConfigurations().getByName("pegasusPlugin"));
          task.setSourceFormat(SchemaFileType.PDL);
          task.setDestinationFormat(SchemaFileType.PDSC);
          task.setKeepOriginal(true);
          task.setSkipVerification(true);
          if (isPropertyTrue(project, ENABLE_ARG_FILE))
          {
            task.setEnableArgFile(true);
          }
        });

    prepareLegacySchemasForPublishTask.dependsOn(destroyStaleFiles);
    dataTemplateJarDepends.add(prepareLegacySchemasForPublishTask);

    // create data template jar file
    Jar dataTemplateJarTask = project.getTasks()
        .create(sourceSet.getName() + "DataTemplateJar", Jar.class, task ->
        {
          task.dependsOn(dataTemplateJarDepends);

          // Copy all schemas as-is into the root schema directory in the JAR
          task.from(publishableSchemasBuildDir, copySpec ->
            copySpec.eachFile(fileCopyDetails ->
              fileCopyDetails.setPath("pegasus" + File.separatorChar + fileCopyDetails.getPath())));

          // Copy the translated PDSC schemas into a separate root directory in the JAR
          // TODO: Remove this permanently once translated PDSCs are no longer needed.
          task.from(publishableLegacySchemasBuildDir, copySpec ->
              copySpec.eachFile(fileCopyDetails ->
                  fileCopyDetails.setPath(TRANSLATED_SCHEMAS_DIR + File.separatorChar + fileCopyDetails.getPath())));

          task.from(targetSourceSet.getOutput());

          task.setAppendix(getAppendix(sourceSet, "data-template"));
          task.setDescription("Generate a data template jar");
        });

    // add the data model and date template jars to the list of project artifacts.
    if (!isTestSourceSet(sourceSet))
    {
      project.getArtifacts().add("dataTemplate", dataTemplateJarTask);
    }
    else
    {
      project.getArtifacts().add("testDataTemplate", dataTemplateJarTask);
    }

    // include additional dependencies into the appropriate configuration used to compile the input source set
    // must include the generated data template classes and their dependencies the configuration
    String compileConfigName = isTestSourceSet(sourceSet) ? "testCompile" : "compile";

    Configuration compileConfig = project.getConfigurations().maybeCreate(compileConfigName);
    compileConfig.extendsFrom(
        getDataModelConfig(project, sourceSet),
        project.getConfigurations().getByName("dataTemplateCompile"));

    project.getDependencies().add(compileConfigName, project.files(dataTemplateJarTask.getArchivePath()));

    if (debug)
    {
      System.out.println("configureDataTemplateGeneration sourceSet " + sourceSet.getName());
      System.out.println(compileConfigName + ".allDependencies : "
          + project.getConfigurations().getByName(compileConfigName).getAllDependencies());
      System.out.println(compileConfigName + ".extendsFrom: "
          + project.getConfigurations().getByName(compileConfigName).getExtendsFrom());
      System.out.println(compileConfigName + ".transitive: "
          + project.getConfigurations().getByName(compileConfigName).isTransitive());
    }

    project.getTasks().getByName(sourceSet.getCompileJavaTaskName()).dependsOn(dataTemplateJarTask);
    return generateDataTemplatesTask;
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
    File idlDir = project.file(getIdlPath(project, sourceSet));
    if (SharedFileUtils.getSuffixedFiles(project, idlDir, IDL_FILE_SUFFIX).isEmpty())
    {
      return;
    }

    File generatedRestClientDir = project.file(getGeneratedDirPath(project, sourceSet, REST_GEN_TYPE)
        + File.separatorChar + "java");

    // always include imported data template jars in compileClasspath of rest client
    FileCollection dataModelConfig = getDataModelConfig(project, sourceSet);

    // if data templates generated from this source set, add the generated data template jar to compileClasspath
    // of rest client.
    String dataTemplateSourceSetName = getGeneratedSourceSetName(sourceSet, DATA_TEMPLATE_GEN_TYPE);

    Jar dataTemplateJarTask = null;

    SourceSetContainer sourceSets = project.getConvention()
        .getPlugin(JavaPluginConvention.class).getSourceSets();

    FileCollection dataModels;
    if (sourceSets.findByName(dataTemplateSourceSetName) != null)
    {
      if (debug)
      {
        System.out.println("sourceSet " + sourceSet.getName() + " has generated sourceSet " + dataTemplateSourceSetName);
      }
      dataTemplateJarTask = (Jar) project.getTasks().getByName(sourceSet.getName() + "DataTemplateJar");
      dataModels = dataModelConfig.plus(project.files(dataTemplateJarTask.getArchivePath()));
    }
    else
    {
      dataModels = dataModelConfig;
    }

    // create source set for generated rest model, rest client source and class files.
    String targetSourceSetName = getGeneratedSourceSetName(sourceSet, REST_GEN_TYPE);
    SourceSet targetSourceSet = sourceSets.create(targetSourceSetName, ss ->
    {
      ss.java(sourceDirectorySet -> sourceDirectorySet.srcDir(generatedRestClientDir));
      ss.setCompileClasspath(dataModels.plus(project.getConfigurations().getByName("restClientCompile")));
    });

    project.getPlugins().withType(EclipsePlugin.class, eclipsePlugin -> {
      EclipseModel eclipseModel = (EclipseModel) project.getExtensions().findByName("eclipse");
      eclipseModel.getClasspath().getPlusConfigurations()
          .add(project.getConfigurations().getByName("restClientCompile"));
    });

    // idea plugin needs to know about new rest client source directory and its dependencies
    addGeneratedDir(project, targetSourceSet, Arrays.asList(
        getDataModelConfig(project, sourceSet),
        project.getConfigurations().getByName("restClientCompile")));

    // generate the rest client source files
    GenerateRestClientTask generateRestClientTask = project.getTasks()
        .create(targetSourceSet.getTaskName("generate", "restClient"), GenerateRestClientTask.class, task ->
        {
          task.dependsOn(project.getConfigurations().getByName("dataTemplate"));
          task.setInputDir(idlDir);
          task.setResolverPath(dataModels);
          task.setRuntimeClasspath(project.getConfigurations().getByName("dataModel")
              .plus(project.getConfigurations().getByName("dataTemplate").getArtifacts().getFiles()));
          task.setCodegenClasspath(project.getConfigurations().getByName("pegasusPlugin"));
          task.setDestinationDir(generatedRestClientDir);
          task.setRestli2FormatSuppressed(project.hasProperty(SUPPRESS_REST_CLIENT_RESTLI_2));
          task.setRestli1FormatSuppressed(project.hasProperty(SUPPRESS_REST_CLIENT_RESTLI_1));
          if (isPropertyTrue(project, ENABLE_ARG_FILE))
          {
            task.setEnableArgFile(true);
          }
        });

    if (dataTemplateJarTask != null)
    {
      generateRestClientTask.dependsOn(dataTemplateJarTask);
    }

    // TODO: Tighten the types so that _generateSourcesJarTask must be of type Jar.
    ((Jar) _generateSourcesJarTask).from(generateRestClientTask.getDestinationDir());
    _generateSourcesJarTask.dependsOn(generateRestClientTask);

    _generateJavadocTask.source(generateRestClientTask.getDestinationDir());
    _generateJavadocTask.setClasspath(_generateJavadocTask.getClasspath()
        .plus(project.getConfigurations().getByName("restClientCompile"))
        .plus(generateRestClientTask.getResolverPath()));
    _generateJavadocTask.dependsOn(generateRestClientTask);

    // make sure rest client source files have been generated before compiling them
    JavaCompile compileGeneratedRestClientTask = (JavaCompile) project.getTasks()
        .getByName(targetSourceSet.getCompileJavaTaskName());
    compileGeneratedRestClientTask.dependsOn(generateRestClientTask);
    compileGeneratedRestClientTask.getOptions().getCompilerArgs().add("-Xlint:-deprecation");

    // create the rest model jar file
    Task restModelJarTask = project.getTasks().create(sourceSet.getName() + "RestModelJar", Jar.class, task ->
    {
      task.from(idlDir, copySpec ->
      {
        copySpec.eachFile(fileCopyDetails -> project.getLogger()
            .info("Add idl file: {}", fileCopyDetails));
        copySpec.setIncludes(Collections.singletonList('*' + IDL_FILE_SUFFIX));
      });
      task.setAppendix(getAppendix(sourceSet, "rest-model"));
      task.setDescription("Generate rest model jar");
    });

    // create the rest client jar file
    Task restClientJarTask = project.getTasks()
        .create(sourceSet.getName() + "RestClientJar", Jar.class, task ->
        {
          task.dependsOn(compileGeneratedRestClientTask);
          task.from(idlDir, copySpec -> {
            copySpec.eachFile(fileCopyDetails -> {
              project.getLogger().info("Add interface file: {}", fileCopyDetails);
              fileCopyDetails.setPath("idl" + File.separatorChar + fileCopyDetails.getPath());
            });
            copySpec.setIncludes(Collections.singletonList('*' + IDL_FILE_SUFFIX));
          });
          task.from(targetSourceSet.getOutput());
          task.setAppendix(getAppendix(sourceSet, "rest-client"));
          task.setDescription("Generate rest client jar");
        });

    // add the rest model jar and the rest client jar to the list of project artifacts.
    if (!isTestSourceSet(sourceSet))
    {
      project.getArtifacts().add("restModel", restModelJarTask);
      project.getArtifacts().add("restClient", restClientJarTask);
    }
    else
    {
      project.getArtifacts().add("testRestModel", restModelJarTask);
      project.getArtifacts().add("testRestClient", restClientJarTask);
    }
  }

  // Return the appendix for generated jar files.
  // The source set name is not included for the main source set.
  private static String getAppendix(SourceSet sourceSet, String suffix)
  {
    return sourceSet.getName().equals("main") ? suffix : sourceSet.getName() + '-' + suffix;
  }

  private static Project getApiProject(Project project)
  {
    if (project.getExtensions().getExtraProperties().has("apiProject"))
    {
      return (Project) project.getExtensions().getExtraProperties().get("apiProject");
    }

    List<String> subsSuffixes;
    if (project.getExtensions().getExtraProperties().has("apiProjectSubstitutionSuffixes"))
    {
      @SuppressWarnings("unchecked")
      List<String> suffixValue = (List<String>) project.getExtensions()
          .getExtraProperties().get("apiProjectSubstitutionSuffixes");

      subsSuffixes = suffixValue;
    }
    else
    {
      subsSuffixes = Arrays.asList("-impl", "-service", "-server", "-server-impl");
    }

    for (String suffix : subsSuffixes)
    {
      if (project.getPath().endsWith(suffix))
      {
        String searchPath = project.getPath().substring(0, project.getPath().length() - suffix.length()) + "-api";
        Project apiProject = project.findProject(searchPath);
        if (apiProject != null)
        {
          return apiProject;
        }
      }
    }

    return project.findProject(project.getPath() + "-api");
  }

  private static Project getCheckedApiProject(Project project)
  {
    Project apiProject = getApiProject(project);

    if (apiProject == project)
    {
      throw new GradleException("The API project of ${project.path} must not be itself.");
    }

    return apiProject;
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
      return null;
    }

    String propertyValue = project.property(propertyName).toString();
    if (propertyValue.isEmpty())
    {
      return null;
    }

    return propertyValue;
  }

  /**
   * Return true if the given property exists and its value is true
   *
   * @param project the project where to look for the property
   * @param propertyName the name of the property
   */
  public static boolean isPropertyTrue(Project project, String propertyName)
  {
    return project.hasProperty(propertyName) && Boolean.valueOf(project.property(propertyName).toString());
  }

  private static String createModifiedFilesMessage(Collection<String> nonEquivExpectedFiles,
                                                   Collection<String> foldersToBeBuilt)
  {
    StringBuilder builder = new StringBuilder();
    builder.append("\nRemember to checkin the changes to the following new or modified files:\n");
    for (String file : nonEquivExpectedFiles)
    {
      builder.append("  ");
      builder.append(file);
      builder.append("\n");
    }

    if (!foldersToBeBuilt.isEmpty())
    {
      builder.append("\nThe file modifications include service interface changes, you can build the the following projects "
          + "to re-generate the client APIs accordingly:\n");
      for (String folder : foldersToBeBuilt)
      {
        builder.append("  ");
        builder.append(folder);
        builder.append("\n");
      }
    }

    return builder.toString();
  }

  private static String createPossibleMissingFilesMessage(Collection<String> missingFiles)
  {
    StringBuilder builder = new StringBuilder();
    builder.append("If this is the result of an automated build, then you may have forgotten to check in some snapshot or idl files:\n");
    for (String file : missingFiles)
    {
      builder.append("  ");
      builder.append(file);
      builder.append("\n");
    }

    return builder.toString();
  }

  private static String findProperty(FileCompatibilityType type)
  {
    String property;
    switch (type)
    {
      case SNAPSHOT:
        property = SNAPSHOT_COMPAT_REQUIREMENT;
        break;
      case IDL:
        property = IDL_COMPAT_REQUIREMENT;
        break;
      default:
        throw new GradleException("No property defined for compatibility type " + type);
    }
    return property;
  }

  private static Set<File> buildWatchedRestModelInputDirs(Project project, SourceSet sourceSet) {
    @SuppressWarnings("unchecked")
    Map<String, PegasusOptions> pegasusOptions = (Map<String, PegasusOptions>) project
        .getExtensions().getExtraProperties().get("pegasus");

    File rootPath = new File(project.getProjectDir(),
        pegasusOptions.get(sourceSet.getName()).restModelOptions.getRestResourcesRootPath());

    IdlOptions idlOptions = pegasusOptions.get(sourceSet.getName()).idlOptions;

    // if idlItems exist, only watch the smaller subset
    return idlOptions.getIdlItems().stream()
        .flatMap(idlItem -> Arrays.stream(idlItem.packageNames))
        .map(packageName -> new File(rootPath, packageName.replace('.', '/')))
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static <T> Set<T> difference(Set<T> left, Set<T> right)
  {
    Set<T> result = new HashSet<>(left);
    result.removeAll(right);
    return result;
  }

  /**
   * Configures the given source set so that its data schema directory (usually 'pegasus') is marked as a resource root.
   * The purpose of this is to improve the IDE experience. Makes sure to exclude this directory from being packaged in
   * with the default Jar task.
   */
  private static void configureDataSchemaResourcesRoot(Project project, SourceSet sourceSet)
  {
    sourceSet.resources(sourceDirectorySet -> {
      final String dataSchemaPath = getDataSchemaPath(project, sourceSet);
      final File dataSchemaRoot = project.file(dataSchemaPath);
      sourceDirectorySet.srcDir(dataSchemaPath);
      project.getLogger().info("Adding resource root '{}'", dataSchemaPath);

      // Exclude the data schema directory from being copied into the default Jar task
      sourceDirectorySet.getFilter().exclude(fileTreeElement -> {
        final File file = fileTreeElement.getFile();
        // Traversal starts with the children of a resource root, so checking the direct parent is sufficient
        final boolean exclude = dataSchemaRoot.equals(file.getParentFile());
        if (exclude)
        {
          project.getLogger().info("Excluding resource directory '{}'", file);
        }
        return exclude;
      });
    });
  }
}
