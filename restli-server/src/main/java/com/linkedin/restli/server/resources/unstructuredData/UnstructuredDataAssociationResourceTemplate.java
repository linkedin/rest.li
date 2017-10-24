/*
 * Copyright (c) 2017 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.restli.server.resources.unstructuredData;


import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiTemplate;
import com.linkedin.restli.server.resources.ResourceContextHolder;


/**
 * Base {@link UnstructuredDataAssociationResource} implementation. All implementations should extend this.
 */
@RestLiTemplate(expectedAnnotation = RestLiAssociation.class)
public class UnstructuredDataAssociationResourceTemplate extends ResourceContextHolder implements UnstructuredDataAssociationResource
{
}