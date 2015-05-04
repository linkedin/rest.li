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

package com.linkedin.restli.client.util;


import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.patch.request.PatchTree;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;


/**
 * Factory to generate cglib-backed proxies for generating {@link PatchTree}s.
 *
 * @author jflorencio
 */
final class GeneratePatchProxyFactory
{
  @SuppressWarnings("unchecked")
  static <T> T newInstance(Class<T> clazz, PatchTree patchTree, PathSpec pathSpec)
  {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(clazz);
    enhancer.setCallback(newMethodInterceptor(clazz, patchTree, pathSpec));
    return (T)enhancer.create();
  }

  private static MethodInterceptor newMethodInterceptor(Class<?> clazz, PatchTree patchTree, PathSpec pathSpec)
  {
    if (RecordTemplate.class.isAssignableFrom(clazz))
      return new GeneratePatchMethodInterceptor(clazz,
                                                (RecordDataSchema) DataTemplateUtil.getSchema(clazz),
                                                pathSpec,
                                                patchTree);

    throw new IllegalArgumentException("Attempted to proxy unsupported class " + clazz.getCanonicalName() +
                                           " with a PathSpec of " + pathSpec + "! Only RecordTemplate subclasses can be proxied!");
  }
}
