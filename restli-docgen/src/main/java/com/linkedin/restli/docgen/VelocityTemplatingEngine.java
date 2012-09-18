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

package com.linkedin.restli.docgen;

import com.linkedin.restli.internal.server.RestLiInternalException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.Log4JLogChute;
import org.apache.velocity.runtime.resource.loader.JarResourceLoader;

/**
 * Templating engine implemented with Apache Velocity.
 *
 * @author Keren Jin
 */
public class VelocityTemplatingEngine implements TemplatingEngine
{
  /**
   * Initialize Velocity engine. Support the template files in either jar file or file system directory.
   */
  public VelocityTemplatingEngine()
  {
    final URL templateDirUrl = getClass().getClassLoader().getResource(VELOCITY_TEMPLATE_DIR);
    if (templateDirUrl == null)
    {
      throw new RestLiInternalException("Unable to find the Velocity template resources");
    }

    StringBuilder configName;
    if ("jar".equals(templateDirUrl.getProtocol()))
    {
      _velocity = new VelocityEngine();

      // config Velocity to use the jar resource loader
      // more detail in Velocity user manual

      _velocity.setProperty(VelocityEngine.RESOURCE_LOADER, "jar");

      configName = new StringBuilder("jar.").append(VelocityEngine.RESOURCE_LOADER).append(".class");
      _velocity.setProperty(configName.toString(), JarResourceLoader.class.getName());

      configName = new StringBuilder("jar.").append(VelocityEngine.RESOURCE_LOADER).append(".path");

      // fix for Velocity 1.5: jar URL needs to be ended with "!/"
      final String normalizedUrl = templateDirUrl.toString().substring(0, templateDirUrl.toString().length() - VELOCITY_TEMPLATE_DIR.length());
      _velocity.setProperty(configName.toString(), normalizedUrl);
    }
    else if ("file".equals(templateDirUrl.getProtocol()))
    {
      _velocity = new VelocityEngine();

      final String resourceDirPath = new File(templateDirUrl.getPath()).getParent();
      configName = new StringBuilder("file.").append(VelocityEngine.RESOURCE_LOADER).append(".path");
      _velocity.setProperty(configName.toString(), resourceDirPath);
    }
    else
    {
      throw new IllegalArgumentException("Unsupported template path scheme");
    }

    _velocity.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, Log4JLogChute.class.getName());
    _velocity.setProperty(Log4JLogChute.RUNTIME_LOG_LOG4J_LOGGER, getClass().getName());

    try
    {
      _velocity.init();
    }
    catch (Exception e)
    {
      throw new RestLiInternalException(e);
    }
  }

  @Override
  public void render(String templateName, Map<String, Object> pageModel, OutputStream out)
  {
    if (_velocity == null)
    {
      return;
    }

    final String actualTemplateName = VELOCITY_TEMPLATE_DIR + "/" + templateName;
    final VelocityContext context = new VelocityContext(pageModel);
    final Writer outWriter = new OutputStreamWriter(out);

    try
    {
      _velocity.mergeTemplate(actualTemplateName, VelocityEngine.ENCODING_DEFAULT, context, outWriter);
    }
    catch (Exception e)
    {
      throw new RestLiInternalException(e);
    }

    try
    {
      outWriter.flush();
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  private static final String VELOCITY_TEMPLATE_DIR = "vmTemplates";

  private final VelocityEngine _velocity;
}
