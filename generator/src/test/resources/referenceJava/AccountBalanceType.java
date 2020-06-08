import javax.annotation.Generated;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.template.DataTemplateUtil;


/**
 * Types of the Account Balance.  Indicates the applicability of the account.
 * 
 */
@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Data Template. Generated from /Users/bpin/code/pegasus/pegasus/generator/src/test/resources/generator/AccountBalanceType.pdsc.")
public enum AccountBalanceType {


    /**
     * promotion account balance
     * 
     */
    PROMOTION,

    /**
     * cash account balance
     * 
     */
    CASH,

    /**
     * cs granted account balance
     * 
     */
    CS_GRANTED,

    /**
     * post paid jymbii account balance
     * 
     */
    POSTPAID_JYMBII,

    /**
     * prepaid jymbii account balance
     * 
     */
    PREPAID_JYMBII,

    /**
     * post pay job account balance
     * 
     */
    JOBS_POSTPAY,

    /**
     * post pay job account balance for free trial overages
     * 
     */
    JOBS_POSTPAY_FREE_TRIAL_OVERAGES,

    /**
     * post pay job account balance created by volume discount
     * 
     */
    JOBS_POSTPAY_VOLUME_DISCOUNT,
    $UNKNOWN;
    private final static EnumDataSchema SCHEMA = ((EnumDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"enum\",\"name\":\"AccountBalanceType\",\"doc\":\"Types of the Account Balance.  Indicates the applicability of the account.\",\"symbols\":[\"PROMOTION\",\"CASH\",\"CS_GRANTED\",\"POSTPAID_JYMBII\",\"PREPAID_JYMBII\",\"JOBS_POSTPAY\",\"JOBS_POSTPAY_FREE_TRIAL_OVERAGES\",\"JOBS_POSTPAY_VOLUME_DISCOUNT\"],\"symbolDocs\":{\"CASH\":\"cash account balance\",\"CS_GRANTED\":\"cs granted account balance\",\"JOBS_POSTPAY\":\"post pay job account balance\",\"JOBS_POSTPAY_FREE_TRIAL_OVERAGES\":\"post pay job account balance for free trial overages\",\"JOBS_POSTPAY_VOLUME_DISCOUNT\":\"post pay job account balance created by volume discount\",\"POSTPAID_JYMBII\":\"post paid jymbii account balance\",\"PREPAID_JYMBII\":\"prepaid jymbii account balance\",\"PROMOTION\":\"promotion account balance\"}}"));

}
