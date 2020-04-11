package com.linkedin.pegasus.gradle.tasks;

import com.linkedin.pegasus.gradle.PathingJarUtil;
import com.linkedin.pegasus.gradle.PegasusOptions;
import com.linkedin.pegasus.gradle.internal.ArgumentFileGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;


/**
 * Generate the idl file from the annotated java classes. This also requires access to the
 * classes that were used to compile these java classes.
 * Projects with no IdlItem will be excluded from this task
 *
 * As prerequisite of this task, add these lines to your build.gradle:
 * <pre>
 * apply plugin: 'li-pegasus2'
 * </pre>
 *
 * Optionally, to generate idl for specific packages, add
 * <pre>
 * pegasus.&lt;sourceSet&gt;.idlOptions.addIdlItem(['&lt;packageName&gt;'])
 * </pre>
 */
@CacheableTask
public class GenerateRestModelTask extends DefaultTask
{
  public static final String INCLUDED_SOURCE_TYPES_PROPERTY = "pegasus.generateRestModel.includedSourceTypes";

  private FileCollection _watchedCodegenClasspath;
  private Set<File> _watchedInputDirs;
  private FileCollection _resolverPath;
  private File _idlDestinationDir;
  private File _snapshotDestinationDir;
  private PegasusOptions.IdlOptions _idlOptions;
  private FileCollection _pathedCodegenClasspath;
  private boolean _enableArgFile;

  // we make a separation between watched and unwatched variables to create a stricter definition for incremental builds.
  // In this case, the unwatched directories and classes include all of the main source sets for the application. This
  // creates a nasty use case where the user updates a utility class, but the rest models still regenerate. In Gradle 4.0
  // this will no longer be needed with the introduction of normalizing filters.
  private FileCollection _codegenClasspath;
  private Set<File> _inputDirs;

  @TaskAction
  public void generate()
  {
    List<String> inputDirPaths = _inputDirs.stream().map(File::getPath).collect(Collectors.toList());

    getProject().getLogger().debug("GenerateRestModel using input directories {}", inputDirPaths);
    getProject().getLogger().debug("GenerateRestModel using destination dir {}", _idlDestinationDir.getPath());
    _snapshotDestinationDir.mkdirs();
    _idlDestinationDir.mkdirs();

    @SuppressWarnings("unchecked")
    List<String> includedSourceTypes = (List<String>) getProject().findProperty(INCLUDED_SOURCE_TYPES_PROPERTY);

    boolean ignoreNonJavaFiles = includedSourceTypes != null
        && includedSourceTypes.size() == 1 && includedSourceTypes.contains("JAVA");

    try
    {
      _pathedCodegenClasspath = PathingJarUtil.generatePathingJar(
          getProject(), "generateRestModel", _codegenClasspath, ignoreNonJavaFiles);
    }
    catch (IOException e)
    {
      throw new GradleException("Error occurred generating pathing JAR.", e);
    }

    // handle multiple idl generations in the same project, see pegasus rest-framework-server-examples
    // for example.
    // by default, scan in all source files for annotated java classes.
    // specifically, to scan in certain packages, use
    //   pegasus.<sourceSet>.idlOptions.addIdlItem(['<packageName>'])
    // where [<packageName>] is the array of packages that should be searched for annotated java classes.
    // for example:
    // pegasus.main.idlOptions.addIdlItem(['com.linkedin.groups.server.rest.impl', 'com.linkedin.greetings.server.rest.impl'])
    // they will still be placed in the same jar, though

    boolean loadAdditionalDocProviders = !ignoreNonJavaFiles
        && getProject().getTasks().findByName("scaladoc") != null;

    if (_idlOptions.getIdlItems().isEmpty())
    {
      executeSnapshotExporter(inputDirPaths, _snapshotDestinationDir.getPath(), loadAdditionalDocProviders);
      executeResourceExporter(inputDirPaths, _idlDestinationDir.getPath(), loadAdditionalDocProviders);
    }
    else
    {
      for (PegasusOptions.IdlItem idlItem : _idlOptions.getIdlItems())
      {
        if (idlItem.apiName.isEmpty())
        {
          getProject().getLogger().info("Generating interface for unnamed api ...");
        }
        else
        {
          getProject().getLogger().info("Generating interface for api: {} ...", idlItem.apiName);
        }

        executeSnapshotExporter(idlItem.apiName, inputDirPaths, Arrays.asList(idlItem.packageNames),
                                _snapshotDestinationDir.getPath(), loadAdditionalDocProviders);
        executeResourceExporter(idlItem.apiName, inputDirPaths, Arrays.asList(idlItem.packageNames),
                                _idlDestinationDir.getPath(), loadAdditionalDocProviders);
      }
    }
  }

