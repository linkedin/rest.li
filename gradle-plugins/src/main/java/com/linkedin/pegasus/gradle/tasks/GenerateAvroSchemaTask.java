package com.linkedin.pegasus.gradle.tasks;

import com.linkedin.pegasus.gradle.PegasusPlugin;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.StopExecutionException;
import org.gradle.api.tasks.TaskAction;

import static com.linkedin.pegasus.gradle.SharedFileUtils.getSuffixedFiles;


/**
 * Generate the Avro schema (.avsc) files from data schema files.
 *
 * To use this plugin, add these three lines to your build.gradle:
 * <pre>
 * apply plugin: 'li-pegasus2'
 * </pre>
 *
 * The plugin will scan the source set's pegasus directory, e.g. "src/main/pegasus"
 * for data schema (.pdsc) files.
 */
@CacheableTask
public class GenerateAvroSchemaTask extends DefaultTask
{
  private File _inputDir;
  private FileCollection _resolverPath;
  private FileCollection _codegenClasspath;
  private File _destinationDir;
  private static final String TYPERF_PROPERTIES_EXCLUDE = "generator.avro.typeref.properties.exclude";

  @TaskAction
  public void generate()
  {
    FileTree inputDataSchemaFiles = getSuffixedFiles(getProject(), _inputDir,
        PegasusPlugin.DATA_TEMPLATE_FILE_SUFFIXES);

    List<String> inputDataSchemaFilenames = StreamSupport.stream(inputDataSchemaFiles.spliterator(), false)
        .map(File::getPath)
        .collect(Collectors.toList());

    if (inputDataSchemaFilenames.isEmpty())
    {
      throw new StopExecutionException("There are no data schema input files. Skip generating avro schema.");
    }

    getProject().getLogger().info("Generating Avro schemas ...");
    getProject().getLogger().lifecycle("There are {} data schema input files. Using input root folder: {}",
        inputDataSchemaFilenames.size(), _inputDir);

    _destinationDir.mkdirs();

    String resolverPathStr = _resolverPath.plus(getProject().files(_inputDir)).getAsPath();

    getProject().javaexec(javaExecSpec ->
    {
      javaExecSpec.setMain("com.linkedin.data.avro.generator.AvroSchemaGenerator");
      javaExecSpec.setClasspath(_codegenClasspath);
      javaExecSpec.jvmArgs("-Dgenerator.resolver.path=" + resolverPathStr);

      String translateOptionalDefault = getTranslateOptionalDefault();
      if (translateOptionalDefault != null) {
        javaExecSpec.jvmArgs("-Dgenerator.avro.optional.default=" + translateOptionalDefault);
      }

      String overrideNamespace = getOverrideNamespace();
      if (overrideNamespace != null) {
        javaExecSpec.jvmArgs("-Dgenerator.avro.namespace.override=" + overrideNamespace);
      }

      String typeRefPropertiesExcludeList = getTypeRefPropertiesExcludeList();
      if (typeRefPropertiesExcludeList == null)
      {
        // unless overridden,
        // by default gradle plugin tasks will set this to have "validate" "java" so it is backward compatible
        typeRefPropertiesExcludeList = "validate,data";
      }
      javaExecSpec.jvmArgs(String.format("-D%s=%s", TYPERF_PROPERTIES_EXCLUDE, typeRefPropertiesExcludeList));

      javaExecSpec.args(_destinationDir.getPath());
      javaExecSpec.args(inputDataSchemaFilenames);
    });
  }

  /**
   * Directory containing the data schema files.
   */
  @InputDirectory
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  public File getInputDir()
  {
    return _inputDir;
  }

  public void setInputDir(File inputDir)
  {
    _inputDir = inputDir;
  }

  /**
   * The resolver path.
   */
  @Classpath
  public FileCollection getResolverPath()
  {
    return _resolverPath;
  }

  public void setResolverPath(FileCollection resolverPath)
  {
    _resolverPath = resolverPath;
  }

  @Classpath
  public FileCollection getCodegenClasspath()
  {
    return _codegenClasspath;
  }

  public void setCodegenClasspath(FileCollection codegenClasspath)
  {
    _codegenClasspath = codegenClasspath;
  }

  /**
   * Directory to write the generated Avro schema files.
   */
  @OutputDirectory
  public File getDestinationDir()
  {
    return _destinationDir;
  }

  public void setDestinationDir(File destinationDir)
  {
    _destinationDir = destinationDir;
  }

  /**
   * Value of project property "generator.avro.optional.default"
   */
  @Input
  @Optional
  public String getTranslateOptionalDefault()
  {
    return getProjectProperty("generator.avro.optional.default");
  }

  /**
   * Value of project property "generator.avro.namespace.override"
   */
  @Input
  @Optional
  public String getOverrideNamespace()
  {
    return getProjectProperty("generator.avro.namespace.override");
  }

  /**
   * Value of project property "generator.avro.typeref.properties.exclude"
   */
  @Input
  @Optional
  public String getTypeRefPropertiesExcludeList()
  {
    return getProjectProperty(TYPERF_PROPERTIES_EXCLUDE);
  }


  private String getProjectProperty(String name)
  {
    if (getProject().hasProperty(name))
    {
      return (String) getProject().property(name);
    }
    else
    {
      return null;
    }
  }
}
