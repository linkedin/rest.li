package com.linkedin.pegasus.gradle.tasks;

import com.linkedin.pegasus.gradle.SchemaFileType;
import java.io.File;
import org.gradle.api.DefaultTask;
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


/**
 * Translates files between the .pdsc and .pdl Pegasus schema formats.
 */
@CacheableTask
public class TranslateSchemasTask extends DefaultTask {
  private File _inputDir;
  private FileCollection _resolverPath;
  private FileCollection _codegenClasspath;
  private File _destinationDir;
  private SchemaFileType _sourceFormat = SchemaFileType.PDSC;
  private SchemaFileType _destinationFormat = SchemaFileType.PDL;
  private boolean _keepOriginal = false;
  private String _preserveSourceCmd;

  @TaskAction
  public void translate()
  {
    getProject().getLogger().info("Translating data schemas ...");
    _destinationDir.mkdirs();

    String resolverPathStr = _resolverPath.plus(getProject().files(_inputDir)).getAsPath();

    getProject().javaexec(javaExecSpec -> {
      javaExecSpec.setMain("com.linkedin.restli.tools.data.SchemaFormatTranslator");
      javaExecSpec.setClasspath(_codegenClasspath);
      javaExecSpec.jvmArgs("-Dgenerator.resolver.path=" + resolverPathStr);
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
      javaExecSpec.args(resolverPathStr);
      javaExecSpec.args(_inputDir.getAbsolutePath());
      javaExecSpec.args(_destinationDir.getAbsolutePath());
    });
  }

  /**
   * Directory containing the data schema files to translate.
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
   * Directory to write the translated files.
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
}
