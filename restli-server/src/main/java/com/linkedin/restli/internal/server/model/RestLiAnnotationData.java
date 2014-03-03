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

import com.linkedin.data.template.TyperefInfo;
import com.linkedin.restli.server.annotations.Key;
import com.linkedin.restli.server.annotations.RestAnnotations;
import com.linkedin.restli.server.annotations.RestLiAssociation;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestLiCollectionCompoundKey;
import com.linkedin.restli.server.annotations.RestLiSimpleResource;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class RestLiAnnotationData
{
  private final Class<?> _parent;
  private final String   _name;
  private final String   _namespace;
  private final Key[]    _keys;
  private final String   _keyName;
  private final Class<? extends TyperefInfo> _typerefInfoClass;

  /**
   * @param collectionAnno {@link RestLiCollection} annotation
   */
  public RestLiAnnotationData(RestLiCollection collectionAnno)
  {
    _parent = collectionAnno.parent();
    _name = collectionAnno.name();
    _namespace = collectionAnno.namespace();
    _keys = null;
    _keyName = RestAnnotations.DEFAULT.equals(collectionAnno.keyName()) ? null : collectionAnno.keyName();
    _typerefInfoClass = RestAnnotations.NULL_TYPEREF_INFO.class.equals(collectionAnno.keyTyperefClass()) ? null : collectionAnno.keyTyperefClass();
  }

  /**
   * @param collectionAnno {@link RestLiCollectionCompoundKey} annotation
   */
  public RestLiAnnotationData(RestLiCollectionCompoundKey collectionAnno)
  {
    _parent = collectionAnno.parent();
    _name = collectionAnno.name();
    _namespace = collectionAnno.namespace();
    _keys = collectionAnno.keys();
    _keyName = null;
    _typerefInfoClass = null;
  }

  /**
   * @param associationAnno {@link RestLiAssociation} annotation
   */
  public RestLiAnnotationData(RestLiAssociation associationAnno)
  {
    _parent = associationAnno.parent();
    _name = associationAnno.name();
    _namespace = associationAnno.namespace();
    _keys = associationAnno.assocKeys();
    _keyName = RestAnnotations.DEFAULT.equals(associationAnno.keyName()) ? null : associationAnno.keyName();
    _typerefInfoClass = null;
  }

  /**
   * @param simpleResourceAnno {@link com.linkedin.restli.server.annotations.RestLiSimpleResource} annotation
   */
  public RestLiAnnotationData(RestLiSimpleResource simpleResourceAnno)
  {
    _parent = simpleResourceAnno.parent();
    _name = simpleResourceAnno.name();
    _namespace = simpleResourceAnno.namespace();
    _keys = null;
    _keyName = null;
    _typerefInfoClass = null;
  }

  /**
   * @return parent resource class
   */
  public Class<?> parent()
  {
    return _parent;
  }

  /**
   * @return resource name
   */
  public String name()
  {
    return _name;
  }

  /**
   * @return namespace
   */
  public String namespace()
  {
    return _namespace;
  }

  /**
   * @return array of the resource {@link Key}s
   */
  public Key[] keys()
  {
    return _keys;
  }

  /**
   * @return "simple" resource key name
   */
  public String keyName()
  {
    return _keyName;
  }

  public Class<? extends TyperefInfo> typerefInfoClass()
  {
    return _typerefInfoClass;
  }

}
