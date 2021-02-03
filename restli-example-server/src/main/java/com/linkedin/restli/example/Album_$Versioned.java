
package com.linkedin.restli.example;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import javax.annotation.Generated;


/**
 * An album for Rest.li
 *
 */
@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Data Template. Generated from restli-example-api/src/main/pegasus/com/linkedin/restli/example/Album.pdl.")
public abstract class Album_$Versioned
    extends RecordTemplate
{

    public Album_$Versioned(DataMap dataMap, RecordDataSchema schema) {
        super(dataMap, schema);
    }

    public static RecordDataSchema dataSchema() {
        return DataTemplateUtil.versionedSchema(Album.dataSchema(), Album_$2.dataSchema(), Album_$1.dataSchema());
    }

    public static Class<? extends Album_$Versioned> getVersionedClass(int version) {
        switch (version) {
            case 1:
                return Album_$1.class;
            case 2:
                return Album_$2.class;
            case DataSchemaConstants.CURRENT_VERSION:
            default:
                return Album.class;
        }
    }

    public abstract int getVersion();
}
