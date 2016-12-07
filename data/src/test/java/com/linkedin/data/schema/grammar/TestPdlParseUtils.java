package com.linkedin.data.schema.grammar;

import com.linkedin.data.grammar.PdlParser;
import java.math.BigDecimal;
import java.util.Arrays;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TestPdlParseUtils
{
  @Test
  public void testExtractMarkdown()
  {
    String extracted = PdlParseUtils.extractMarkdown(
        "  /**\n" +
        "   * The quick\n" +
        "   * brown fox\n" +
        "   */\n");

    assertEquals(extracted, "The quick\n brown fox");
  }

  @Test
  public void testUnescapeDocstring()
  {
    String extracted = PdlParseUtils.extractMarkdown(
        "  /**\n" +
        "   * &lt;div&gt;Some html&lt;/div&gt;\n" +
        "   * &#47;&#42; A comment &#42;&#47;\n" +
        "   */\n");
    assertEquals(extracted,
        "<div>Some html</div>\n" +
        " /* A comment */");
  }

  @Test
  public void testExtractString()
  {
    String extracted = PdlParseUtils.extractString("\"A string with escape chars: \\n\\t\\f\"");
    assertEquals(extracted, "A string with escape chars: \n\t\f");
  }

  @Test
  public void testStripMargin()
  {
    String docString = PdlParseUtils.stripMargin(
        "   * The quick\n" +
        "   * brown fox\n");
    assertEquals(docString,
        " The quick\n" +
        " brown fox\n");
  }

  @Test
  public void testUnescapeIdentifier()
  {
    assertEquals(PdlParseUtils.unescapeIdentifier("`record`"), "record");
    assertEquals(PdlParseUtils.unescapeIdentifier("notEscaped"), "notEscaped");
  }

  @Test
  public void testJoin()
  {
    PdlParser.IdentifierContext a = new PdlParser.IdentifierContext(null, 0);
    a.value  = "a";
    PdlParser.IdentifierContext b = new PdlParser.IdentifierContext(null, 0);
    b.value  = "b";

    assertEquals(PdlParseUtils.join(Arrays.asList(a, b)), "a.b");
  }

  @Test
  public void testToNumber()
  {
    Number n1 = PdlParseUtils.toNumber("1");
    assertEquals(n1.getClass(), Integer.class);
    assertEquals(n1.intValue(), 1);

    Number n10000000000 = PdlParseUtils.toNumber("10000000000");
    assertEquals(n10000000000.getClass(), Long.class);
    assertEquals(n10000000000.longValue(), 10000000000L);

    Number n1_0 = PdlParseUtils.toNumber("1.0");
    assertEquals(n1_0.getClass(), Double.class);
    assertEquals(n1_0.doubleValue(), 1.0d, 0.001d);

    Number n1_0e10 = PdlParseUtils.toNumber("1234567.1e1000");
    assertEquals(n1_0e10.getClass(), BigDecimal.class);
  }
}
