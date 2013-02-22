package com.linkedin.restli.common;


import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.schema.validator.DataSchemaAnnotationValidator;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestEmptyRecord
{
  @Test
  public void testEmpty()
  {
    final EmptyRecord record = new EmptyRecord();
    final DataSchemaAnnotationValidator validator = new DataSchemaAnnotationValidator(record.schema());
    final ValidationResult result = ValidateDataAgainstSchema.validate(record.data(), record.schema(), new ValidationOptions(), validator);
    Assert.assertTrue(result.isValid());
  }

  @Test
  public void testNonEmpty()
  {
    final EmptyRecord record = new EmptyRecord();
    record.data().put("non", "empty");
    final DataSchemaAnnotationValidator validator = new DataSchemaAnnotationValidator(record.schema());
    final ValidationResult result = ValidateDataAgainstSchema.validate(record.data(),
                                                                       record.schema(),
                                                                       new ValidationOptions(),
                                                                       validator);
    Assert.assertFalse(result.isValid());
  }
}
