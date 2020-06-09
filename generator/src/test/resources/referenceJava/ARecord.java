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
@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Data Template. Generated from /Users/bpin/code/pegasus/pegasus/generator/src/test/resources/generator/ARecord.pdl.")
public class ARecord
    extends RecordTemplate
{

    private final static ARecord.Fields _fields = new ARecord.Fields();
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema("record ARecord{aField:typeref AField=string}", SchemaFormatType.PDL));
    private final static RecordDataSchema.Field FIELD_AField = SCHEMA.getField("aField");

    public ARecord() {
        super(new DataMap(2, 0.75F), SCHEMA);
    }

    public ARecord(DataMap data) {
        super(data, SCHEMA);
    }

    public static ARecord.Fields fields() {
        return _fields;
    }

    /**
     * Existence checker for aField
     * 
     * @see ARecord.Fields#aField
     */
    public boolean hasAField() {
        return contains(FIELD_AField);
    }

    /**
     * Remover for aField
     * 
     * @see ARecord.Fields#aField
     */
    public void removeAField() {
        remove(FIELD_AField);
    }

    /**
     * Getter for aField
     * 
     * @see ARecord.Fields#aField
     */
    public String getAField(GetMode mode) {
        return obtainDirect(FIELD_AField, String.class, mode);
    }

    /**
     * Getter for aField
     * 
     * @return
     *     Required field. Could be null for partial record.
     * @see ARecord.Fields#aField
     */
    @Nonnull
    public String getAField() {
        return obtainDirect(FIELD_AField, String.class, GetMode.STRICT);
    }

    /**
     * Setter for aField
     * 
     * @see ARecord.Fields#aField
     */
    public ARecord setAField(String value, SetMode mode) {
        putDirect(FIELD_AField, String.class, String.class, value, mode);
        return this;
    }

    /**
     * Setter for aField
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see ARecord.Fields#aField
     */
    public ARecord setAField(
        @Nonnull
        String value) {
        putDirect(FIELD_AField, String.class, String.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    @Override
    public ARecord clone()
        throws CloneNotSupportedException
    {
        return ((ARecord) super.clone());
    }

    @Override
    public ARecord copy()
        throws CloneNotSupportedException
    {
        return ((ARecord) super.copy());
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

        public PathSpec aField() {
            return new PathSpec(getPathComponents(), "aField");
        }

    }

}
