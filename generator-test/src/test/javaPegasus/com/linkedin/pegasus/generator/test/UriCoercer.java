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

package com.linkedin.pegasus.generator.test;

import java.net.URI;
import java.net.URISyntaxException;

import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class UriCoercer implements DirectCoercer<URI>
{
  private static final boolean REGISTER_COERCER = Custom.registerCoercer(new UriCoercer(), URI.class);

  private UriCoercer()
  {
  }

  @Override
  public Object coerceInput(URI object) throws ClassCastException
  {
    return object.toString();
  }

  @Override
  public URI coerceOutput(Object object) throws TemplateOutputCastException
  {
    try
    {
      return new URI((String)object);
    }
    catch (URISyntaxException e)
    {
      throw new TemplateOutputCastException("Invalid URI format", e);
    }
  }
}
