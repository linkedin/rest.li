package com.linkedin.pegasus.generator;

import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.sun.codemodel.JClass;
import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Utility to check if a nested type has or will generate projection mask APIs.
 */
public class ProjectionMaskApiChecker
{
  private final TemplateSpecGenerator _specGenerator;
  private final Set<String> _sourceFiles;
  private final ClassLoader _classLoader;

  ProjectionMaskApiChecker(TemplateSpecGenerator specGenerator,
      Set<File> sourceFiles, ClassLoader classLoader)
  {
    _specGenerator = specGenerator;
    _sourceFiles = sourceFiles.stream().map(File::getAbsolutePath).collect(Collectors.toSet());
    _classLoader = classLoader;
  }

  /**
   * Returns true if any of the conditions below is true.
   * <ul>
   *   <li>The passed in class can be loaded from class path and also contains the ProjectionMask class.</li>
   *   <li>The passed in class will be generated from a source PDL file on which the template generator is running.</li>
   * </ul>
   */
  boolean hasProjectionMaskApi(JClass definedClass, ClassTemplateSpec templateSpec)
  {
    try
    {
      final Class<?> clazz = _classLoader.loadClass(definedClass.fullName());
      return Arrays.stream(clazz.getClasses()).anyMatch(c -> c.getSimpleName().equals("ProjectionMask"));
    }
    catch (ClassNotFoundException e)
    {
      // Ignore, and check if the class will be generated from a source PDL
    }
    return isGeneratedFromSource(templateSpec);
  }

  /**
   * Returns true if the provided class is generated from one of the source PDLs.
   */
  boolean isGeneratedFromSource(ClassTemplateSpec templateSpec)
  {
    DataSchemaLocation location = _specGenerator.getClassLocation(templateSpec);
    return location != null && _sourceFiles.contains(location.getSourceFile().getAbsolutePath());
  }
}
