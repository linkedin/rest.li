package com.linkedin.restli.internal.tools;

import com.linkedin.data.schema.annotation.SchemaAnnotationHandler;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This utility is used to get Java classes based on the provided class jar paths
 *
 * @author Yingjie
 */
public class ClassJarPathUtil
{
  private static final Logger _logger = LoggerFactory.getLogger(ClassJarPathUtil.class);
  private static final String DEFAULT_PATH_SEPARATOR = File.pathSeparator;

  /**
   * A helper method which is used to get the SchemaAnnotationHandler classes based on the given handlerJarPaths and class names
   * @param handlerJarPaths
   * @param classNames
   * @return a list of SchemaAnnotationHandler classes. List<SchemaAnnotationHandler>
   * @throws IllegalStateException if it could not instantiate the given class.
   */
  public static List<SchemaAnnotationHandler> getAnnotationHandlers(String handlerJarPaths, String classNames) throws IllegalStateException
  {
    List<SchemaAnnotationHandler> handlers = new ArrayList<>();
    ClassLoader classLoader = new URLClassLoader(parsePaths(handlerJarPaths)
        .stream()
        .map(str ->
        {
          try
          {
            return Paths.get(str).toUri().toURL();
          }
          catch (Exception e)
          {
            _logger.error("Parsing class jar path URL {} parsing failed", str, e);
          }
          return null;
        }).filter(Objects::nonNull).toArray(URL[]::new));

    for (String className: parsePaths(classNames))
    {
      try
      {
        Class<?> handlerClass = Class.forName(className, false, classLoader);
        SchemaAnnotationHandler handler = (SchemaAnnotationHandler) handlerClass.newInstance();
        handlers.add(handler);
      }
      catch (Exception e)
      {
        throw new IllegalStateException("Error instantiating class: " + className + e.getMessage(), e);
      }
    }
    return handlers;
  }

  /**
   * A helper method to get a list of class paths from a pathString.
   * @param pathAsStr
   * @return a list of class paths. List<String>
   */
  public static List<String> parsePaths(String pathAsStr)
  {
    List<String> list = new ArrayList<>();
    if (pathAsStr != null)
    {
      StringTokenizer tokenizer = new StringTokenizer(pathAsStr, DEFAULT_PATH_SEPARATOR);
      while (tokenizer.hasMoreTokens())
      {
        list.add(tokenizer.nextToken());
      }
    }
    return list;
  }
}
