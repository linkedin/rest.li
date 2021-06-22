
package com.linkedin.restli.example;

import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RequiredFieldNotPresentException;
import com.linkedin.data.template.SetMode;
import java.util.List;
import javax.annotation.Generated;
import javax.annotation.Nonnull;


/**
 * An album for Rest.li
 *
 */
@Generated(value = "com.linkedin.pegasus.generator.JavaCodeUtil", comments = "Rest.li Data Template. Generated from restli-example-api/src/main/pegasus/com/linkedin/restli/example/Album.pdl.")
public class Album
    extends Album_$Versioned
{

    private final static Fields _fields = new Fields();
    private final static RecordDataSchema SCHEMA = ((RecordDataSchema) DataTemplateUtil.parseSchema("namespace com.linkedin.restli.example/**An album for Rest.li*/record Album{id:long,urn:string,title:string/**When this album was created*/creationTime:long}", SchemaFormatType.PDL));
    private Long _idField = null;
    private String _urnField = null;
    private String _titleField = null;
    private Long _creationTimeField = null;
    private ChangeListener __changeListener = new ChangeListener(this);
    private final static RecordDataSchema.Field FIELD_Id = SCHEMA.getField("id");
    private final static RecordDataSchema.Field FIELD_Urn = SCHEMA.getField("urn");
    private final static RecordDataSchema.Field FIELD_Title = SCHEMA.getField("title");
    private final static RecordDataSchema.Field FIELD_CreationTime = SCHEMA.getField("creationTime");
    static {
      SCHEMA.setVersion(DataSchemaConstants.CURRENT_VERSION);
    }

    public Album() {
        super(new DataMap(6, 0.75F), SCHEMA);
        addChangeListener(__changeListener);
    }

    public Album(DataMap data) {
        super(data, SCHEMA);
        addChangeListener(__changeListener);
    }
    public int getVersion() {
        return DataSchemaConstants.CURRENT_VERSION;
    }

    public static Fields fields() {
        return _fields;
    }

    public static RecordDataSchema dataSchema() {
        return SCHEMA;
    }

    /**
     * Existence checker for id
     *
     * @see Fields#id
     */
    public boolean hasId() {
        if (_idField!= null) {
            return true;
        }
        return super._map.containsKey("id");
    }

    /**
     * Remover for id
     *
     * @see Fields#id
     */
    public void removeId() {
        super._map.remove("id");
    }

    /**
     * Getter for id
     *
     * @see Fields#id
     */
    public Long getId(GetMode mode) {
        switch (mode) {
            case STRICT:
                return getId();
            case DEFAULT:
            case NULL:
                if (_idField!= null) {
                    return _idField;
                } else {
                    Object __rawValue = super._map.get("id");
                    _idField = DataTemplateUtil.coerceLongOutput(__rawValue);
                    return _idField;
                }
        }
        throw new IllegalStateException(("Unknown mode "+ mode));
    }

    /**
     * Getter for id
     *
     * @return
     *     Required field. Could be null for partial record.
     * @see Fields#id
     */
    @Nonnull
    public Long getId() {
        if (_idField!= null) {
            return _idField;
        } else {
            Object __rawValue = super._map.get("id");
            if (__rawValue == null) {
                throw new RequiredFieldNotPresentException("id");
            }
            _idField = DataTemplateUtil.coerceLongOutput(__rawValue);
            return _idField;
        }
    }

    /**
     * Setter for id
     *
     * @see Fields#id
     */
    public Album setId(Long value, SetMode mode) {
        switch (mode) {
            case DISALLOW_NULL:
                return setId(value);
            case REMOVE_OPTIONAL_IF_NULL:
                if (value == null) {
                    throw new IllegalArgumentException("Cannot remove mandatory field id of com.linkedin.restli.example.Album");
                } else {
                    CheckedUtil.putWithoutChecking(super._map, "id", DataTemplateUtil.coerceLongInput(value));
                    _idField = value;
                }
                break;
            case REMOVE_IF_NULL:
                if (value == null) {
                    removeId();
                } else {
                    CheckedUtil.putWithoutChecking(super._map, "id", DataTemplateUtil.coerceLongInput(value));
                    _idField = value;
                }
                break;
            case IGNORE_NULL:
                if (value!= null) {
                    CheckedUtil.putWithoutChecking(super._map, "id", DataTemplateUtil.coerceLongInput(value));
                    _idField = value;
                }
                break;
        }
        return this;
    }

    /**
     * Setter for id
     *
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see Fields#id
     */
    public Album setId(
        @Nonnull
        Long value) {
        if (value == null) {
            throw new NullPointerException("Cannot set field id of com.linkedin.restli.example.Album to null");
        } else {
            CheckedUtil.putWithoutChecking(super._map, "id", DataTemplateUtil.coerceLongInput(value));
            _idField = value;
        }
        return this;
    }

    /**
     * Setter for id
     *
     * @see Fields#id
     */
    public Album setId(long value) {
        CheckedUtil.putWithoutChecking(super._map, "id", DataTemplateUtil.coerceLongInput(value));
        _idField = value;
        return this;
    }

    /**
     * Existence checker for urn
     *
     * @see Fields#urn
     */
    public boolean hasUrn() {
        if (_urnField!= null) {
            return true;
        }
        return super._map.containsKey("urn");
    }

    /**
     * Remover for urn
     *
     * @see Fields#urn
     */
    public void removeUrn() {
        super._map.remove("urn");
    }

    /**
     * Getter for urn
     *
     * @see Fields#urn
     */
    public String getUrn(GetMode mode) {
        switch (mode) {
            case STRICT:
                return getUrn();
            case DEFAULT:
            case NULL:
                if (_urnField!= null) {
                    return _urnField;
                } else {
                    Object __rawValue = super._map.get("urn");
                    _urnField = DataTemplateUtil.coerceStringOutput(__rawValue);
                    return _urnField;
                }
        }
        throw new IllegalStateException(("Unknown mode "+ mode));
    }

    /**
     * Getter for urn
     *
     * @return
     *     Required field. Could be null for partial record.
     * @see Fields#urn
     */
    @Nonnull
    public String getUrn() {
        if (_urnField!= null) {
            return _urnField;
        } else {
            Object __rawValue = super._map.get("urn");
            if (__rawValue == null) {
                throw new RequiredFieldNotPresentException("urn");
            }
            _urnField = DataTemplateUtil.coerceStringOutput(__rawValue);
            return _urnField;
        }
    }

    /**
     * Setter for urn
     *
     * @see Fields#urn
     */
    public Album setUrn(String value, SetMode mode) {
        switch (mode) {
            case DISALLOW_NULL:
                return setUrn(value);
            case REMOVE_OPTIONAL_IF_NULL:
                if (value == null) {
                    throw new IllegalArgumentException("Cannot remove mandatory field urn of com.linkedin.restli.example.Album");
                } else {
                    CheckedUtil.putWithoutChecking(super._map, "urn", value);
                    _urnField = value;
                }
                break;
            case REMOVE_IF_NULL:
                if (value == null) {
                    removeUrn();
                } else {
                    CheckedUtil.putWithoutChecking(super._map, "urn", value);
                    _urnField = value;
                }
                break;
            case IGNORE_NULL:
                if (value!= null) {
                    CheckedUtil.putWithoutChecking(super._map, "urn", value);
                    _urnField = value;
                }
                break;
        }
        return this;
    }

    /**
     * Setter for urn
     *
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see Fields#urn
     */
    public Album setUrn(
        @Nonnull
        String value) {
        if (value == null) {
            throw new NullPointerException("Cannot set field urn of com.linkedin.restli.example.Album to null");
        } else {
            CheckedUtil.putWithoutChecking(super._map, "urn", value);
            _urnField = value;
        }
        return this;
    }

    /**
     * Existence checker for title
     *
     * @see Fields#title
     */
    public boolean hasTitle() {
        if (_titleField!= null) {
            return true;
        }
        return super._map.containsKey("title");
    }

    /**
     * Remover for title
     *
     * @see Fields#title
     */
    public void removeTitle() {
        super._map.remove("title");
    }

    /**
     * Getter for title
     *
     * @see Fields#title
     */
    public String getTitle(GetMode mode) {
        switch (mode) {
            case STRICT:
                return getTitle();
            case DEFAULT:
            case NULL:
                if (_titleField!= null) {
                    return _titleField;
                } else {
                    Object __rawValue = super._map.get("title");
                    _titleField = DataTemplateUtil.coerceStringOutput(__rawValue);
                    return _titleField;
                }
        }
        throw new IllegalStateException(("Unknown mode "+ mode));
    }

    /**
     * Getter for title
     *
     * @return
     *     Required field. Could be null for partial record.
     * @see Fields#title
     */
    @Nonnull
    public String getTitle() {
        if (_titleField!= null) {
            return _titleField;
        } else {
            Object __rawValue = super._map.get("title");
            if (__rawValue == null) {
                throw new RequiredFieldNotPresentException("title");
            }
            _titleField = DataTemplateUtil.coerceStringOutput(__rawValue);
            return _titleField;
        }
    }

    /**
     * Setter for title
     *
     * @see Fields#title
     */
    public Album setTitle(String value, SetMode mode) {
        switch (mode) {
            case DISALLOW_NULL:
                return setTitle(value);
            case REMOVE_OPTIONAL_IF_NULL:
                if (value == null) {
                    throw new IllegalArgumentException("Cannot remove mandatory field title of com.linkedin.restli.example.Album");
                } else {
                    CheckedUtil.putWithoutChecking(super._map, "title", value);
                    _titleField = value;
                }
                break;
            case REMOVE_IF_NULL:
                if (value == null) {
                    removeTitle();
                } else {
                    CheckedUtil.putWithoutChecking(super._map, "title", value);
                    _titleField = value;
                }
                break;
            case IGNORE_NULL:
                if (value!= null) {
                    CheckedUtil.putWithoutChecking(super._map, "title", value);
                    _titleField = value;
                }
                break;
        }
        return this;
    }

    /**
     * Setter for title
     *
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see Fields#title
     */
    public Album setTitle(
        @Nonnull
        String value) {
        if (value == null) {
            throw new NullPointerException("Cannot set field title of com.linkedin.restli.example.Album to null");
        } else {
            CheckedUtil.putWithoutChecking(super._map, "title", value);
            _titleField = value;
        }
        return this;
    }

    /**
     * Existence checker for creationTime
     *
     * @see Fields#creationTime
     */
    public boolean hasCreationTime() {
        if (_creationTimeField!= null) {
            return true;
        }
        return super._map.containsKey("creationTime");
    }

    /**
     * Remover for creationTime
     *
     * @see Fields#creationTime
     */
    public void removeCreationTime() {
        super._map.remove("creationTime");
    }

    /**
     * Getter for creationTime
     *
     * @see Fields#creationTime
     */
    public Long getCreationTime(GetMode mode) {
        switch (mode) {
            case STRICT:
                return getCreationTime();
            case DEFAULT:
            case NULL:
                if (_creationTimeField!= null) {
                    return _creationTimeField;
                } else {
                    Object __rawValue = super._map.get("creationTime");
                    _creationTimeField = DataTemplateUtil.coerceLongOutput(__rawValue);
                    return _creationTimeField;
                }
        }
        throw new IllegalStateException(("Unknown mode "+ mode));
    }

    /**
     * Getter for creationTime
     *
     * @return
     *     Required field. Could be null for partial record.
     * @see Fields#creationTime
     */
    @Nonnull
    public Long getCreationTime() {
        if (_creationTimeField!= null) {
            return _creationTimeField;
        } else {
            Object __rawValue = super._map.get("creationTime");
            if (__rawValue == null) {
                throw new RequiredFieldNotPresentException("creationTime");
            }
            _creationTimeField = DataTemplateUtil.coerceLongOutput(__rawValue);
            return _creationTimeField;
        }
    }

    /**
     * Setter for creationTime
     *
     * @see Fields#creationTime
     */
    public Album setCreationTime(Long value, SetMode mode) {
        switch (mode) {
            case DISALLOW_NULL:
                return setCreationTime(value);
            case REMOVE_OPTIONAL_IF_NULL:
                if (value == null) {
                    throw new IllegalArgumentException("Cannot remove mandatory field creationTime of com.linkedin.restli.example.Album");
                } else {
                    CheckedUtil.putWithoutChecking(super._map, "creationTime", DataTemplateUtil.coerceLongInput(value));
                    _creationTimeField = value;
                }
                break;
            case REMOVE_IF_NULL:
                if (value == null) {
                    removeCreationTime();
                } else {
                    CheckedUtil.putWithoutChecking(super._map, "creationTime", DataTemplateUtil.coerceLongInput(value));
                    _creationTimeField = value;
                }
                break;
            case IGNORE_NULL:
                if (value!= null) {
                    CheckedUtil.putWithoutChecking(super._map, "creationTime", DataTemplateUtil.coerceLongInput(value));
                    _creationTimeField = value;
                }
                break;
        }
        return this;
    }

    /**
     * Setter for creationTime
     *
     * @param value
     *     Must not be null. For more control, use setters with mode instead.
     * @see Fields#creationTime
     */
    public Album setCreationTime(
        @Nonnull
        Long value) {
        if (value == null) {
            throw new NullPointerException("Cannot set field creationTime of com.linkedin.restli.example.Album to null");
        } else {
            CheckedUtil.putWithoutChecking(super._map, "creationTime", DataTemplateUtil.coerceLongInput(value));
            _creationTimeField = value;
        }
        return this;
    }

    /**
     * Setter for creationTime
     *
     * @see Fields#creationTime
     */
    public Album setCreationTime(long value) {
        CheckedUtil.putWithoutChecking(super._map, "creationTime", DataTemplateUtil.coerceLongInput(value));
        _creationTimeField = value;
        return this;
    }

    @Override
    public Album clone()
        throws CloneNotSupportedException
    {
        Album __clone = ((Album) super.clone());
        __clone.__changeListener = new ChangeListener(__clone);
        __clone.addChangeListener(__clone.__changeListener);
        return __clone;
    }

    @Override
    public Album copy()
        throws CloneNotSupportedException
    {
        Album __copy = ((Album) super.copy());
        __copy._urnField = null;
        __copy._creationTimeField = null;
        __copy._idField = null;
        __copy._titleField = null;
        __copy.__changeListener = new ChangeListener(__copy);
        __copy.addChangeListener(__copy.__changeListener);
        return __copy;
    }

    private static class ChangeListener
        implements com.linkedin.data.collections.CheckedMap.ChangeListener<String, Object>
    {

        private final Album __objectRef;

        private ChangeListener(Album reference) {
            __objectRef = reference;
        }

        @Override
        public void onUnderlyingMapChanged(String key, Object value) {
            switch (key) {
                case "urn":
                    __objectRef._urnField = null;
                    break;
                case "creationTime":
                    __objectRef._creationTimeField = null;
                    break;
                case "id":
                    __objectRef._idField = null;
                    break;
                case "title":
                    __objectRef._titleField = null;
                    break;
            }
        }

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

        public PathSpec id() {
            return new PathSpec(getPathComponents(), "id");
        }

        public PathSpec urn() {
            return new PathSpec(getPathComponents(), "urn");
        }

        public PathSpec title() {
            return new PathSpec(getPathComponents(), "title");
        }

        /**
         * When this album was created
         *
         */
        public PathSpec creationTime() {
            return new PathSpec(getPathComponents(), "creationTime");
        }

    }

}
