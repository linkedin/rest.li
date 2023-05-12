package com.linkedin.util;

import java.io.IOException;
import java.io.StringWriter;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestLineColumnNumberWriter
{

  @Test
  public void testHandlesDifferentNewlines() throws IOException
  {
    LineColumnNumberWriter writer = new LineColumnNumberWriter(new StringWriter());
    writer.write("1\n2\n3\n");
    Assert.assertEquals(writer.getCurrentPosition(), new LineColumnNumberWriter.CharacterPosition(4, 1));
    writer.write("1\r\n2\r\n3\r\n");
    Assert.assertEquals(writer.getCurrentPosition(), new LineColumnNumberWriter.CharacterPosition(7, 1));
    writer.write("1\r2\r3\r");
    Assert.assertEquals(writer.getCurrentPosition(), new LineColumnNumberWriter.CharacterPosition(10, 1));
  }

  @Test
  public void testSavedPositionIgnoresLeadingWhitespace() throws IOException
  {
    LineColumnNumberWriter writer = new LineColumnNumberWriter(new StringWriter());
    writer.write("123\n");
    writer.saveCurrentPosition();
    writer.saveCurrentPosition();
    writer.write(" \n ");
    writer.write("456");
    writer.saveCurrentPosition();
    writer.write("   789");
    Assert.assertEquals(writer.popSavedPosition(), new LineColumnNumberWriter.CharacterPosition(3, 8));
    Assert.assertEquals(writer.popSavedPosition(), new LineColumnNumberWriter.CharacterPosition(3, 2));
    Assert.assertEquals(writer.popSavedPosition(), new LineColumnNumberWriter.CharacterPosition(3, 2));
  }

  @Test
  public void testGetLastNonWhitespacePosition() throws IOException
  {
    LineColumnNumberWriter writer = new LineColumnNumberWriter(new StringWriter());
    writer.write("123");
    Assert.assertEquals(writer.getLastNonWhitespacePosition(), new LineColumnNumberWriter.CharacterPosition(1, 3));
    writer.write("\n ");
    Assert.assertEquals(writer.getLastNonWhitespacePosition(), new LineColumnNumberWriter.CharacterPosition(1, 3));
    writer.write("4");
    Assert.assertEquals(writer.getLastNonWhitespacePosition(), new LineColumnNumberWriter.CharacterPosition(2, 2));
  }
}
