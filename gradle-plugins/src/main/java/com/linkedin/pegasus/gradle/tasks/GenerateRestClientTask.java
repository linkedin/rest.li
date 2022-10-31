package com.linkedin.pegasus.gradle.tasks;

import com.linkedin.pegasus.gradle.PathingJarUtil;
import com.linkedin.pegasus.gradle.PegasusOptions;
import com.linkedin.pegasus.gradle.PegasusPlugin;
import com.linkedin.pegasus.gradle.SharedFileUtils;
import com.linkedin.pegasus.gradle.internal.ArgumentFileGenerator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import static com.linkedin.pegasus.gradle.internal.ArgumentFileGenerator.createArgFile;
import static com.linkedin.pegasus.gradle.internal.ArgumentFileGenerator.getArgFileSyntax;


/**
 * This task will generate the rest client source files.
 *
 * As pre-requisite of this task,, add these lines to your build.gradle:
 * <pre>
 * apply plugin: 'li-pegasus2'
 * </pre>
 *
 * Optionally, you can specify certain resource classes to be generated idl
 * <pre>
 * pegasus.<sourceSet>.clientOptions.addClientItem('<restModelFilePath>', '<defaultPackage>', <keepDataTemplates>)
 * </pre>
 * keepDataTemplates is a boolean that isn't used right now, but might be implemented in the future.
 */
@CacheableTask
public class GenerateRestClientTask extends DefaultTask
{
  // Input Task Property
  private File _inputDir;
  private FileCollection _resolverPath;
  private FileCollection _runtimeClasspath;
  private FileCollection _codegenClasspath;
  private boolean _enableArgFile;
  private Boolean _generateLowercasePath;

  // Output Task Property
  private File _destinationDir;

  // Internal Task Properties
  private boolean _restli1FormatSuppressed;
  private boolean _restli2FormatSuppressed;
  private boolean _restli1BuildersDeprecated = true;
  private boolean _generateFluentApi = false;

