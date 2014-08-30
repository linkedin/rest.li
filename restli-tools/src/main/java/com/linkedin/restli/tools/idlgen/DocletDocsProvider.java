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

package com.linkedin.restli.tools.idlgen;


import com.linkedin.restli.internal.server.model.ResourceModelEncoder.DocsProvider;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Tag;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.output.NullWriter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Specialized {@link DocsProvider} whose documentation comes from the Javadoc Doclet {@link RestLiDoclet}.
 *
 * @author dellamag
 */
public class DocletDocsProvider implements DocsProvider
{
  private static final Logger log = LoggerFactory.getLogger(DocletDocsProvider.class);

  private final String _apiName;
  private final String[] _classpath;
  private final String[] _sourcePaths;
  private final String[] _resourcePackages;

  private Long _docletId = null;

  public DocletDocsProvider(String apiName,
                            String[] classpath,
                            String[] sourcePaths,
                            String[] resourcePackages)
  {
    _apiName = apiName;
    _classpath = classpath;
    _sourcePaths = sourcePaths;
    _resourcePackages = resourcePackages;
  }

  @Override
  public Set<String> supportedFileExtensions()
  {
    return Collections.singleton(".java");
  }

  @Override
  public void registerSourceFiles(Collection<String> sourceFileNames)
  {
    log.info("Executing Javadoc tool...");

    final String flatClasspath;
    if (_classpath == null)
    {
      flatClasspath = System.getProperty("java.class.path");
    }
    else
    {
      flatClasspath = StringUtils.join(_classpath, ":");
    }

    final PrintWriter sysoutWriter = new PrintWriter(System.out, true);
    final PrintWriter nullWriter = new PrintWriter(new NullWriter());
    final List<String> javadocArgs = new ArrayList<String>(Arrays.asList("-classpath",
                                                                         flatClasspath,
                                                                         "-sourcepath",
                                                                         StringUtils.join(_sourcePaths, ":")));
    if (_resourcePackages != null)
    {
      javadocArgs.add("-subpackages");
      javadocArgs.add(StringUtils.join(_resourcePackages, ":"));
    }
    else
    {
      javadocArgs.addAll(sourceFileNames);
    }

    _docletId = RestLiDoclet.generateJavadoc(_apiName, sysoutWriter, nullWriter, nullWriter, javadocArgs.toArray(new String[0]));
  }

  @Override
  public String getClassDoc(Class<?> resourceClass)
  {
    final ClassDoc doc = RestLiDoclet.getClassDoc(_docletId, resourceClass);
    if (doc == null)
    {
      return null;
    }

    return buildDoc(doc.commentText());
  }

  @Override
  public String getClassDeprecatedTag(Class<?> resourceClass)
  {
    final ClassDoc doc = RestLiDoclet.getClassDoc(_docletId, resourceClass);
    if (doc == null)
    {
      return null;
    }

    return formatDeprecatedTags(doc);
  }

  private String formatDeprecatedTags(Doc doc)
  {
    Tag[] deprecatedTags = doc.tags("deprecated");
    if(deprecatedTags.length > 0)
    {
      StringBuilder deprecatedText = new StringBuilder();
      for(int i = 0; i < deprecatedTags.length; i++)
      {
        deprecatedText.append(deprecatedTags[i].text());
        if(i < deprecatedTags.length - 1)
        {
          deprecatedText.append(" ");
        }
      }
      return deprecatedText.toString();
    }
    else
    {
      return null;
    }
  }

  @Override
  public String getMethodDoc(Method method)
  {
    final MethodDoc doc = RestLiDoclet.getMethodDoc(_docletId, method);
    if (doc == null)
    {
      return null;
    }

    return buildDoc(doc.commentText());
  }

  @Override
  public String getMethodDeprecatedTag(Method method)
  {
    final MethodDoc doc = RestLiDoclet.getMethodDoc(_docletId, method);
    if (doc == null)
    {
      return null;
    }

    return formatDeprecatedTags(doc);
  }

  @Override
  public String getParamDoc(Method method, String name)
  {
    final MethodDoc methodDoc = RestLiDoclet.getMethodDoc(_docletId, method);
    if (methodDoc != null)
    {
      for (ParamTag tag: methodDoc.paramTags())
      {
        if (name.equals(tag.parameterName()))
        {
          return buildDoc(tag.parameterComment());
        }
      }
    }

    return null;
  }

  @Override
  public String getReturnDoc(Method method)
  {
    final MethodDoc methodDoc = RestLiDoclet.getMethodDoc(_docletId, method);
    if (methodDoc != null)
    {
      for (Tag tag : methodDoc.tags())
      {
        if(tag.name().toLowerCase().equals("@return"))
        {
          return buildDoc(tag.text());
        }
      }
    }

    return null;
  }

  private String buildDoc(String docText)
  {
    if (docText != null && !docText.isEmpty())
    {
      return docText;
    }

    return null;
  }
}
