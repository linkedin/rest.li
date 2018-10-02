package com.linkedin.restli.internal.client;

import com.linkedin.data.DataMap;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.UpdateStatus;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for various response decoder.
 *
 * @author mnchen
 */
public class ResponseDecoderUtil
{
  /**
   * Helper method to assist BATCH_UPDATE, BATCH_PARTIAL_UPDATE, and BATCH_DELETE response decoder
   * {@link BatchUpdateResponseDecoder} in transforming the
   * raw payload data map received over-the-wire to a data map suitable for instantiation of a
   * {@link BatchKVResponse<?, UpdateStatus>}
   * @param dataMap received in the response payload (split results and errors in the data map)
   * @return data map suitable for {@link BatchKVResponse} (merged results in the data map)
   */
  public static DataMap mergeUpdateStatusResponseData(DataMap dataMap)
  {
    if (dataMap == null)
    {
      return null;
    }
    final DataMap mergedResults = new DataMap();
    final DataMap inputResults = dataMap.containsKey(BatchResponse.RESULTS) ? dataMap.getDataMap(BatchResponse.RESULTS)
            : new DataMap();
    final DataMap inputErrors = dataMap.containsKey(BatchResponse.ERRORS) ? dataMap.getDataMap(BatchResponse.ERRORS)
            : new DataMap();

    final Set<String> mergedKeys = new HashSet<>(inputResults.keySet());
    mergedKeys.addAll(inputErrors.keySet());

    for (String key : mergedKeys)
    {
      // DataMap for UpdateStatus
      final DataMap updateData;

      // status field is mandatory
      if (inputResults.containsKey(key))
      {
        updateData = inputResults.getDataMap(key);
      }
      else
      {
        updateData = new DataMap();
      }

      // DataMap for ErrorResponse
      final DataMap errorData = (DataMap) inputErrors.get(key);
      if (errorData != null)
      {
        // The status from ErrorResponse overwrites the one in UpdateResponse. However, results and
        // errors are not expected to have overlapping key. See BatchUpdateResponseBuilder.
        updateData.put("status", errorData.get("status"));
        updateData.put("error", errorData);
      }

      mergedResults.put(key, updateData);
    }

    final DataMap responseData = new DataMap();
    responseData.put(BatchKVResponse.RESULTS, mergedResults);
    responseData.put(BatchKVResponse.ERRORS, inputErrors);

    return responseData;
  }
}
