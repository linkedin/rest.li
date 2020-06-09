import java.util.List;
import javax.annotation.Generated;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;


/**
 * 
 * 
 */
@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Data Template. Generated from /Users/bpin/code/pegasus/pegasus/generator/src/test/resources/generator/BRecord.pdl.")
public class InlineRecord
    extends RecordTemplate
{

    private final static InlineRecord.Fields _fields = new InlineRecord.Fields();
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema("record InlineRecord{}", SchemaFormatType.PDL));

    public InlineRecord() {
        super(new DataMap(0, 0.75F), SCHEMA);
    }

    public InlineRecord(DataMap data) {
        super(data, SCHEMA);
    }

    public static InlineRecord.Fields fields() {
        return _fields;
    }

    @Override
    public InlineRecord clone()
        throws CloneNotSupportedException
    {
        return ((InlineRecord) super.clone());
    }

    @Override
    public InlineRecord copy()
        throws CloneNotSupportedException
    {
        return ((InlineRecord) super.copy());
    }

    public static class Fields
        extends PathSpec
    {


        public Fields(List<String> path, String name) {
            super(path, name);
        }

        public Fields() {
            super();
        }

    }

}
