package com.linkedin.restli.client.uribuilders;

import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.restli.client.CreateIdEntityRequest;
import com.linkedin.restli.common.ProtocolVersion;

import java.net.URI;

/**
 * @author Boyang Chen
 */
public class CreateIdEntityRequestUriBuilder extends AbstractRestliRequestUriBuilder<CreateIdEntityRequest<?, ?>>
{
  CreateIdEntityRequestUriBuilder(CreateIdEntityRequest<?, ?> request, String uriPrefix, ProtocolVersion version)
  {
    super(request, uriPrefix, version);
  }

  @Override
  public URI build()
  {
    UriBuilder b = UriBuilder.fromUri(buildBaseUriWithPrefix());
    appendQueryParams(b);
    return b.build();
  }
}
