package com.linkedin.pegasus.generator;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.MaskMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.sun.codemodel.JClass;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class TestProjectionMaskApiChecker {
  private static final String FS = File.separator;
  private static final String testDir = System.getProperty("testDir", new File("src/test").getAbsolutePath());
  private static final String pegasusDir = testDir + FS + "resources" + FS + "generator";

  @Mock TemplateSpecGenerator _templateSpecGenerator;
  ClassLoader _classLoader;
  Set<File> _sourceFiles;
  @Mock ClassTemplateSpec _templateSpec;
  @Mock DataSchemaLocation _location;
  @Mock File _nestedTypeSource;
  @Mock JClass _nestedType;
  @Mock ClassLoader _mockClassLoader;

  @BeforeMethod
  private void beforeMethod() throws IOException
  {
    MockitoAnnotations.initMocks(this);
    _sourceFiles = new HashSet<>();
    _sourceFiles.addAll(Arrays.asList(new File(pegasusDir).listFiles()));
    _classLoader = getClass().getClassLoader();

    Mockito.when(_templateSpecGenerator.getClassLocation(_templateSpec)).thenReturn(_location);
    Mockito.when(_location.getSourceFile()).thenReturn(_nestedTypeSource);
  }

  @Test
  public void testGeneratedFromSource() throws  Exception
  {
    ProjectionMaskApiChecker projectionMaskApiChecker = new ProjectionMaskApiChecker(
        _templateSpecGenerator, _sourceFiles, _classLoader);
    Mockito.when(_nestedTypeSource.getAbsolutePath()).thenReturn(pegasusDir + FS + "Bar.pdl");

    Assert.assertTrue(projectionMaskApiChecker.isGeneratedFromSource(_templateSpec));
    Mockito.verify(_nestedTypeSource, Mockito.atLeast(1)).getAbsolutePath();
  }

  @Test
  public void testGeneratedFromSourceExternal() throws  Exception
  {
    ProjectionMaskApiChecker projectionMaskApiChecker = new ProjectionMaskApiChecker(
        _templateSpecGenerator, _sourceFiles, _classLoader);
    Mockito.when(_nestedTypeSource.getAbsolutePath()).thenReturn("models.jar:/Bar.pdl");

    Assert.assertFalse(projectionMaskApiChecker.isGeneratedFromSource(_templateSpec));
    Mockito.verify(_nestedTypeSource, Mockito.atLeast(1)).getAbsolutePath();
  }

  @Test
  public void testHasProjectionMaskApiClassFoundWithoutProjectionMask() throws  Exception
  {
    ProjectionMaskApiChecker projectionMaskApiChecker = new ProjectionMaskApiChecker(
        _templateSpecGenerator, _sourceFiles, _classLoader);
    Mockito.when(_nestedTypeSource.getAbsolutePath()).thenReturn(pegasusDir + FS + "Bar.pdl");
    Mockito.when(_nestedType.fullName()).thenReturn(FakeRecord.class.getName());

    Assert.assertFalse(projectionMaskApiChecker.hasProjectionMaskApi(_nestedType, _templateSpec));
    Mockito.verify(_nestedType, Mockito.times(1)).fullName();
    Mockito.verify(_nestedTypeSource, Mockito.never()).getAbsolutePath();
  }

  @Test
  public void testHasProjectionMaskApiClassFoundWithProjectionMask() throws  Exception
  {
    ProjectionMaskApiChecker projectionMaskApiChecker = new ProjectionMaskApiChecker(
        _templateSpecGenerator, _sourceFiles, _classLoader);
    Mockito.when(_nestedTypeSource.getAbsolutePath()).thenReturn(pegasusDir + FS + "Bar.pdl");
    Mockito.when(_nestedType.fullName()).thenReturn(FakeRecordWithProjectionMask.class.getName());

    Assert.assertTrue(projectionMaskApiChecker.hasProjectionMaskApi(_nestedType, _templateSpec));
    Mockito.verify(_nestedType, Mockito.times(1)).fullName();
    Mockito.verify(_nestedTypeSource, Mockito.never()).getAbsolutePath();
  }

  @Test
  public void testHasProjectionMaskApiGeneratedFromSource() throws  Exception
  {
    ProjectionMaskApiChecker projectionMaskApiChecker = new ProjectionMaskApiChecker(
        _templateSpecGenerator, _sourceFiles, _mockClassLoader);
    Mockito.when(_nestedTypeSource.getAbsolutePath()).thenReturn(pegasusDir + FS + "Bar.pdl");
    Mockito.when(_nestedType.fullName()).thenReturn("com.linkedin.common.AuditStamp");
    Mockito.when(_mockClassLoader.loadClass("com.linkedin.common.AuditStamp")).thenThrow(
        new ClassNotFoundException());

    Assert.assertTrue(projectionMaskApiChecker.hasProjectionMaskApi(_nestedType, _templateSpec));
    Mockito.verify(_mockClassLoader, Mockito.times(1)).loadClass(Mockito.anyString());
    Mockito.verify(_nestedType, Mockito.times(1)).fullName();
    Mockito.verify(_nestedTypeSource, Mockito.times(1)).getAbsolutePath();
  }

  @Test
  public void testHasProjectionMaskApiExternal() throws  Exception
  {
    ProjectionMaskApiChecker projectionMaskApiChecker = new ProjectionMaskApiChecker(
        _templateSpecGenerator, _sourceFiles, _mockClassLoader);
    Mockito.when(_nestedTypeSource.getAbsolutePath()).thenReturn("models.jar:/AuditStamp.pdl");
    Mockito.when(_nestedType.fullName()).thenReturn("com.linkedin.common.AuditStamp");
    Mockito.when(_mockClassLoader.loadClass("com.linkedin.common.AuditStamp")).thenThrow(
        new ClassNotFoundException());

    Assert.assertFalse(projectionMaskApiChecker.hasProjectionMaskApi(_nestedType, _templateSpec));
    Mockito.verify(_mockClassLoader, Mockito.times(1)).loadClass(Mockito.anyString());
    Mockito.verify(_nestedType, Mockito.times(1)).fullName();
    Mockito.verify(_nestedTypeSource, Mockito.times(1)).getAbsolutePath();
  }

  private static class FakeRecord extends RecordTemplate
  {
    protected FakeRecord(DataMap map, RecordDataSchema schema)
    {
      super(map, schema);
    }
  }

  private static class FakeRecordWithProjectionMask extends RecordTemplate
  {
    protected FakeRecordWithProjectionMask(DataMap map, RecordDataSchema schema)
    {
      super(map, schema);
    }

    public static class ProjectionMask extends MaskMap
    {

    }
  }
}
