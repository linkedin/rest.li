package com.linkedin.pegasus.gradle

import org.gradle.util.GFileUtils
import org.testng.annotations.Test

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class TestIOUtil {

  @Test
  void writesTextOnNonExistentFile() {
    Path tempDirectory = Files.createTempDirectory(getClass().getSimpleName())
    File f = tempDirectory.resolve("foo/bar/baz.txt").toFile()

    f.delete()
    f.parentFile.delete()

    assert !f.exists()
    assert !f.parentFile.exists()

    //when
    IOUtil.writeText(f, "foo")

    //then
    assert GFileUtils.readFile(f) == "foo"

    deleteDirectory(tempDirectory)
  }

  private static void deleteDirectory(Path directory) {
    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
      @Override
      FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.delete(file)
        return FileVisitResult.CONTINUE
      }

      @Override
      FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        Files.delete(dir)
        return FileVisitResult.CONTINUE;
      }
    });
  }
}
