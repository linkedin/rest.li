package com.linkedin.data.schema.generator;


import com.linkedin.pegasus.generator.test.IntUnionRecord;
import com.linkedin.pegasus.generator.test.StringUnionRecord;

import com.linkedin.pegasus.generator.test.unnamed.UnionNameConflict;
import com.linkedin.pegasus.generator.test.unnamed.UnionNameConflictArray;
import com.linkedin.pegasus.generator.test.unnamed.UnionNameConflictMap;
import java.io.IOException;

import org.testng.annotations.Test;


/**
 * @author Keren Jin
 */
public class TestPegasusDataTemplateGenerator
{
  // this methods tests if the nested union class is generated in the defining class
  // if the compilation fails, the test fails
  // see also build.gradle
  @Test
  public void testIncludeUnion() throws IOException
  {
    // add usage to ensure the import will not be automatically trimmed by IDE
    IntUnionRecord.IntUnion intUnion;
    StringUnionRecord.StringUnion stringUnion;
  }

  @Test
  public void testIncludeUnionConflictResolution() throws IOException
  {
    // add usage to ensure the import will not be automatically trimmed by IDE
    UnionNameConflict.UnionNameConflict$Union union;
    UnionNameConflict.UnionNameConflictUnion union2;
    UnionNameConflictArray.UnionNameConflictArray$Array unionArray;
    UnionNameConflictArray.UnionNameConflictArray$Union unionArrayUnion;
    UnionNameConflictArray.UnionNameConflictArray$UnionArray unionArray2;
    UnionNameConflictMap.UnionNameConflictMap$Map unionMap;
    UnionNameConflictMap.UnionNameConflictMap$Union unionMapUnion;
    UnionNameConflictMap.UnionNameConflictMap$UnionMap unionMap2;
  }
}
