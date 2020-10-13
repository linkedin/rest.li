package com.linkedin.intellij.pegasusplugin;

import com.intellij.openapi.fileTypes.LanguageFileType;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.linkedin.intellij.pegasusplugin.messages.PdlPluginMessages.*;


public class PdlFileType extends LanguageFileType {
  public static final PdlFileType INSTANCE = new PdlFileType();

  private PdlFileType() {
    super(PdlLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getName() {
    return message("filetype.name");
  }

  @NotNull
  @Override
  public String getDescription() {
    return message("filetype.description");
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "pdl";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return PdlIcons.FILE;
  }

  /**
   * Constructs a full PDL filename.
   * @param baseFilename filename without extension
   * @return filename with extension
   */
  public static String toFilename(String baseFilename) {
    return String.format("%s.%s", baseFilename, INSTANCE.getDefaultExtension());
  }

  public static final String SAMPLE_CODE =
    "namespace org.example\n"
  + "\n"
  + "import org.example.Member1\n"
  + "import org.example.Member2\n"
  + "\n"
  + "/** \n"
  + " * A Fortune.\n"
  + " */\n"
  + "@language.propertyKey = \"property value\"\n"
  + "record Fortune {\n"
  + "  field1: optional int // comment 1\n"
  + "  field2: array[int] = [1, 2, 3]\n"
  + "  /* comment 2 */\n"
  + "  @deprecated\n"
  + "  field3: map[string, int] = { \"a\": 1, \"b\": 2 }\n"
  + "  inline: record Inline {\n"
  + "    inlineField1: union[Member1, Member2]\n"
  + "  }\n"
  + "}\n";
}
