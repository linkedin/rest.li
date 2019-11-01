package com.linkedin.pegasus.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.internal.FileUtils;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.util.GFileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static org.testng.Assert.*;


public final class TestPathingJarUtil
{
  private File temp;

  private void createTempDir() {
    temp = new File("/tmp/TestPathingJarUtil");
    temp.mkdir();
  }

  private void cleanupTempDir() {
    GFileUtils.deleteDirectory(temp);
  }


  @Test
  public void testCreatesGeneratesPathingJar() throws IOException
  {
    //setup
    createTempDir();
    Project project = ProjectBuilder.builder().withProjectDir(temp).build();
    String taskName = "myTaskName";
    project.getBuildDir().mkdir();
    System.out.println(project.getBuildDir().getAbsolutePath());
    File tempFile = new File(project.getBuildDir(), "temp1.class");
    GFileUtils.touch(tempFile);
    FileCollection files = project.files(tempFile);

    //when
    PathingJarUtil.generatePathingJar(project, taskName, files, true);
    File pathingJar = new File(project.getBuildDir(), taskName + '/' + project.getName() + "-pathing.jar");
    assertTrue(pathingJar.exists());
    JarInputStream jarStream = new JarInputStream(new FileInputStream(pathingJar));
    Manifest manifest = jarStream.getManifest();
    assertTrue(manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH).contains("temp1.class"));

    cleanupTempDir();
  }

  @Test
  public void testDoesNotCreatePathingJar() throws IOException
  {
    //setup
    createTempDir();
    Project project = ProjectBuilder.builder().withProjectDir(temp).build();
    String taskName = "myTaskName";

    project.getBuildDir().mkdir();
    File tempFile = new File(project.getBuildDir(), "temp.class");
    File restliTools = new File(project.getBuildDir(), "restli-tools-scala");

    GFileUtils.touch(tempFile);
    GFileUtils.touch(restliTools);
    FileCollection files = project.files(tempFile, restliTools);

    //when
    File pathingJar = new File(project.getBuildDir(), taskName + '/' + project.getName() + "-pathing.jar");
    PathingJarUtil.generatePathingJar(project, taskName, files, false);
    assertFalse(pathingJar.exists());

    cleanupTempDir();
  }
}
