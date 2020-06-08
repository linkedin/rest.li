import javax.annotation.Generated;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.template.DataTemplateUtil;


/**
 * Lifecycle states for a premium serivce.
 * 
 */
@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Data Template. Generated from /Users/bpin/code/pegasus/pegasus/generator/src/test/resources/generator/PremiumServiceState.pdsc.")
public enum PremiumServiceState {


    /**
     * Premium Service might be in use in future
     * 
     */
    FUTURE,

    /**
     * Premium Service is active.
     * 
     */
    ACTIVE,

    /**
     * Premium Service is created, but not paid yet.
     * 
     */
    CREATED,

    /**
     * Premium Service is expired due to user cancellation.
     * 
     */
    EXPIRED,

    /**
     * Premium Service is deleted.
     * 
     */
    DELETED,

    /**
     * Premium Service is suspended.
     * 
     */
    SUSPENDED,
    $UNKNOWN;
    private final static EnumDataSchema SCHEMA = ((EnumDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"enum\",\"name\":\"PremiumServiceState\",\"doc\":\"Lifecycle states for a premium serivce.\",\"symbols\":[\"FUTURE\",\"ACTIVE\",\"CREATED\",\"EXPIRED\",\"DELETED\",\"SUSPENDED\"],\"symbolDocs\":{\"ACTIVE\":\"Premium Service is active.\",\"CREATED\":\"Premium Service is created, but not paid yet.\",\"DELETED\":\"Premium Service is deleted.\",\"EXPIRED\":\"Premium Service is expired due to user cancellation.\",\"FUTURE\":\"Premium Service might be in use in future\",\"SUSPENDED\":\"Premium Service is suspended.\"}}"));

}
