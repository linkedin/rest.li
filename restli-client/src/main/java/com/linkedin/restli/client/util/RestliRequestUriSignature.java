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

package com.linkedin.restli.client.util;


import com.linkedin.data.Data;
import com.linkedin.data.DataMap;
import com.linkedin.restli.client.DeleteRequest;
import com.linkedin.restli.client.FindRequest;
import com.linkedin.restli.client.GetAllRequest;
import com.linkedin.restli.client.GetRequest;
import com.linkedin.restli.client.PartialUpdateRequest;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.UpdateRequest;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.client.QueryParamsUtil;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.URIParamUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;


/**
 * <p>
 * A "summary" object for the URI of a {@link Request}.
 * </p>
 *
 * <p>
 * Rest.li does not expose request URI directly because it is part of wire protocol and
 * is subject to change from version to version. This class is meant to provide a robust way to
 * compare and textify request URI. For example, when creating a {@link HashMap} to match request and response pairs,
 * this class can be used as the key.
 * </p>
 *
 * <p>
 * A request URI consists of multiple fields, such as base URI template, query parameters, etc.
 * This class allows user to specify which fields to be captured in the signature.
 * Arbitrary combination of captured fields can be used for flexibility.
 * </p>
 *
 * <p>
 * The mask fields value is not used when computing hashCode(), equals() or toString().
 * </p>
 *
 * <p>
 * The signature does not expose the actual field values. If needed, please get them from the original {@link Request} object.
 * </p>
 *
 * @author Keren Jin
 */
public class RestliRequestUriSignature
{
  public static enum SignatureField
  {
    BASE_URI_TEMPLATE,
    PATH_KEYS,
    ID,
    QUERY_PARAMS
  }

  public static final Set<SignatureField> ALL_FIELDS =
    Collections.unmodifiableSet(new HashSet<>(Arrays.asList(SignatureField.values())));

  private final Set<SignatureField> _maskFields;
  private final String _baseUriTemplate;
  private final Map<String, Object> _pathKeys;
  private final Object _id;
  private final Map<String, Object> _queryParams;
  private final Map<String, Class<?>> _queryParamClasses;

  public RestliRequestUriSignature(Request<?> request, Set<SignatureField> maskFields)
  {
    if (maskFields.isEmpty())
    {
      throw new IllegalArgumentException("Signature fields must include at least one field.");
    }

    _maskFields = maskFields;

    if (maskFields.contains(SignatureField.BASE_URI_TEMPLATE))
    {
      _baseUriTemplate = request.getBaseUriTemplate();
    }
    else
    {
      _baseUriTemplate = null;
    }

    if (maskFields.contains(SignatureField.PATH_KEYS))
    {
      _pathKeys = request.getPathKeys();
    }
    else
    {
      _pathKeys = null;
    }

    if (!maskFields.contains(SignatureField.ID))
    {
      _id = null;
    }
    else if (request instanceof GetRequest)
    {
      _id = ((GetRequest) request).getObjectId();
    }
    else if (request instanceof UpdateRequest)
    {
      _id = ((UpdateRequest) request).getId();
    }
    else if (request instanceof PartialUpdateRequest)
    {
      _id = ((PartialUpdateRequest) request).getId();
    }
    else if (request instanceof DeleteRequest)
    {
      _id = ((DeleteRequest) request).getId();
    }
    else if (request instanceof FindRequest)
    {
      final CompoundKey assocKey = ((FindRequest) request).getAssocKey();
      _id = assocKey.getNumParts() == 0 ? null : assocKey;
    }
    else if (request instanceof GetAllRequest)
    {
      final CompoundKey assocKey = ((GetAllRequest) request).getAssocKey();
      _id = assocKey.getNumParts() == 0 ? null : assocKey;
    }
    else
    {
      _id = null;
    }

    if (maskFields.contains(SignatureField.QUERY_PARAMS))
    {
      // query param comparison is slightly different
      // if the first level values are collections, consider unordered and do set equality check
      // for deeper level values, do regular equality check

      final Map<String, Object> rawQueryParams = request.getQueryParamsObjects();
      if (rawQueryParams == null)
      {
        _queryParams = null;
        _queryParamClasses = null;
      }
      else
      {
        _queryParams = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawQueryParams.entrySet())
        {
          if (entry.getValue() instanceof Collection)
          {
            _queryParams.put(entry.getKey(), new HashSet<Object>((Collection<?>) entry.getValue()));
          }
          else
          {
            _queryParams.put(entry.getKey(), entry.getValue());
          }
        }
        _queryParamClasses = request.getQueryParamClasses();
      }
    }
    else
    {
      _queryParams = null;
      _queryParamClasses = null;
    }
  }

  public Set<SignatureField> getMaskFields()
  {
    return _maskFields;
  }

  @Override
  public int hashCode()
  {
    return new HashCodeBuilder()
      .append(_baseUriTemplate)
      .append(_pathKeys)
      .append(_id)
      .append(_queryParams)
      .toHashCode();
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == null) return false;
    if (obj == this) return true;
    if (getClass() != obj.getClass()) return false;

    final RestliRequestUriSignature other = (RestliRequestUriSignature) obj;
    return new EqualsBuilder()
      .append(_baseUriTemplate, other._baseUriTemplate)
      .append(_pathKeys, other._pathKeys)
      .append(_id, other._id)
      .append(_queryParams, other._queryParams)
      .isEquals();
  }

  @Override
  public String toString()
  {
    final ToStringBuilder builder = new ToStringBuilder(null, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("baseUriTemplate", _baseUriTemplate)
      .append("pathKeys", _pathKeys)
      .append("id", _id)
      .append("queryParams", _queryParams);

    return builder.toString();
  }

  /**
   * Return a {@link String} representation of the signature. The difference to {@link #toString()} is that
   * this method always returns stable result across different Map capacity and Java versions, ideal for storing and caching.
   * This is done by visiting the Map {@link Map}s by the order of its keys.
   *
   * This method is more costly tha toString(). If there is no requirement for stability, prefer to use toString().
   */
  public String dump()
  {
    final ProtocolVersion protocolVersion = AllProtocolVersions.LATEST_PROTOCOL_VERSION;
    final DataMap pathKeysMap = new DataMap(URIParamUtils.encodePathKeysForUri(_pathKeys, protocolVersion));
    final DataMap queryParamsMap = QueryParamsUtil.convertToDataMap(_queryParams, _queryParamClasses, protocolVersion);

    final ToStringBuilder builder = new ToStringBuilder(null, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("baseUriTemplate", _baseUriTemplate)
        .append("pathKeys", Data.dump("", pathKeysMap, ""))
        .append("id", _id)
        .append("queryParams", Data.dump("", queryParamsMap, ""));

    return builder.toString();
  }
}
