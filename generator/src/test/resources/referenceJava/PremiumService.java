import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;


/**
 * A premium service.
 * 
 */
@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Data Template. Generated from /Users/bpin/code/pegasus/pegasus/generator/src/test/resources/generator/PremiumService.pdsc.")
public class PremiumService
    extends RecordTemplate
{

    private final static PremiumService.Fields _fields = new PremiumService.Fields();
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"record\",\"name\":\"PremiumService\",\"doc\":\"A premium service.\",\"fields\":[{\"name\":\"id\",\"type\":\"int\",\"doc\":\"The primary key id for a premium service.\",\"optional\":true},{\"name\":\"trialPeriod\",\"type\":{\"type\":\"typeref\",\"name\":\"IsoDuration\",\"doc\":\"An ISO duration. See org.joda.time.Period.\",\"ref\":\"string\"},\"doc\":\"Trial period for the recurrence. See org.joda.time.Period\",\"optional\":true},{\"name\":\"state\",\"type\":{\"type\":\"enum\",\"name\":\"PremiumServiceState\",\"doc\":\"Lifecycle states for a premium serivce.\",\"symbols\":[\"FUTURE\",\"ACTIVE\",\"CREATED\",\"EXPIRED\",\"DELETED\",\"SUSPENDED\"],\"symbolDocs\":{\"ACTIVE\":\"Premium Service is active.\",\"CREATED\":\"Premium Service is created, but not paid yet.\",\"DELETED\":\"Premium Service is deleted.\",\"EXPIRED\":\"Premium Service is expired due to user cancellation.\",\"FUTURE\":\"Premium Service might be in use in future\",\"SUSPENDED\":\"Premium Service is suspended.\"}},\"doc\":\"Lifecycle states for a premium serivce.\",\"optional\":true},{\"name\":\"currencyCode\",\"type\":\"string\",\"doc\":\"Can be ISO currency code for the money OR A Virtual Currency which is prefixed with V$. If a member purchases a 5 Job Pack he will get 5 Job Credits. Each Credit can be used to Post 1 Job.\",\"optional\":true},{\"name\":\"amount\",\"type\":\"string\",\"doc\":\"The amount of money.\",\"optional\":true},{\"name\":\"interval\",\"type\":\"string\",\"doc\":\"Duration for the recurrence in ISO. See org.joda.time.Period\",\"optional\":true},{\"name\":\"productCode\",\"type\":\"string\",\"doc\":\"code for the product.\"},{\"name\":\"accountBalance\",\"type\":\"string\",\"doc\":\"The account balance.\",\"optional\":true},{\"name\":\"pendingCancellation\",\"type\":\"boolean\",\"doc\":\"Whether or not there is a pending cancellation scheduled for this service.\",\"optional\":true},{\"name\":\"freeTrial\",\"type\":\"boolean\",\"doc\":\"Whether or not user is on free trial.\",\"optional\":true},{\"name\":\"accountBalanceType\",\"type\":{\"type\":\"enum\",\"name\":\"AccountBalanceType\",\"doc\":\"Types of the Account Balance.  Indicates the applicability of the account.\",\"symbols\":[\"PROMOTION\",\"CASH\",\"CS_GRANTED\",\"POSTPAID_JYMBII\",\"PREPAID_JYMBII\",\"JOBS_POSTPAY\",\"JOBS_POSTPAY_FREE_TRIAL_OVERAGES\",\"JOBS_POSTPAY_VOLUME_DISCOUNT\"],\"symbolDocs\":{\"CASH\":\"cash account balance\",\"CS_GRANTED\":\"cs granted account balance\",\"JOBS_POSTPAY\":\"post pay job account balance\",\"JOBS_POSTPAY_FREE_TRIAL_OVERAGES\":\"post pay job account balance for free trial overages\",\"JOBS_POSTPAY_VOLUME_DISCOUNT\":\"post pay job account balance created by volume discount\",\"POSTPAID_JYMBII\":\"post paid jymbii account balance\",\"PREPAID_JYMBII\":\"prepaid jymbii account balance\",\"PROMOTION\":\"promotion account balance\"}},\"doc\":\"The account balance type\",\"optional\":true}]}"));
    private final static RecordDataSchema.Field FIELD_Id = SCHEMA.getField("id");
    private final static RecordDataSchema.Field FIELD_TrialPeriod = SCHEMA.getField("trialPeriod");
    private final static RecordDataSchema.Field FIELD_State = SCHEMA.getField("state");
    private final static RecordDataSchema.Field FIELD_CurrencyCode = SCHEMA.getField("currencyCode");
    private final static RecordDataSchema.Field FIELD_Amount = SCHEMA.getField("amount");
    private final static RecordDataSchema.Field FIELD_Interval = SCHEMA.getField("interval");
    private final static RecordDataSchema.Field FIELD_ProductCode = SCHEMA.getField("productCode");
    private final static RecordDataSchema.Field FIELD_AccountBalance = SCHEMA.getField("accountBalance");
    private final static RecordDataSchema.Field FIELD_PendingCancellation = SCHEMA.getField("pendingCancellation");
    private final static RecordDataSchema.Field FIELD_FreeTrial = SCHEMA.getField("freeTrial");
    private final static RecordDataSchema.Field FIELD_AccountBalanceType = SCHEMA.getField("accountBalanceType");

    public PremiumService() {
        super(new DataMap(15, 0.75F), SCHEMA);
    }

    public PremiumService(DataMap data) {
        super(data, SCHEMA);
    }

    public static PremiumService.Fields fields() {
        return _fields;
    }

    /**
     * Existence checker for id
     * 
     * @see PremiumService.Fields#id
     */
    public boolean hasId() {
        return contains(FIELD_Id);
    }

    /**
     * Remover for id
     * 
     * @see PremiumService.Fields#id
     */
    public void removeId() {
        remove(FIELD_Id);
    }

    /**
     * Getter for id
     * 
     * @see PremiumService.Fields#id
     */
    public Integer getId(GetMode mode) {
        return obtainDirect(FIELD_Id, Integer.class, mode);
    }

    /**
     * Getter for id
     * 
     * @return
     *     Optional field. Always check for null.
     * @see PremiumService.Fields#id
     */
    @Nullable
    public Integer getId() {
        return obtainDirect(FIELD_Id, Integer.class, GetMode.STRICT);
    }

    /**
     * Setter for id
     * 
     * @see PremiumService.Fields#id
     */
    public PremiumService setId(Integer value, SetMode mode) {
        putDirect(FIELD_Id, Integer.class, Integer.class, value, mode);
        return this;
    }

    /**
     * Setter for id
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see PremiumService.Fields#id
     */
    public PremiumService setId(
        @Nonnull
        Integer value) {
        putDirect(FIELD_Id, Integer.class, Integer.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Setter for id
     * 
     * @see PremiumService.Fields#id
     */
    public PremiumService setId(int value) {
        putDirect(FIELD_Id, Integer.class, Integer.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for trialPeriod
     * 
     * @see PremiumService.Fields#trialPeriod
     */
    public boolean hasTrialPeriod() {
        return contains(FIELD_TrialPeriod);
    }

    /**
     * Remover for trialPeriod
     * 
     * @see PremiumService.Fields#trialPeriod
     */
    public void removeTrialPeriod() {
        remove(FIELD_TrialPeriod);
    }

    /**
     * Getter for trialPeriod
     * 
     * @see PremiumService.Fields#trialPeriod
     */
    public String getTrialPeriod(GetMode mode) {
        return obtainDirect(FIELD_TrialPeriod, String.class, mode);
    }

    /**
     * Getter for trialPeriod
     * 
     * @return
     *     Optional field. Always check for null.
     * @see PremiumService.Fields#trialPeriod
     */
    @Nullable
    public String getTrialPeriod() {
        return obtainDirect(FIELD_TrialPeriod, String.class, GetMode.STRICT);
    }

    /**
     * Setter for trialPeriod
     * 
     * @see PremiumService.Fields#trialPeriod
     */
    public PremiumService setTrialPeriod(String value, SetMode mode) {
        putDirect(FIELD_TrialPeriod, String.class, String.class, value, mode);
        return this;
    }

    /**
     * Setter for trialPeriod
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see PremiumService.Fields#trialPeriod
     */
    public PremiumService setTrialPeriod(
        @Nonnull
        String value) {
        putDirect(FIELD_TrialPeriod, String.class, String.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for state
     * 
     * @see PremiumService.Fields#state
     */
    public boolean hasState() {
        return contains(FIELD_State);
    }

    /**
     * Remover for state
     * 
     * @see PremiumService.Fields#state
     */
    public void removeState() {
        remove(FIELD_State);
    }

    /**
     * Getter for state
     * 
     * @see PremiumService.Fields#state
     */
    public PremiumServiceState getState(GetMode mode) {
        return obtainDirect(FIELD_State, PremiumServiceState.class, mode);
    }

    /**
     * Getter for state
     * 
     * @return
     *     Optional field. Always check for null.
     * @see PremiumService.Fields#state
     */
    @Nullable
    public PremiumServiceState getState() {
        return obtainDirect(FIELD_State, PremiumServiceState.class, GetMode.STRICT);
    }

    /**
     * Setter for state
     * 
     * @see PremiumService.Fields#state
     */
    public PremiumService setState(PremiumServiceState value, SetMode mode) {
        putDirect(FIELD_State, PremiumServiceState.class, String.class, value, mode);
        return this;
    }

    /**
     * Setter for state
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see PremiumService.Fields#state
     */
    public PremiumService setState(
        @Nonnull
        PremiumServiceState value) {
        putDirect(FIELD_State, PremiumServiceState.class, String.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for currencyCode
     * 
     * @see PremiumService.Fields#currencyCode
     */
    public boolean hasCurrencyCode() {
        return contains(FIELD_CurrencyCode);
    }

    /**
     * Remover for currencyCode
     * 
     * @see PremiumService.Fields#currencyCode
     */
    public void removeCurrencyCode() {
        remove(FIELD_CurrencyCode);
    }

    /**
     * Getter for currencyCode
     * 
     * @see PremiumService.Fields#currencyCode
     */
    public String getCurrencyCode(GetMode mode) {
        return obtainDirect(FIELD_CurrencyCode, String.class, mode);
    }

    /**
     * Getter for currencyCode
     * 
     * @return
     *     Optional field. Always check for null.
     * @see PremiumService.Fields#currencyCode
     */
    @Nullable
    public String getCurrencyCode() {
        return obtainDirect(FIELD_CurrencyCode, String.class, GetMode.STRICT);
    }

    /**
     * Setter for currencyCode
     * 
     * @see PremiumService.Fields#currencyCode
     */
    public PremiumService setCurrencyCode(String value, SetMode mode) {
        putDirect(FIELD_CurrencyCode, String.class, String.class, value, mode);
        return this;
    }

    /**
     * Setter for currencyCode
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see PremiumService.Fields#currencyCode
     */
    public PremiumService setCurrencyCode(
        @Nonnull
        String value) {
        putDirect(FIELD_CurrencyCode, String.class, String.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for amount
     * 
     * @see PremiumService.Fields#amount
     */
    public boolean hasAmount() {
        return contains(FIELD_Amount);
    }

    /**
     * Remover for amount
     * 
     * @see PremiumService.Fields#amount
     */
    public void removeAmount() {
        remove(FIELD_Amount);
    }

    /**
     * Getter for amount
     * 
     * @see PremiumService.Fields#amount
     */
    public String getAmount(GetMode mode) {
        return obtainDirect(FIELD_Amount, String.class, mode);
    }

    /**
     * Getter for amount
     * 
     * @return
     *     Optional field. Always check for null.
     * @see PremiumService.Fields#amount
     */
    @Nullable
    public String getAmount() {
        return obtainDirect(FIELD_Amount, String.class, GetMode.STRICT);
    }

    /**
     * Setter for amount
     * 
     * @see PremiumService.Fields#amount
     */
    public PremiumService setAmount(String value, SetMode mode) {
        putDirect(FIELD_Amount, String.class, String.class, value, mode);
        return this;
    }

    /**
     * Setter for amount
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see PremiumService.Fields#amount
     */
    public PremiumService setAmount(
        @Nonnull
        String value) {
        putDirect(FIELD_Amount, String.class, String.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for interval
     * 
     * @see PremiumService.Fields#interval
     */
    public boolean hasInterval() {
        return contains(FIELD_Interval);
    }

    /**
     * Remover for interval
     * 
     * @see PremiumService.Fields#interval
     */
    public void removeInterval() {
        remove(FIELD_Interval);
    }

    /**
     * Getter for interval
     * 
     * @see PremiumService.Fields#interval
     */
    public String getInterval(GetMode mode) {
        return obtainDirect(FIELD_Interval, String.class, mode);
    }

    /**
     * Getter for interval
     * 
     * @return
     *     Optional field. Always check for null.
     * @see PremiumService.Fields#interval
     */
    @Nullable
    public String getInterval() {
        return obtainDirect(FIELD_Interval, String.class, GetMode.STRICT);
    }

    /**
     * Setter for interval
     * 
     * @see PremiumService.Fields#interval
     */
    public PremiumService setInterval(String value, SetMode mode) {
        putDirect(FIELD_Interval, String.class, String.class, value, mode);
        return this;
    }

    /**
     * Setter for interval
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see PremiumService.Fields#interval
     */
    public PremiumService setInterval(
        @Nonnull
        String value) {
        putDirect(FIELD_Interval, String.class, String.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for productCode
     * 
     * @see PremiumService.Fields#productCode
     */
    public boolean hasProductCode() {
        return contains(FIELD_ProductCode);
    }

    /**
     * Remover for productCode
     * 
     * @see PremiumService.Fields#productCode
     */
    public void removeProductCode() {
        remove(FIELD_ProductCode);
    }

    /**
     * Getter for productCode
     * 
     * @see PremiumService.Fields#productCode
     */
    public String getProductCode(GetMode mode) {
        return obtainDirect(FIELD_ProductCode, String.class, mode);
    }

    /**
     * Getter for productCode
     * 
     * @return
     *     Required field. Could be null for partial record.
     * @see PremiumService.Fields#productCode
     */
    @Nonnull
    public String getProductCode() {
        return obtainDirect(FIELD_ProductCode, String.class, GetMode.STRICT);
    }

    /**
     * Setter for productCode
     * 
     * @see PremiumService.Fields#productCode
     */
    public PremiumService setProductCode(String value, SetMode mode) {
        putDirect(FIELD_ProductCode, String.class, String.class, value, mode);
        return this;
    }

    /**
     * Setter for productCode
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see PremiumService.Fields#productCode
     */
    public PremiumService setProductCode(
        @Nonnull
        String value) {
        putDirect(FIELD_ProductCode, String.class, String.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for accountBalance
     * 
     * @see PremiumService.Fields#accountBalance
     */
    public boolean hasAccountBalance() {
        return contains(FIELD_AccountBalance);
    }

    /**
     * Remover for accountBalance
     * 
     * @see PremiumService.Fields#accountBalance
     */
    public void removeAccountBalance() {
        remove(FIELD_AccountBalance);
    }

    /**
     * Getter for accountBalance
     * 
     * @see PremiumService.Fields#accountBalance
     */
    public String getAccountBalance(GetMode mode) {
        return obtainDirect(FIELD_AccountBalance, String.class, mode);
    }

    /**
     * Getter for accountBalance
     * 
     * @return
     *     Optional field. Always check for null.
     * @see PremiumService.Fields#accountBalance
     */
    @Nullable
    public String getAccountBalance() {
        return obtainDirect(FIELD_AccountBalance, String.class, GetMode.STRICT);
    }

    /**
     * Setter for accountBalance
     * 
     * @see PremiumService.Fields#accountBalance
     */
    public PremiumService setAccountBalance(String value, SetMode mode) {
        putDirect(FIELD_AccountBalance, String.class, String.class, value, mode);
        return this;
    }

    /**
     * Setter for accountBalance
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see PremiumService.Fields#accountBalance
     */
    public PremiumService setAccountBalance(
        @Nonnull
        String value) {
        putDirect(FIELD_AccountBalance, String.class, String.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for pendingCancellation
     * 
     * @see PremiumService.Fields#pendingCancellation
     */
    public boolean hasPendingCancellation() {
        return contains(FIELD_PendingCancellation);
    }

    /**
     * Remover for pendingCancellation
     * 
     * @see PremiumService.Fields#pendingCancellation
     */
    public void removePendingCancellation() {
        remove(FIELD_PendingCancellation);
    }

    /**
     * Getter for pendingCancellation
     * 
     * @see PremiumService.Fields#pendingCancellation
     */
    public Boolean isPendingCancellation(GetMode mode) {
        return obtainDirect(FIELD_PendingCancellation, Boolean.class, mode);
    }

    /**
     * Getter for pendingCancellation
     * 
     * @return
     *     Optional field. Always check for null.
     * @see PremiumService.Fields#pendingCancellation
     */
    @Nullable
    public Boolean isPendingCancellation() {
        return obtainDirect(FIELD_PendingCancellation, Boolean.class, GetMode.STRICT);
    }

    /**
     * Setter for pendingCancellation
     * 
     * @see PremiumService.Fields#pendingCancellation
     */
    public PremiumService setPendingCancellation(Boolean value, SetMode mode) {
        putDirect(FIELD_PendingCancellation, Boolean.class, Boolean.class, value, mode);
        return this;
    }

    /**
     * Setter for pendingCancellation
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see PremiumService.Fields#pendingCancellation
     */
    public PremiumService setPendingCancellation(
        @Nonnull
        Boolean value) {
        putDirect(FIELD_PendingCancellation, Boolean.class, Boolean.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Setter for pendingCancellation
     * 
     * @see PremiumService.Fields#pendingCancellation
     */
    public PremiumService setPendingCancellation(boolean value) {
        putDirect(FIELD_PendingCancellation, Boolean.class, Boolean.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for freeTrial
     * 
     * @see PremiumService.Fields#freeTrial
     */
    public boolean hasFreeTrial() {
        return contains(FIELD_FreeTrial);
    }

    /**
     * Remover for freeTrial
     * 
     * @see PremiumService.Fields#freeTrial
     */
    public void removeFreeTrial() {
        remove(FIELD_FreeTrial);
    }

    /**
     * Getter for freeTrial
     * 
     * @see PremiumService.Fields#freeTrial
     */
    public Boolean isFreeTrial(GetMode mode) {
        return obtainDirect(FIELD_FreeTrial, Boolean.class, mode);
    }

    /**
     * Getter for freeTrial
     * 
     * @return
     *     Optional field. Always check for null.
     * @see PremiumService.Fields#freeTrial
     */
    @Nullable
    public Boolean isFreeTrial() {
        return obtainDirect(FIELD_FreeTrial, Boolean.class, GetMode.STRICT);
    }

    /**
     * Setter for freeTrial
     * 
     * @see PremiumService.Fields#freeTrial
     */
    public PremiumService setFreeTrial(Boolean value, SetMode mode) {
        putDirect(FIELD_FreeTrial, Boolean.class, Boolean.class, value, mode);
        return this;
    }

    /**
     * Setter for freeTrial
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see PremiumService.Fields#freeTrial
     */
    public PremiumService setFreeTrial(
        @Nonnull
        Boolean value) {
        putDirect(FIELD_FreeTrial, Boolean.class, Boolean.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Setter for freeTrial
     * 
     * @see PremiumService.Fields#freeTrial
     */
    public PremiumService setFreeTrial(boolean value) {
        putDirect(FIELD_FreeTrial, Boolean.class, Boolean.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    /**
     * Existence checker for accountBalanceType
     * 
     * @see PremiumService.Fields#accountBalanceType
     */
    public boolean hasAccountBalanceType() {
        return contains(FIELD_AccountBalanceType);
    }

    /**
     * Remover for accountBalanceType
     * 
     * @see PremiumService.Fields#accountBalanceType
     */
    public void removeAccountBalanceType() {
        remove(FIELD_AccountBalanceType);
    }

    /**
     * Getter for accountBalanceType
     * 
     * @see PremiumService.Fields#accountBalanceType
     */
    public AccountBalanceType getAccountBalanceType(GetMode mode) {
        return obtainDirect(FIELD_AccountBalanceType, AccountBalanceType.class, mode);
    }

    /**
     * Getter for accountBalanceType
     * 
     * @return
     *     Optional field. Always check for null.
     * @see PremiumService.Fields#accountBalanceType
     */
    @Nullable
    public AccountBalanceType getAccountBalanceType() {
        return obtainDirect(FIELD_AccountBalanceType, AccountBalanceType.class, GetMode.STRICT);
    }

    /**
     * Setter for accountBalanceType
     * 
     * @see PremiumService.Fields#accountBalanceType
     */
    public PremiumService setAccountBalanceType(AccountBalanceType value, SetMode mode) {
        putDirect(FIELD_AccountBalanceType, AccountBalanceType.class, String.class, value, mode);
        return this;
    }

    /**
     * Setter for accountBalanceType
     * 
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see PremiumService.Fields#accountBalanceType
     */
    public PremiumService setAccountBalanceType(
        @Nonnull
        AccountBalanceType value) {
        putDirect(FIELD_AccountBalanceType, AccountBalanceType.class, String.class, value, SetMode.DISALLOW_NULL);
        return this;
    }

    @Override
    public PremiumService clone()
        throws CloneNotSupportedException
    {
        return ((PremiumService) super.clone());
    }

    @Override
    public PremiumService copy()
        throws CloneNotSupportedException
    {
        return ((PremiumService) super.copy());
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

        /**
         * The primary key id for a premium service.
         * 
         */
        public PathSpec id() {
            return new PathSpec(getPathComponents(), "id");
        }

        /**
         * Trial period for the recurrence. See org.joda.time.Period
         * 
         */
        public PathSpec trialPeriod() {
            return new PathSpec(getPathComponents(), "trialPeriod");
        }

        /**
         * Lifecycle states for a premium serivce.
         * 
         */
        public PathSpec state() {
            return new PathSpec(getPathComponents(), "state");
        }

        /**
         * Can be ISO currency code for the money OR A Virtual Currency which is prefixed with V$. If a member purchases a 5 Job Pack he will get 5 Job Credits. Each Credit can be used to Post 1 Job.
         * 
         */
        public PathSpec currencyCode() {
            return new PathSpec(getPathComponents(), "currencyCode");
        }

        /**
         * The amount of money.
         * 
         */
        public PathSpec amount() {
            return new PathSpec(getPathComponents(), "amount");
        }

        /**
         * Duration for the recurrence in ISO. See org.joda.time.Period
         * 
         */
        public PathSpec interval() {
            return new PathSpec(getPathComponents(), "interval");
        }

        /**
         * code for the product.
         * 
         */
        public PathSpec productCode() {
            return new PathSpec(getPathComponents(), "productCode");
        }

        /**
         * The account balance.
         * 
         */
        public PathSpec accountBalance() {
            return new PathSpec(getPathComponents(), "accountBalance");
        }

        /**
         * Whether or not there is a pending cancellation scheduled for this service.
         * 
         */
        public PathSpec pendingCancellation() {
            return new PathSpec(getPathComponents(), "pendingCancellation");
        }

        /**
         * Whether or not user is on free trial.
         * 
         */
        public PathSpec freeTrial() {
            return new PathSpec(getPathComponents(), "freeTrial");
        }

        /**
         * The account balance type
         * 
         */
        public PathSpec accountBalanceType() {
            return new PathSpec(getPathComponents(), "accountBalanceType");
        }

    }

}
