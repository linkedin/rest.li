package com.linkedin.restli.client;


import java.util.Map;

import com.linkedin.data.template.RecordTemplate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


/**
 * @author Rob Loh - rloh@linkedin.com
 */
public class BatchingKey<T extends RecordTemplate, R extends BatchRequest<?>>
{
  final private R _request;
  final private Map<String,Object> _queryParams;
  final private boolean _batchFields;

  /**
   * Creates the BatchingKey, to find other requests which can be batched together
   *
   * @param request the BatchGetRequest to create a key from
   * @param batchFields boolean, says whether or not we should be batching across the fields as well
   * @return Map which can be used to find other requests that can be batched (those that are .equals)
   */
  public BatchingKey(R request, boolean batchFields) {
    _request = request;
    _queryParams = BatchGetRequestUtil.getQueryParamsForBatchingKey(request);
    _batchFields = batchFields;
  }

  /**
   * This method provides granular exception messages for developers to know why a request
   * intended to be added to a batch, might fail to be batched.
   *
   * @param request the request to validate is the same or not
   * @throws IllegalArgumentException if there are any problems
   */
  public <T> void validate(BatchRequest<T> request)
  {
    if( !request.getBaseUriTemplate().equals(_request.getBaseUriTemplate()) ||
        !request.getPathKeys().equals(_request.getPathKeys()) ) {
      throw new IllegalArgumentException("Requests must have same base URI template and path keys to batch");
    }

    if( !request.getResourceProperties().equals(_request.getResourceProperties()) ) {
      throw new IllegalArgumentException("Requests must be for the same resource to batch");
    }

    if( !request.getRequestOptions().equals(_request.getRequestOptions()) ) {
      throw new IllegalArgumentException("Requests must have the same RestliRequestOptions to batch!");
    }

    // Enforce uniformity of query params excluding ids and fields
    final Map<String, Object> queryParams = BatchGetRequestUtil.getQueryParamsForBatchingKey(request);
    if ( !queryParams.equals(_queryParams) ) {
      throw new IllegalArgumentException("Requests must have same parameters to batch");
    }

    if (!_batchFields) {
      if( !request.getFields().equals(_request.getFields()) ) {
        throw new IllegalArgumentException("Requests must have same fields to batch");
      }
    }
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (o == null || !(o instanceof BatchingKey)) return false;

    @SuppressWarnings("unchecked")
    BatchingKey<T,R> that = (BatchingKey<T,R>) o;

    EqualsBuilder builder = new EqualsBuilder();
    builder.append(_request.getBaseUriTemplate(), that._request.getBaseUriTemplate());
    builder.append(_request.getPathKeys(), that._request.getPathKeys());
    builder.append(_request.getResourceProperties(), that._request.getResourceProperties());
    builder.append(_request.getRequestOptions(), that._request.getRequestOptions());
    builder.append(_queryParams, that._queryParams);
    builder.append(_batchFields, that._batchFields );
    if (_batchFields)
     builder.append(_request.getFields(), that._request.getFields());

    return builder.isEquals();
  }

  @Override
  public int hashCode()
  {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append( _request.getBaseUriTemplate() )
      .append( _request.getPathKeys() )
      .append( _request.getResourceProperties() )
      .append( _request.getRequestOptions() )
      .append( _queryParams );
    if(_batchFields && null != _request.getFields())
      builder.append( _request.getFields() );
    return builder.toHashCode();
  }
}
