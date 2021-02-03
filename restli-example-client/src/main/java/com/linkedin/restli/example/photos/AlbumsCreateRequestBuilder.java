
package com.linkedin.restli.example.photos;

import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.restli.client.RestliRequestOptions;
import com.linkedin.restli.client.base.CreateIdRequestBuilderBase;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.validation.RestLiDataValidator;
import com.linkedin.restli.example.Album_$Versioned;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;


@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Request Builder")
public class AlbumsCreateRequestBuilder
    extends CreateIdRequestBuilderBase<Long, Album_$Versioned, AlbumsCreateRequestBuilder>
{


    public AlbumsCreateRequestBuilder(String baseUriTemplate, ResourceSpec resourceSpec, RestliRequestOptions requestOptions) {
        super(baseUriTemplate, Album_$Versioned.class, resourceSpec, requestOptions);
    }

    @Override
    public AlbumsCreateRequestBuilder input(Album_$Versioned entity) {
        setHeader(RestConstants.HEADER_RESTLI_SCHEMA_VERSION, Integer.toString(entity.getVersion()));
        return super.input(entity);
    }

    public static ValidationResult validateInput(Album_$Versioned input) {
        Map<String, List<String>> annotations = new HashMap<String, List<String>>();
        annotations.put("readOnly", Arrays.asList("id", "urn"));
        RestLiDataValidator validator = new RestLiDataValidator(annotations, Album_$Versioned.class, ResourceMethod.CREATE);
        return validator.validateInput(input);
    }

    @Override
    protected Class<Album_$Versioned> getValueClass() {
        return (Class<Album_$Versioned>) Album_$Versioned.getVersionedClass(getInput().getVersion());
    }
}
