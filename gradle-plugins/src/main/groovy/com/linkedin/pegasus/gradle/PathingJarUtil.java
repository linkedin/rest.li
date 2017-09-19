package com.linkedin.pegasus.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;


/**
 * Utilities for creating and handling pathing JARs.
 */
public class PathingJarUtil {

  private final static Logger LOG = Logging.getLogger(PathingJarUtil.class);

  /**
   * Creates a pathing JAR to reference for a {@link org.gradle.api.tasks.JavaExec} task. This is used to address long
   * classpath failures in Java processes. We pile all of the non-directory dependencies into a single jar, whose
   * manifest contains relative references to all of these dependencies. The result, the classpath is dramatically
   * shorter and the task still has access to all of the same dependencies.
   *
   * @param project the {@link Project}
   * @param taskName the name of the task to create the pathing JAR for
   * @param classpath the classpath for the task
   * @param alwaysUsePathingJar pathing jar is created when set to true,
   *  else depending on the inclusion of 'restli-tools-scala' pathing jar may not be created
   * @return the new classpath for the task
   * @throws IOException if there any issues creating the pathing JAR
   */
  public static FileCollection generatePathingJar(final Project project, final String taskName, final FileCollection classpath,
    boolean alwaysUsePathingJar) throws IOException {
    //There is a bug in the Scala nsc compiler that does not parse the dependencies of JARs in the JAR manifest
    //As such, we disable pathing for any libraries compiling docs for Scala resources
    if (!alwaysUsePathingJar && !classpath.filter(f -> f.getAbsolutePath().contains("restli-tools-scala")).isEmpty()) {
      LOG.info("Compiling Scala resource classes. Disabling pathing jar for " + taskName + " to avoid breaking Scala compilation");
      return classpath;
    }

    //We extract the classpath from the target task here, in the configuration phase
    //Note that we don't invoke getFiles() here because that would trigger dependency resolution in configuration phase
    FileCollection filteredClasspath = classpath.filter(f -> !f.isDirectory());
    File destinationDir = new File(project.getBuildDir(), taskName);
    destinationDir.mkdirs();
    File pathingJarPath = new File(destinationDir, project.getName() + "-pathing.jar");
    OutputStream pathingJar = new FileOutputStream(pathingJarPath);

    //Classpath manifest does not support directories and needs to contain relative paths
    String cp = ClasspathManifest.relativeClasspathManifest(destinationDir, filteredClasspath.getFiles());

    //Create the JAR
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, cp);
    JarOutputStream jarOutputStream = new JarOutputStream(pathingJar, manifest);
    jarOutputStream.close();

    return classpath.filter(File::isDirectory).plus(project.files(pathingJarPath));
  }
}