  @TaskAction
  public void generate()
  {
    PegasusOptions.ClientOptions pegasusClientOptions = new PegasusOptions.ClientOptions();

    // idl input could include rest model jar files
    for (File input : getProject().files(_inputDir))
    {
      if (input.isDirectory())
      {
        for (File f : SharedFileUtils.getSuffixedFiles(getProject(), input, PegasusPlugin.IDL_FILE_SUFFIX))
        {
          if (!pegasusClientOptions.hasRestModelFileName(f.getName()))
          {
            pegasusClientOptions.addClientItem(f.getName(), "", false);
            getProject().getLogger().lifecycle("Add interface file: {}", f.getPath());
          }
        }
      }
    }

    if (pegasusClientOptions.getClientItems().isEmpty())
    {
      return;
    }

    getProject().getLogger().info("Generating REST client builders ...");

    String resolverPathStr = _resolverPath.getAsPath();
    _destinationDir.mkdirs();

    Map<String, List<String>> version1Files = new HashMap<>();
    Map<String, List<String>> version2Files = new HashMap<>();
    List<String> fluentApiFiles = new ArrayList<>();

    getProject().getLogger().lifecycle("Destination directory: {}", _destinationDir);
    getProject().getLogger().lifecycle("Generate fluent apis: {}", _generateFluentApi);

    for (PegasusOptions.ClientItem clientItem : pegasusClientOptions.getClientItems())
    {
      getProject().getLogger().lifecycle("Generating rest client source files for: {}",
          clientItem.restModelFileName);

      String defaultPackage;
      if (clientItem.defaultPackage.equals("") && getProject().hasProperty("idlDefaultPackage"))
      {
        defaultPackage = (String) getProject().property("idlDefaultPackage");
      }
      else
      {
        defaultPackage = clientItem.defaultPackage;
      }


      String restModelFilePath = _inputDir.toString() + File.separatorChar + clientItem.restModelFileName;

      if (!_restli1FormatSuppressed)
      {
        version1Files.computeIfAbsent(defaultPackage, key -> new ArrayList<>()).add(restModelFilePath);
      }

      if (!_restli2FormatSuppressed)
      {
        version2Files.computeIfAbsent(defaultPackage, key -> new ArrayList<>())
            .add(restModelFilePath);
      }

      if (_generateFluentApi)
      {
        fluentApiFiles.add(restModelFilePath);
      }
    }

    String deprecatedVersion = _restli1BuildersDeprecated ? "2.0.0" : null;

    FileCollection _pathedCodegenClasspath;
    try {
      _pathedCodegenClasspath = PathingJarUtil.generatePathingJar(getProject(), getName(),
      _runtimeClasspath.plus(_codegenClasspath), false);
    }
    catch (IOException e)
    {
      throw new GradleException("Error occurred generating pathing JAR.", e);
    }
    version1Files.forEach((defaultPackage, files) ->
      getProject().javaexec(javaExecSpec ->
      {
        List<String> sources = files;
        String resolverPathArg = resolverPathStr;
        if (isEnableArgFile())
        {
          sources = Collections.singletonList(getArgFileSyntax(createArgFile("v1_" + defaultPackage, files, getTemporaryDir())));
          resolverPathArg = ArgumentFileGenerator.getArgFileSyntax(ArgumentFileGenerator.createArgFile(
              "generateRestClient_resolverPath_v1", Collections.singletonList(resolverPathArg), getTemporaryDir()));
        }
        javaExecSpec.setClasspath(_pathedCodegenClasspath);
        javaExecSpec.setMain("com.linkedin.restli.tools.clientgen.RestRequestBuilderGenerator");
        javaExecSpec.jvmArgs("-Dgenerator.resolver.path=" + resolverPathArg); //RestRequestBuilderGenerator.run(resolverPath)
        javaExecSpec.jvmArgs("-Dgenerator.default.package=" + defaultPackage); //RestRequestBuilderGenerator.run(defaultPackage)
        javaExecSpec.jvmArgs("-Dgenerator.generate.imported=false"); //RestRequestBuilderGenerator.run(generateImported)
        javaExecSpec.jvmArgs("-Dgenerator.rest.generate.datatemplates=false"); //RestRequestBuilderGenerator.run(generateDataTemplates)
        javaExecSpec.jvmArgs("-Dgenerator.rest.generate.version=1.0.0"); //RestRequestBuilderGenerator.run(version)
        javaExecSpec.jvmArgs("-Dgenerator.rest.generate.deprecated.version=" + deprecatedVersion); //RestRequestBuilderGenerator.run(deprecatedByVersion)
        if (_generateLowercasePath != null)
        {
          javaExecSpec.jvmArgs("-Dgenerator.rest.generate.lowercase.path=" + _generateLowercasePath); //RestRequestBuilderGenerator.run(generateLowercasePath)
        }
        javaExecSpec.jvmArgs("-Droot.path=" + getProject().getRootDir().getPath());
        javaExecSpec.args(_destinationDir.getAbsolutePath());
        javaExecSpec.args(sources);
      }).assertNormalExitValue()
    );

    version2Files.forEach((defaultPackage, files) ->
      getProject().javaexec(javaExecSpec ->
      {
        List<String> sources = files;
        String resolverPathArg = resolverPathStr;
        if (isEnableArgFile())
        {
          sources = Collections.singletonList(getArgFileSyntax(createArgFile("v2_" + defaultPackage, files, getTemporaryDir())));
          resolverPathArg = ArgumentFileGenerator.getArgFileSyntax(ArgumentFileGenerator.createArgFile(
              "generateRestClient_resolverPath_v2", Collections.singletonList(resolverPathArg), getTemporaryDir()));
        }
        javaExecSpec.setClasspath(_pathedCodegenClasspath);
        javaExecSpec.setMain("com.linkedin.restli.tools.clientgen.RestRequestBuilderGenerator");
        javaExecSpec.jvmArgs("-Dgenerator.resolver.path=" + resolverPathArg); //RestRequestBuilderGenerator.run(resolverPath)
        javaExecSpec.jvmArgs("-Dgenerator.default.package=" + defaultPackage); //RestRequestBuilderGenerator.run(defaultPackage)
        javaExecSpec.jvmArgs("-Dgenerator.generate.imported=false"); //RestRequestBuilderGenerator.run(generateImported)
        javaExecSpec.jvmArgs("-Dgenerator.rest.generate.datatemplates=false"); //RestRequestBuilderGenerator.run(generateDataTemplates)
        javaExecSpec.jvmArgs("-Dgenerator.rest.generate.version=2.0.0"); //RestRequestBuilderGenerator.run(version)
        if (_generateLowercasePath != null)
        {
          javaExecSpec.jvmArgs("-Dgenerator.rest.generate.lowercase.path=" + _generateLowercasePath); //RestRequestBuilderGenerator.run(generateLowercasePath)
        }
        javaExecSpec.jvmArgs("-Droot.path=" + getProject().getRootDir().getPath());
        javaExecSpec.args(_destinationDir.getAbsolutePath());
        javaExecSpec.args(sources);
      }).assertNormalExitValue()
    );

    // Fluent API generator will not generate classes for schemas referenced from IDLs (eg, FooArray for list params).
    // These are already generated by the request builder generators and will be reused.
    if (!fluentApiFiles.isEmpty())
    {
      getProject().getLogger().lifecycle("Generating fluent client bindings for {} files.",
          fluentApiFiles.size());
      getProject().javaexec(javaExecSpec ->
        {
          List<String> sources = fluentApiFiles;
          String resolverPathArg = resolverPathStr;
          if (isEnableArgFile())
          {
            sources = Collections.singletonList(getArgFileSyntax(createArgFile("fluent_", fluentApiFiles, getTemporaryDir())));
            resolverPathArg = getArgFileSyntax(createArgFile(
                "generateRestClient_resolverPath_fluent", Collections.singletonList(resolverPathArg), getTemporaryDir()));
          }
          javaExecSpec.setClasspath(_pathedCodegenClasspath);
          javaExecSpec.setMain("com.linkedin.restli.tools.clientgen.FluentApiGenerator");
          javaExecSpec.args("--resolverPath", resolverPathArg);
          javaExecSpec.args("--rootPath", getProject().getRootDir().getPath());
          javaExecSpec.args("--targetDir", _destinationDir.getAbsolutePath());
          javaExecSpec.args(sources);
        }).assertNormalExitValue();
      }
  }


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
  public FileCollection getRuntimeClasspath()
  {
    return _runtimeClasspath;
  }

