
package com.linkedin.restli.example.photos;

import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.base.DeleteRequestBuilderBase;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.example.Album_$Versioned;
import javax.annotation.Generated;


@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Request Builder")
public class AlbumsDeleteRequestBuilder
    extends DeleteRequestBuilderBase<Long, Album_$Versioned, AlbumsDeleteRequestBuilder>
{


    public AlbumsDeleteRequestBuilder(String baseUriTemplate, ResourceSpec resourceSpec, RestliRequestOptions requestOptions) {
        super(baseUriTemplate, Album_$Versioned.class, resourceSpec, requestOptions);
    }

    @Override
    protected Class<Album_$Versioned> getValueClass() {
        String versionHeader = getHeader(RestConstants.HEADER_RESTLI_SCHEMA_VERSION);
        int version = versionHeader == null ? DataSchemaConstants.CURRENT_VERSION : Integer.parseInt(versionHeader);
        return (Class<Album_$Versioned>) Album_$Versioned.getVersionedClass(version);
    }
}