  @Classpath
  public FileCollection getWatchedCodegenClasspath()
  {
    return _watchedCodegenClasspath;
  }

  public void setWatchedCodegenClasspath(FileCollection watchedCodegenClasspath)
  {
    _watchedCodegenClasspath = watchedCodegenClasspath;
  }

  @InputFiles
  @SkipWhenEmpty
  @PathSensitive(PathSensitivity.RELATIVE)
  public Set<File> getWatchedInputDirs()
  {
    return _watchedInputDirs;
  }

  public void setWatchedInputDirs(Set<File> watchedInputDirs)
  {
    _watchedInputDirs = watchedInputDirs;
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

  @OutputDirectory
  @PathSensitive(PathSensitivity.NAME_ONLY)
  public File getIdlDestinationDir()
  {
    return _idlDestinationDir;
  }

  public void setIdlDestinationDir(File idlDestinationDir)
  {
    _idlDestinationDir = idlDestinationDir;
  }

  @OutputDirectory
  public File getSnapshotDestinationDir()
  {
    return _snapshotDestinationDir;
  }

  public void setSnapshotDestinationDir(File snapshotDestinationDir)
  {
    _snapshotDestinationDir = snapshotDestinationDir;
  }

  @Internal
  public PegasusOptions.IdlOptions getIdlOptions()
  {
    return _idlOptions;
  }

  public void setIdlOptions(PegasusOptions.IdlOptions idlOptions)
  {
    _idlOptions = idlOptions;
  }

  @Internal
  public FileCollection getCodegenClasspath() {
    return _codegenClasspath;
  }

  public void setCodegenClasspath(FileCollection codegenClasspath) {
    _codegenClasspath = codegenClasspath;
  }

  @Internal
  public Set<File> getInputDirs() {
    return _inputDirs;
  }

  public void setInputDirs(Set<File> inputDirs) {
    _inputDirs = inputDirs;
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


  private void executeSnapshotExporter(List<String> inputDirs, String destinationPath, boolean additionalDocProviders)
  {
    executeSnapshotExporter(null, inputDirs, null, destinationPath, additionalDocProviders);
  }

  private void executeSnapshotExporter(String name, List<String> inputDirs, List<String> packages, String destinationPath,
                                       boolean additionalDocProviders)
  {
    getProject().javaexec(javaExecSpec ->
    {
      String resolverPathArg = _resolverPath.getAsPath();
      if (isEnableArgFile())
      {
        resolverPathArg = ArgumentFileGenerator.getArgFileSyntax(ArgumentFileGenerator.createArgFile(
            "generateRestModel_resolverPath", Collections.singletonList(resolverPathArg), getTemporaryDir()));
      }

      javaExecSpec.setMain("com.linkedin.restli.tools.snapshot.gen.RestLiSnapshotExporterCmdLineApp");
      javaExecSpec.setClasspath(_pathedCodegenClasspath);
      javaExecSpec.jvmArgs("-Dgenerator.resolver.path=" + resolverPathArg);
      javaExecSpec.systemProperty("scala.usejavacp", "true");
      if (name != null)
      {
        javaExecSpec.args("-name", name);
      }
      javaExecSpec.args(prepend("-sourcepath", inputDirs));
      javaExecSpec.args("-outdir", destinationPath);
      if (packages != null)
      {
        javaExecSpec.args(prepend("-resourcepackages", packages));
      }
      if (additionalDocProviders)
      {
        javaExecSpec.args("-loadAdditionalDocProviders");
      }
    });
  }

  private void executeResourceExporter(List<String> inputDirs, String destinationPath, boolean additionalDocProviders)
  {
    executeResourceExporter(null, inputDirs, null, destinationPath, additionalDocProviders);
  }

  private void executeResourceExporter(String name, List<String> inputDirs, List<String> packages, String destinationPath,
                                       boolean additionalDocProviders)
  {
    getProject().javaexec(javaExecSpec ->
    {
      javaExecSpec.setMain("com.linkedin.restli.tools.idlgen.RestLiResourceModelExporterCmdLineApp");
      javaExecSpec.setClasspath(_pathedCodegenClasspath);
      javaExecSpec.systemProperty("scala.usejavacp", "true");
      if (name != null)
      {
        javaExecSpec.args("-name", name);
      }
      javaExecSpec.args(prepend("-sourcepath", inputDirs));
      javaExecSpec.args("-outdir", destinationPath);
      if (packages != null)
      {
        javaExecSpec.args(prepend("-resourcepackages", packages));
      }
      if (additionalDocProviders)
      {
        javaExecSpec.args("-loadAdditionalDocProviders");
      }
    });
  }

  private static <T> List<T> prepend(T first, List<T> rest)
  {
    return Stream.concat(Stream.of(first), rest.stream()).collect(Collectors.toList());
  }
}
