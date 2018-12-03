package com.linkedin.restli.internal.tools;


import com.linkedin.restli.internal.server.model.ResourceModelEncoder;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;


final public class AdditionalDocProvidersUtil
{
  private AdditionalDocProvidersUtil()
  {
  }

  public static List<ResourceModelEncoder.DocsProvider> findDocProviders(Logger log, boolean loadAdditionalDocProviders)
  {
    List<ResourceModelEncoder.DocsProvider> providers = new ArrayList<>();
    if (loadAdditionalDocProviders)
    {
      try
      {
        providers.add(
            (ResourceModelEncoder.DocsProvider) Class.forName("com.linkedin.sbtrestli.tools.scala.ScalaDocsProvider").newInstance());
      }
      catch (ClassNotFoundException | InstantiationException | IllegalAccessException ignored)
      {
        log.warn(
            "Attempted to load ScalaDocsProvider but it was not found. Please add 'com.linkedin.sbt-restli:restli-tools-scala_<scala-version>:<package-version>' to your classpath.\n"
            + "For more information, see: https://linkedin.github.io/rest.li/Scala-Integration#scaladoc");
      }
      catch (Throwable t)
      {
        log.info("Failed to initialize ScalaDocsProvider class", t);
      }
    }
    return providers;
  }
}
