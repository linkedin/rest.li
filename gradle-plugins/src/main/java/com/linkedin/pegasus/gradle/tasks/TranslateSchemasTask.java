package com.linkedin.pegasus.gradle.tasks;

import com.linkedin.pegasus.gradle.PathingJarUtil;
import com.linkedin.pegasus.gradle.SchemaFileType;
import com.linkedin.pegasus.gradle.internal.ArgumentFileGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;


/**
 * Translates files between the .pdsc and .pdl Pegasus schema formats.
 */
@CacheableTask
public class TranslateSchemasTask extends DefaultTask {
  private FileCollection _inputDirs = getProject().files();
  private FileCollection _resolverPath;
  private FileCollection _codegenClasspath;
  private File _destinationDir;
  private SchemaFileType _sourceFormat = SchemaFileType.PDSC;
  private SchemaFileType _destinationFormat = SchemaFileType.PDL;
  private boolean _keepOriginal = false;
  private String _preserveSourceCmd;
  private boolean _skipVerification = false;
  private boolean _forcePdscFullyQualifedNames = false;
  private boolean _enableArgFile;

  @TaskAction
  public void translate()
  {
    getProject().getLogger().info("Translating data schemas ...");
    if (_destinationDir != null) {
      _destinationDir.mkdirs();
    }

    String resolverPathStr = _resolverPath.plus(_inputDirs).getAsPath();

    FileCollection pathedCodegenClasspath;
    try
    {
      pathedCodegenClasspath = PathingJarUtil.generatePathingJar(getProject(), getName(),
          _codegenClasspath, false);
    }
    catch (IOException e)
    {
      throw new GradleException("Error occurred generating pathing JAR.", e);
    }

    for (File dir : _inputDirs.getFiles()) {
      if (dir.exists() && (!dir.isDirectory() || dir.list().length > 0)) {
        javaExec(resolverPathStr, pathedCodegenClasspath, dir);
      }
    }
  }

  private void javaExec(String resolverPathStr, FileCollection pathedCodegenClasspath, File inputDir) {
    getProject().javaexec(javaExecSpec ->
    {
      String resolverPathArg = resolverPathStr;
      if (isEnableArgFile())
      {
        resolverPathArg = ArgumentFileGenerator.getArgFileSyntax(ArgumentFileGenerator.createArgFile(
            "translateSchemas_resolverPath", Collections.singletonList(resolverPathArg), getTemporaryDir()));
      }
      javaExecSpec.setMain("com.linkedin.restli.tools.data.SchemaFormatTranslator");
      javaExecSpec.setClasspath(pathedCodegenClasspath);
      javaExecSpec.args("--source-format");
      javaExecSpec.args(_sourceFormat.getFileExtension());
      javaExecSpec.args("--destination-format");
      javaExecSpec.args(_destinationFormat.getFileExtension());
      if (_keepOriginal)
      {
        javaExecSpec.args("--keep-original");
      }
      if (_preserveSourceCmd != null)
      {
        javaExecSpec.args("--preserve-source");
        javaExecSpec.args(_preserveSourceCmd);
      }
      if (_skipVerification)
      {
        javaExecSpec.args("--skip-verification");
      }
      if (_forcePdscFullyQualifedNames)
      {
        javaExecSpec.args("--force-pdsc-fully-qualified-names");
      }
      javaExecSpec.args(resolverPathArg);
      javaExecSpec.args(inputDir.getAbsolutePath());
      if (_destinationDir == null) {
        javaExecSpec.args(inputDir.getAbsolutePath());
      } else {
        javaExecSpec.args(_destinationDir.getAbsolutePath());
      }
    });
  }

  /**
   * Directories containing the data schema files to translate.
   */
  @InputFiles
  @SkipWhenEmpty
  public FileCollection getInputDirs()
  {
    return _inputDirs;
  }

  public void setInputDirs(FileCollection fileCollection) {
    _inputDirs = fileCollection;
  }

  @Deprecated
  public File getInputDir()
  {
    return _inputDirs.getSingleFile();
  }

  @Deprecated
  public void setInputDir(File inputDir)
  {
    _inputDirs = getProject().files(inputDir);
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
   * Directory in which to write the translated files.
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

  @Input
  public SchemaFileType getSourceFormat()
  {
    return _sourceFormat;
  }

  public void setSourceFormat(SchemaFileType sourceFormat)
  {
    _sourceFormat = sourceFormat;
  }

  @Input
  public SchemaFileType getDestinationFormat()
  {
    return _destinationFormat;
  }

  public void setDestinationFormat(SchemaFileType destinationFormat)
  {
    _destinationFormat = destinationFormat;
  }

  @Input
  public boolean isKeepOriginal()
  {
    return _keepOriginal;
  }

  public void setKeepOriginal(boolean keepOriginal)
  {
    _keepOriginal = keepOriginal;
  }

  @Input
  @Optional
  public String getPreserveSourceCmd()
  {
    return _preserveSourceCmd;
  }

  public void setPreserveSourceCmd(String preserveSourceCmd)
  {
    _preserveSourceCmd = preserveSourceCmd;
  }

  @Input
  public boolean isSkipVerification()
  {
    return _skipVerification;
  }

  public void setSkipVerification(boolean skipVerification)
  {
    _skipVerification = skipVerification;
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

  @Input
  public boolean isForcePdscFullyQualifedNames()
  {
    return _forcePdscFullyQualifedNames;
  }

  public void setForcePdscFullyQualifedNames(boolean forcePdscFullyQualifedNames)
  {
    _forcePdscFullyQualifedNames = forcePdscFullyQualifedNames;
  }
}
