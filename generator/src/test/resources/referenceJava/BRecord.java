import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;


/**
 * 
 * 
 */
@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Data Template. Generated from /Users/bpin/code/pegasus/pegasus/generator/src/test/resources/generator/BRecord.pdl.")
public class BRecord
    extends RecordTemplate
{

    private final static BRecord.Fields _fields = new BRecord.Fields();
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema("record BRecord{bField:record InlineRecord{}}", SchemaFormatType.PDL));
    private final static RecordDataSchema.Field FIELD_BField = SCHEMA.getField("bField");

    public BRecord() {
        super(new DataMap(2, 0.75F), SCHEMA, 2);
    }

    public BRecord(DataMap data) {
        super(data, SCHEMA);
    }

    public static BRecord.Fields fields() {
        return _fields;
    }

    /**
     * Existence checker for bField
     * 
     * @see BRecord.Fields#bField
     */
    public boolean hasBField() {
        return contains(FIELD_BField);
    }

    /**
     * Remover for bField
     * 
     * @see BRecord.Fields#bField
     */
    public void removeBField() {
        remove(FIELD_BField);
    }

    /**
     * Getter for bField
     * 
     * @see BRecord.Fields#bField
     */
    public InlineRecord getBField(GetMode mode) {
        return obtainWrapped(FIELD_BField, InlineRecord.class, mode);
    }

    /**
     * Getter for bField
     * 
     * @return
     *     Required field. Could be null for partial record.
     * @see BRecord.Fields#bField
     */
    @Nonnull
    public InlineRecord getBField() {
        return obtainWrapped(FIELD_BField, InlineRecord.class, GetMode.STRICT);
    }

    /**
     * Setter for bField
     * 
     * @see BRecord.Fields#bField
     */
    public BRecord setBField(InlineRecord value, SetMode mode) {
        putWrapped(FIELD_BField, InlineRecord.class, value, mode);
        return this;
    }

    /**
     * Setter for bField
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see BRecord.Fields#bField
     */
    public BRecord setBField(
        @Nonnull
        InlineRecord value) {
        putWrapped(FIELD_BField, InlineRecord.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    @Override
    public BRecord clone()
        throws CloneNotSupportedException
    {
        return ((BRecord) super.clone());
    }

    @Override
    public BRecord copy()
        throws CloneNotSupportedException
    {
        return ((BRecord) super.copy());
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

        public InlineRecord.Fields bField() {
            return new InlineRecord.Fields(getPathComponents(), "bField");
        }

    }

}
