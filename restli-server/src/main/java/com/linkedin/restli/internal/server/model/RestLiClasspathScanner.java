/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.restli.internal.server.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.server.ResourceConfigException;
import com.linkedin.restli.server.annotations.RestLiActions;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;

/**
 * @author Josh Walker
 * @version $Revision: $
 *
 * Scans the resources available to the current classloader
 * to find the rest.li annotated classes in the specified
 * set of packages.
 *
 * Inspired by Jersey's package scanning logic.
 */
class RestLiClasspathScanner
{
  public static final String CLASS_SUFFIX = ".class";
  public static final char PACKAGE_SEPARATOR = '.';
  public static final char FILE_SEPARATOR = File.separatorChar;
  public static final char UNIX_FILE_SEPARATOR = '/';
  public static final String SCHEME_FILE = "file";
  public static final String SCHEME_JAR = "jar";
  public static final String SCHEME_ZIP = "zip";
  public static final char JAR_ENTRY_DELIMITER = '!';
  private static final Set<Class<? extends Annotation>> _annotations = buildAnnotations();

  private static Set<Class<? extends Annotation>> buildAnnotations()
  {
    Set<Class<? extends Annotation>> annotations = new HashSet<Class<? extends Annotation>>();
    annotations.add(RestLiCollection.class);
    annotations.add(RestLiAssociation.class);
    annotations.add(RestLiActions.class);
    annotations.add(RestLiSimpleResource.class);
    return Collections.unmodifiableSet(annotations);
  }

  private final Set<Class<?>> _matchedClasses;

  private final ClassLoader _classLoader;
  private final Set<String> _packagePaths;
  private final Set<String> _classNames;

  public RestLiClasspathScanner(final Set<String> packageNames, final Set<String> classNames, final ClassLoader classLoader)
  {
    _classLoader = classLoader;
    _packagePaths = new HashSet<String>();
    //convert package names to paths, to optimize matching against .class paths
    for (String packageName : packageNames)
    {
      _packagePaths.add(nameToPath(packageName));
    }
    _classNames = classNames;
    _matchedClasses = new HashSet<Class<?>>();
  }

  private String nameToPath(final String name)
  {
    return name.replace(PACKAGE_SEPARATOR, FILE_SEPARATOR);
  }

  private String pathToName(final String path)
  {
    return path.replace(FILE_SEPARATOR, PACKAGE_SEPARATOR);
  }

  private String toUnixPath(final String path)
  {
    return path.replace(FILE_SEPARATOR, UNIX_FILE_SEPARATOR);
  }

  private String toNativePath(final String path)
  {
    return path.replace(UNIX_FILE_SEPARATOR, FILE_SEPARATOR);
  }

  public Class<?> classForName(final String name)
          throws ClassNotFoundException
  {
    return Class.forName(name, false, _classLoader);
  }

  public Set<Class<?>> getMatchedClasses()
  {
    return _matchedClasses;
  }

  public void scanPackages()
  {
    try
    {
      for (String p : _packagePaths)
      {
        Enumeration<URL> resources = _classLoader.getResources(toUnixPath(p));
        while (resources.hasMoreElements())
        {
          URI u = resources.nextElement().toURI();
          String scheme = u.getScheme().toLowerCase();
          if (scheme.equals(SCHEME_JAR) || scheme.equals(SCHEME_ZIP))
          {
            scanJar(u);
          }
          else if (scheme.equals(SCHEME_FILE))
          {
            scanDirectory(new File(u.getPath()));
          }
          else
          {
            throw new ResourceConfigException("Unable to scan resource '" + u.toString()
                + "'. URI scheme not supported by scanner.");
          }
        }
      }
    }
    catch (IOException e)
    {
      throw new ResourceConfigException("Unable to scan resources", e);
    }
    catch (URISyntaxException e)
    {
      throw new ResourceConfigException("Unable to scan resources", e);
    }
  }

  public String scanClasses()
  {
    final StringBuilder errorBuilder = new StringBuilder();

    for (String c : _classNames)
    {
      try
      {
        final Class<?> candidateClass = classForName(c);
        for (Annotation a : candidateClass.getAnnotations())
        {
          if (_annotations.contains(a.annotationType()))
          {
            _matchedClasses.add(candidateClass);
            break;
          }
        }
      }
      catch (ClassNotFoundException e)
      {
        errorBuilder.append(String.format("Failed to load class %s\n", c));
      }
    }

    return errorBuilder.toString();
  }

  private void scanJar(final URI u) throws IOException
  {
    String ssp = u.getRawSchemeSpecificPart();
    URL jarUrl = new URL(ssp.substring(0, ssp.lastIndexOf(JAR_ENTRY_DELIMITER)));
    InputStream in = null;
    JarInputStream jarIn = null;
    try
    {
      in = jarUrl.openStream();
      jarIn = new JarInputStream(in);

      //remove "!/" to get a path like "com/linkedin/util"
      String parent = ssp.substring(ssp.lastIndexOf(JAR_ENTRY_DELIMITER) + 2);
      for (JarEntry e = jarIn.getNextJarEntry(); e != null; e = jarIn.getNextJarEntry())
      {
        if (!e.isDirectory() && e.getName().startsWith(parent))
        {
          checkForMatchingClass(toNativePath(e.getName()));
        }
        jarIn.closeEntry();
      }
    }
    finally
    {
      if (jarIn != null)
      {
        jarIn.close();
      }
      if (in != null)
      {
        in.close();
      }
    }
  }

  private void scanDirectory(final File root)
  {
    if (!root.isDirectory())
    {
      return;
    }

    for (File child : root.listFiles())
    {
      if (child.isDirectory())
      {
        scanDirectory(child);
      }
      else
      {
        checkForMatchingClass(child.getAbsolutePath());
      }
    }

  }

  public void checkForMatchingClass(final String name)
  {
    if (name.endsWith(CLASS_SUFFIX))
    {
      for (String packagePath : _packagePaths)
      {
        if (name.contains(packagePath))
        {
          int start = name.lastIndexOf(packagePath);
          int end = name.lastIndexOf(CLASS_SUFFIX);
          String clazzPath = name.substring(start, end);
          String clazzName = pathToName(clazzPath);

          try
          {
            Class<?> clazz = classForName(clazzName);
            for (Annotation a : clazz.getAnnotations())
            {
              if (_annotations.contains(a.annotationType()))
              {
                _matchedClasses.add(clazz);
                break;
              }
            }
          }
          catch (ClassNotFoundException e)
          {
            throw new RestLiInternalException("Failed to load class while scanning packages", e);
          }
        }
      }
    }
  }

}
