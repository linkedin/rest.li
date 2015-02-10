/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.common.validation;


import com.linkedin.restli.restspec.RestSpecAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies paths of ReadOnly fields for a Rest.li resource.
 * A ReadOnly field cannot be set or changed by the client.
 * An example is server generated ids. The client should not supply the id when creating the entity.
 * <p>
 * ReadOnly fields should not be specified in a create or partial update request.
 * They should be specified in update requests, but they should have the same value as the original entity
 * (if an optional field was missing from the entity, it should be missing in the update request too).
 * <b>This is not checked by the Rest.li framework and should be checked in the resource implementation.</b>
 * <p>
 * See {@link RestLiDataValidator} for details on how to format paths.
 *
 * @author Soojung Ha
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@RestSpecAnnotation(name = "readOnly")
public @interface ReadOnly
{
  String[] value();
}