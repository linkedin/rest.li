/*
   Copyright (c) 2014 LinkedIn Corp.

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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Delegates doc requests to language specific {@link DocsProvider}s.  The documentation
 * from the first language specific provider that returns a doc string is used.
 *
 * For example,  one provider may be for javadoc and another for scaladoc.  Providers should
 * only return documentation for resources files written in the language they are a provider for.
 */
public class MultiLanguageDocsProvider implements DocsProvider
{
  private static final Logger log = LoggerFactory.getLogger(MultiLanguageDocsProvider.class);

  public static List<DocsProvider> loadExternalProviders(List<DocsProvider> docsProviders)
  {
    List<DocsProvider> providers = new ArrayList<DocsProvider>();
    for(Object provider : docsProviders)
    {
      log.info("Executing "+ provider.getClass().getSimpleName() + " tool...");
      try
      {
        if(provider instanceof DocsProvider)
        {
          DocsProvider docsProvider = (DocsProvider)provider;
          providers.add(docsProvider);
        }
        else
        {
          log.error("Unable to cast provided docs provider to DocsProvider class: " + provider + ", skipping.");
        }
      }
      catch (Exception e)
      {
        log.error("Unable to registerSourceFiles documentation provider for class: " + provider + ", skipping.", e);
      }
    }
    return providers;
  }

  private final List<DocsProvider> _languageSpecificProviders;

  /**
   * Constructor.
   *
   * @param languageSpecificProviders provides an ordered list of language specific providers.
   *                                  The providers should only provide documentation strings for the
   *                                  resources written in the language they provide.  In the case where
   *                                  multiple providers are able to return documentation for a
   *                                  class, the documentation from the first one in the list is used.
   */
  public MultiLanguageDocsProvider(List<DocsProvider> languageSpecificProviders)
  {
    _languageSpecificProviders = languageSpecificProviders;
  }

  @Override
  public void registerSourceFiles(Collection<String> filenames)
  {
    for(DocsProvider provider : _languageSpecificProviders)
    {
      provider.registerSourceFiles(filterForFileExtensions(filenames, provider.supportedFileExtensions()));
    }
  }

  private static Collection<String> filterForFileExtensions(Collection<String> filenames, Collection<String> extensions)
  {
    List<String> filenamesMatchingExtension = new ArrayList<String>();

    for(String extension : extensions) // usually just one
    {
      if(filenames != null)
      {
        for(String sourceFile : filenames)
        {
          if(sourceFile.endsWith(extension))
          {
            filenamesMatchingExtension.add(sourceFile);
          }
        }
      }
    }
    return filenamesMatchingExtension;
  }

  @Override
  public Set<String> supportedFileExtensions()
  {
    Set<String> supportedFileExtensions = new HashSet<String>();
    for(DocsProvider provider : _languageSpecificProviders)
    {
      supportedFileExtensions.addAll(provider.supportedFileExtensions());
    }
    return Collections.unmodifiableSet(supportedFileExtensions);
  }

  @Override
  public String getClassDoc(Class<?> resourceClass)
  {
    for(DocsProvider provider: _languageSpecificProviders)
    {
      String doc = provider.getClassDoc(resourceClass);
      if(doc != null) return doc;
    }
    return null;
  }

  @Override
  public String getClassDeprecatedTag(Class<?> resourceClass)
  {
    for(DocsProvider provider: _languageSpecificProviders)
    {
      String tag = provider.getClassDeprecatedTag(resourceClass);
      if(tag != null) return tag;
    }
    return null;
  }

  @Override
  public String getMethodDoc(Method method)
  {
    for(DocsProvider provider: _languageSpecificProviders)
    {
      String doc = provider.getMethodDoc(method);
      if(doc != null) return doc;
    }
    return null;
  }

  @Override
  public String getMethodDeprecatedTag(Method method)
  {
    for(DocsProvider provider: _languageSpecificProviders)
    {
      String tag = provider.getMethodDeprecatedTag(method);
      if(tag != null) return tag;
    }
    return null;
  }

  @Override
  public String getParamDoc(Method method, String name)
  {
    for(DocsProvider provider: _languageSpecificProviders)
    {
      String doc = provider.getParamDoc(method, name);
      if(doc != null) return doc;
    }
    return null;
  }
}
