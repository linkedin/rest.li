import javax.annotation.Generated;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.TyperefInfo;


/**
 * An ISO duration. See org.joda.time.Period.
 * 
 */
@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Data Template. Generated from /Users/bpin/code/pegasus/pegasus/generator/src/test/resources/generator/IsoDuration.pdsc.")
public class IsoDuration
    extends TyperefInfo
{

    private final static TyperefDataSchema SCHEMA = ((TyperefDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"typeref\",\"name\":\"IsoDuration\",\"doc\":\"An ISO duration. See org.joda.time.Period.\",\"ref\":\"string\"}"));

    public IsoDuration() {
        super(SCHEMA);
    }

}