  public void setRuntimeClasspath(FileCollection runtimeClasspath)
  {
    _runtimeClasspath = runtimeClasspath;
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

  @Input
  public boolean isEnableArgFile()
  {
    return _enableArgFile;
  }

  public void setEnableArgFile(boolean enable)
  {
    _enableArgFile = enable;
  }

  /**
   * @deprecated by {@link #isGenerateLowercasePath()} ()} because Gradle 7
   *     requires input and output properties to be annotated on getters, which
   *     have a prefix of "is" or "get".
   */
  @Deprecated
  public Boolean generateLowercasePath()
  {
    return isGenerateLowercasePath();
  }

  @Optional
  @Input
  public Boolean isGenerateLowercasePath()
  {
    return _generateLowercasePath;
  }

  public void setGenerateLowercasePath(Boolean enable)
  {
    _generateLowercasePath = enable;
  }

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
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isRestli2FormatSuppressed()} instead
   */
  @Deprecated
  @Internal
  public boolean getIsRestli2FormatSuppressed()
  {
    return isRestli2FormatSuppressed();
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isRestli2FormatSuppressed()} instead
   */
  @Deprecated
  @Internal
  public boolean isIsRestli2FormatSuppressed()
  {
    return isRestli2FormatSuppressed();
  }

  @Internal
  public boolean isRestli2FormatSuppressed()
  {
    return _restli2FormatSuppressed;
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * setter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #setRestli2FormatSuppressed(boolean)} instead
   */
  @Deprecated
  public void setIsRestli2FormatSuppressed(boolean restli2FormatSuppressed)
  {
    setRestli2FormatSuppressed(restli2FormatSuppressed);
  }

  public void setRestli2FormatSuppressed(boolean restli2FormatSuppressed)
  {
    _restli2FormatSuppressed = restli2FormatSuppressed;
  }

  @Internal
  public boolean isRestli1FormatSuppressed()
  {
    return _restli1FormatSuppressed;
  }

  public void setRestli1FormatSuppressed(boolean restli1FormatSuppressed)
  {
    _restli1FormatSuppressed = restli1FormatSuppressed;
  }

  @Internal
  public boolean isGenerateFluentApi()
  {
    return _generateFluentApi;
  }

  public void setGenerateFluentApi(boolean generateFluentApi)
  {
    _generateFluentApi = generateFluentApi;
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isRestli1BuildersDeprecated()} instead
   */
  @Deprecated
  @Internal
  public boolean get_isRestli1BuildersDeprecated()
  {
    return isRestli1BuildersDeprecated();
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * getter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #isRestli1BuildersDeprecated()} instead
   */
  @Deprecated
  @Internal
  public boolean is_isRestli1BuildersDeprecated()
  {
    return isRestli1BuildersDeprecated();
  }

  @Internal
  public boolean isRestli1BuildersDeprecated()
  {
    return _restli1BuildersDeprecated;
  }

  /**
   * This method is kept for backwards compatibility.
   * <p>
   * A Groovy property with this name was exposed, which leads to this lengthy
   * setter name. In Java, boolean fields are named without the "is" prefix.
   *
   * @deprecated use {@link #setRestli1BuildersDeprecated(boolean)} instead
   */
  @Deprecated
  public void set_isRestli1BuildersDeprecated(boolean restli1BuildersDeprecated)
  {
    setRestli1BuildersDeprecated(restli1BuildersDeprecated);
  }

  public void setRestli1BuildersDeprecated(boolean restli1BuildersDeprecated)
  {
    _restli1BuildersDeprecated = restli1BuildersDeprecated;
  }
}
