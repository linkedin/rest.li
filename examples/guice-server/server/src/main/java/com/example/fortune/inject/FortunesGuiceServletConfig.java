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
package com.example.fortune.inject;


import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.filter.FilterChains;
import com.linkedin.r2.filter.compression.EncodingType;
import com.linkedin.r2.filter.compression.ServerCompressionFilter;
import com.linkedin.r2.filter.logging.SimpleLoggingFilter;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.guice.GuiceRestliServlet;


/**
 *
 */
public class FortunesGuiceServletConfig extends GuiceServletContextListener
{
  private static final int THRESHOLD = 4096;

  @Override
  protected Injector getInjector()
  {
    return Guice.createInjector(
        new AbstractModule()
        {
          @Override
          protected void configure()
          {
            RestLiConfig restLiConfig = new RestLiConfig();
            restLiConfig.setResourcePackageNames("com.example.fortune");
            bind(RestLiConfig.class).toInstance(restLiConfig);

            FilterChain filterChain = FilterChains.createRestChain(
                new ServerCompressionFilter(new EncodingType[] { EncodingType.SNAPPY }, new CompressionConfig(THRESHOLD)),
                new SimpleLoggingFilter());
            bind(FilterChain.class).toInstance(filterChain);
          }
        },
        new ServletModule()
        {
          @Override
          protected void configureServlets()
          {
            serve("/*").with(GuiceRestliServlet.class);
          }
        });
    }

}
