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

package com.linkedin.restli.internal.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.filter.request.MaskCreator;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.URIMaskUtil;
import com.linkedin.jersey.api.uri.UriComponent;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class URIUtil
{
  public static String encodeFields(PathSpec[] fieldPaths)
  {
    MaskTree mask = MaskCreator.createPositiveMask(fieldPaths);
    return URIMaskUtil.encodeMaskForURI(mask);
  }

  public static String encodeForPath(String raw)
  {
    return UriComponent.encode(raw, UriComponent.Type.PATH);
  }

  public static String encodeForQueryParam(String raw)
  {
    return UriComponent.encode(raw, UriComponent.Type.QUERY_PARAM);
  }
}
