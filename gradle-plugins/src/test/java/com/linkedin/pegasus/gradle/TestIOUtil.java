package com.linkedin.pegasus.gradle;

import org.gradle.util.GFileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.testng.Assert.*;


public final class TestIOUtil
{
  @Test
  public void writesTextOnNonExistentFile() throws IOException
  {
    Path tempDirectory = Files.createTempDirectory(getClass().getSimpleName());
    File f = tempDirectory.resolve("foo/bar/baz.txt").toFile();

    f.delete();
    f.getParentFile().delete();

    assertFalse(f.exists());
    assertFalse(f.getParentFile().exists());

    //when
    IOUtil.writeText(f, "foo");

    //then
    assertEquals(GFileUtils.readFile(f), "foo");

    deleteDirectory(tempDirectory);
  }

  private static void deleteDirectory(Path directory) throws IOException
  {
    Files.walkFileTree(directory, new SimpleFileVisitor<Path>()
    {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
      {
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
      {
        Files.delete(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
