package com.linkedin.restli.client.uribuilders;

import com.linkedin.restli.client.CreateIdEntityRequest;
import com.linkedin.restli.common.ProtocolVersion;


/**
 * @author Boyang Chen
 */
public class CreateIdEntityRequestUriBuilder extends AbstractRestliRequestUriBuilder<CreateIdEntityRequest<?, ?>>
{
  CreateIdEntityRequestUriBuilder(CreateIdEntityRequest<?, ?> request, String uriPrefix, ProtocolVersion version)
  {
    super(request, uriPrefix, version);
  }
}
