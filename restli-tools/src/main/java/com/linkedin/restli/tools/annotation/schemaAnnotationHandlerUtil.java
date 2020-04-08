/*
   Copyright (c) 2020 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.linkedin.restli.tools.annotation;

import com.linkedin.data.schema.annotation.SchemaAnnotationHandler;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.data.schema.resolver.FileDataSchemaResolver.*;


public class schemaAnnotationHandlerUtil
{
  private static final Logger _logger = LoggerFactory.getLogger(schemaAnnotationHandlerUtil.class);

  public static List<SchemaAnnotationHandler> getSchemaAnnotationHandlers(String handlerJarPaths, String handlerClassNames) {
    List<SchemaAnnotationHandler> handlers = new ArrayList<>();

    List<String> handlerJarPathsArray = parsePaths(handlerJarPaths);
    // Use Jar Paths to initiate URL class loaders.
    ClassLoader classLoader = new URLClassLoader(handlerJarPathsArray.stream().map(str -> {
      try {
        return Paths.get(str).toUri().toURL();
      } catch (Exception e) {
        _logger.error("URL {} parsing failed", str, e);
      }
      return null;
    }).filter(Objects::nonNull).toArray(URL[]::new));

    for (String className : parsePaths(handlerClassNames)) {
      try {
        Class<?> handlerClass = Class.forName(className, false, classLoader);
        SchemaAnnotationHandler handler = (SchemaAnnotationHandler) handlerClass.newInstance();
        handlers.add(handler);
        _logger.info("added handler {} for annotation namespace \"{}\"", className, handler.getAnnotationNamespace());
      } catch (Exception e) {
        _logger.error("Error instantiating handler class {} ", className, e);
        // fail even just one handler fails
        throw new IllegalStateException("Error instantiating handler class " + className);
      }
    }
    return handlers;
  }

  private static List<String> parsePaths(String pathAsStr)
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
