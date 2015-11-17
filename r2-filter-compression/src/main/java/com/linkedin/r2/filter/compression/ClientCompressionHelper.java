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

package com.linkedin.r2.filter.compression;

import com.linkedin.r2.filter.CompressionConfig;
import com.linkedin.r2.filter.CompressionOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class contains helper methods for Rest and Stream ClientCompressionFilter.
 */
public class ClientCompressionHelper
{
  private static final Logger LOG = LoggerFactory.getLogger(ClientCompressionHelper.class);


  /**
   * Turns on response compression for all operations
   */
  public static final String COMPRESS_ALL_RESPONSES_INDICATOR = "*";

  private static final String FAMILY_SEPARATOR = ":";
  private static final String COMPRESS_ALL_IN_FAMILY = FAMILY_SEPARATOR + COMPRESS_ALL_RESPONSES_INDICATOR;

  private final CompressionConfig _requestCompressionConfig;


  // For response compression, we keep the method-based configuration until all servers migrate to the
  // new size-based configuration.
  /**
   * The set of methods for which response compression will be turned on
   */
  private final Set<String> _responseCompressionMethods = new HashSet<String>();

  /**
   * The set of families for which response compression will be turned on.
   */
  private final Set<String> _responseCompressionFamilies = new HashSet<String>();

  private final boolean _compressAllResponses;

  public ClientCompressionHelper(CompressionConfig requestCompressionConfig,
      List<String> responseCompressionOperations)
  {
    _requestCompressionConfig = requestCompressionConfig;
    buildResponseCompressionMethodsAndFamiliesSet(responseCompressionOperations);
    _compressAllResponses = _responseCompressionMethods.contains(COMPRESS_ALL_RESPONSES_INDICATOR);
  }

  /**
   * Determines whether the request should be compressed, first checking {@link CompressionOption} in the request context,
   * then the threshold.
   *
   * @param entityLength request body length.
   * @param requestCompressionOverride compression force on/off override from the request context.
   * @return true if the outgoing request should be compressed.
   */
  public boolean shouldCompressRequest(int entityLength, CompressionOption requestCompressionOverride)
  {
    if (requestCompressionOverride != null)
    {
      return (requestCompressionOverride == CompressionOption.FORCE_ON);
    }
    return entityLength > _requestCompressionConfig.getCompressionThreshold();
  }

  /**
   * Returns true if the client might want a compressed response from the server.
   * @param operation
   */
  public boolean shouldCompressResponseForOperation(String operation)
  {
    return _compressAllResponses ||
        _responseCompressionMethods.contains(operation) ||
        isMemberOfCompressionFamily(operation);
  }

  /**
   * Checks if the operation is a member of a family for which we have turned response compression on.
   * @param operation
   */
  private boolean isMemberOfCompressionFamily(String operation)
  {
    if (operation.contains(FAMILY_SEPARATOR))
    {
      String[] parts = operation.split(FAMILY_SEPARATOR);
      if (parts == null || parts.length != 2)
      {
        return false;
      }
      String family = parts[0];
      return _responseCompressionFamilies.contains(family);
    }
    return false;
  }

  /**
   * Converts a comma separated list of operations into a set of Strings representing the operations for which we want
   * response compression to be turned on
   * @param responseCompressionOperations
   */
  private void buildResponseCompressionMethodsAndFamiliesSet(List<String> responseCompressionOperations)
  {
    for (String operation: responseCompressionOperations)
    {
      // family operations are represented in the config as "familyName:*"
      if (operation.endsWith(COMPRESS_ALL_IN_FAMILY))
      {
        String[] parts = operation.split(FAMILY_SEPARATOR);
        if (parts == null || parts.length != 2)
        {
          LOG.warn("Illegal compression operation family " + operation + " specified");
          return;
        }
        _responseCompressionFamilies.add(parts[0].trim());
      }
      else
      {
        _responseCompressionMethods.add(operation);
      }
    }
  }
}
