package com.linkedin.restli.internal.server.response;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.BatchFinderCriteriaResult;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.ResponseType;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.server.RestLiServiceException;
import java.util.List;

/**
 * Contains response data for {@link ResourceMethod#BATCH_FINDER}.
 *
 * @author Maxime Lamure
 */

public class BatchFinderResponseEnvelope extends RestLiResponseEnvelope
{
  private List<BatchFinderEntry> _items;

  /**
   * Constructs a <tt>BatchFinderResponseEnvelope</tt> with a status and list of {@link BatchFinderEntry}
   *
   * @param status the {@link HttpStatus}
   * @param items the items of the response
   */
  BatchFinderResponseEnvelope(HttpStatus status, List<BatchFinderEntry> items)
  {
    super(status);
    _items = items;
  }

  /**
   * Constructs a <tt>BatchFinderResponseEnvelope</tt> with an {@link RestLiServiceException}
   *
   * @param exception the {@link RestLiServiceException}
   */
  BatchFinderResponseEnvelope(RestLiServiceException exception)
  {
    super(exception);
  }

  @Override
  public ResourceMethod getResourceMethod()
  {
    return ResourceMethod.BATCH_FINDER;
  }

  /**
   * Sets the data stored by this envelope to null.
   */
  @Override
  protected void clearData()
  {
    _items = null;
  }

  /**
   * Returns the {@link ResponseType}.
   *
   * @return {@link ResponseType}.
   */
  @Override
  public final ResponseType getResponseType()
  {
    return ResponseType.BATCH_COLLECTION;
  }


  /**
   * Returns the list of collection responses for this request.
   *
   * @return the items of this collection response.
   */
  public List<BatchFinderEntry> getItems()
  {
    return _items;
  }


  /**
   * Set the list of collection responses for this request.
   *
   * @param items of this collection response.
   */
  public void setItems(List<BatchFinderEntry> items)
  {
    _items = items;
  }



  /**
   * Represents an item in the BatchFinder response list.
   */
  public static final class BatchFinderEntry {
    private List<? extends RecordTemplate> _elements;
    private CollectionMetadata _paging;
    private RecordTemplate _customMetadata;
    private RestLiServiceException _exception;

    /**
     * Constructs a <tt>BatchFinderEntry</tt>
     *
     * @param elements the elements list
     * @param paging the paging metadata
     * @param customMetadata the custom metadata, as defined by the application
     *
     */
    public BatchFinderEntry(List<? extends RecordTemplate> elements, CollectionMetadata paging, RecordTemplate customMetadata) {
      _elements = elements;
      _paging = paging;
      _customMetadata = customMetadata;
    }

    /**
     * Constructs a <tt>BatchFinderEntry</tt> that represents an error
     *
     * @param error the exception
     *
     */
    public BatchFinderEntry(RestLiServiceException error) {
      _exception = error;
    }


    /**
     * Returns the elements.
     *
     * @return the list of elements
     */
    public List<? extends RecordTemplate> getElements() {
      return _elements;
    }

    /**
     * Returns the custom metadata.
     *
     * @return the custom metadata
     */
    public RecordTemplate getCustomMetadata() {
      return _customMetadata;
    }

    /**
     * Returns the paging.
     *
     * @return the paging
     */
    public CollectionMetadata getPaging() {
      return _paging;
    }

    /**
     * Returns the exception.
     *
     * @return the exception
     */
    public RestLiServiceException getException() {
      return _exception;
    }

    /**
     * Set the elements
     *
     * @param elements The elements
     */
    public void setElements(List<? extends RecordTemplate> elements) {
      _elements = elements;
    }

    /**
     * Set the paging
     *
     * @param paging The paging metadata
     */
    public void setPaging(CollectionMetadata paging) {
      _paging = paging;
    }

    /**
     * Set the paging
     *
     * @param customMetadata The custom metadata
     */
    public void setCustomMetadata(RecordTemplate customMetadata) {
      _customMetadata = customMetadata;
    }

    /**
     * Set the exception
     *
     * @param exception The exception
     */
    public void setException(RestLiServiceException exception) {
      _exception = exception;
    }

    /**
     * Determines if the entry is a failure.
     *
     * @return true if the entry contains an exception, false otherwise.
     */
    public boolean isErrorResponse()
    {
      return _exception != null;
    }

    /**
     * <tt>toResponse</tt> build a DataMap from the current instance.
     * This DataMap contains only primitive types, DataMap and DataList to be serialized.
     *
     * @param errorResponseBuilder The builder to use to build the response for the error
     */
    public DataMap toResponse(ErrorResponseBuilder errorResponseBuilder) {
      BatchFinderCriteriaResult<AnyRecord> batchFinderCriteriaResult = new BatchFinderCriteriaResult<>(new DataMap(), AnyRecord.class);

      if (_exception != null) {
        // error case
        batchFinderCriteriaResult.setIsError(true);
        batchFinderCriteriaResult.setError(errorResponseBuilder.buildErrorResponse(this._exception));
      }
      else
      {
        // success case
        CollectionResponse<AnyRecord> item = new CollectionResponse<>(AnyRecord.class);
        DataList itemsMap = (DataList) item.data().get(CollectionResponse.ELEMENTS);
        for (int i = 0; i < _elements.size(); i++) {
          CheckedUtil.addWithoutChecking(itemsMap, _elements.get(i).data());
        }

        //elements
        batchFinderCriteriaResult.setElements(item);

        //metadata
        if (_customMetadata != null) {
          batchFinderCriteriaResult.setMetadataRaw(_customMetadata.data());
        }

        //paging
        batchFinderCriteriaResult.setPaging(_paging);
      }
      return batchFinderCriteriaResult.data();
    }
  }
}
