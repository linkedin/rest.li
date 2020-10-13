package com.linkedin.intellij.pegasusplugin.codestyle;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import com.linkedin.intellij.pegasusplugin.PdlFileType;
import com.linkedin.intellij.pegasusplugin.PdlIcons;
import java.util.Map;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.linkedin.intellij.pegasusplugin.messages.PdlPluginMessages.*;



public class PdlColorSettingsPage implements ColorSettingsPage {
  private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[] {
      new AttributesDescriptor(message("configuration.comment"), PdlSyntaxHighlighter.COMMENT),
      new AttributesDescriptor(message("configuration.keyword"), PdlSyntaxHighlighter.KEYWORD),
      new AttributesDescriptor(message("configuration.documentation_strings"), PdlSyntaxHighlighter.DOC_COMMENT),
      new AttributesDescriptor(message("configuration.number"), PdlSyntaxHighlighter.NUMBER),
      new AttributesDescriptor(message("configuration.string"), PdlSyntaxHighlighter.STRING),
      new AttributesDescriptor(message("configuration.field"), PdlSyntaxHighlighter.FIELD),
      new AttributesDescriptor(message("configuration.type_name"), PdlSyntaxHighlighter.TYPE_NAME),
      new AttributesDescriptor(message("configuration.builtin_type_names"), PdlSyntaxHighlighter.BUILTIN_TYPE_NAME),
      new AttributesDescriptor(message("configuration.type_reference"), PdlSyntaxHighlighter.TYPE_REFERENCE),
      new AttributesDescriptor(message("configuration.enumeration_symbols"), PdlSyntaxHighlighter.ENUM_SYMBOL),
      new AttributesDescriptor(message("configuration.properties"), PdlSyntaxHighlighter.PROPERTY),
      new AttributesDescriptor(message("configuration.colon"), PdlSyntaxHighlighter.COLON),
      new AttributesDescriptor(message("configuration.optional"), PdlSyntaxHighlighter.OPTIONAL),
      new AttributesDescriptor(message("configuration.braces"), PdlSyntaxHighlighter.BRACES),
      new AttributesDescriptor(message("configuration.brackets"), PdlSyntaxHighlighter.BRACKETS),
      new AttributesDescriptor(message("configuration.parentheses"), PdlSyntaxHighlighter.PARENTHESES)
  };

  @Nullable
  @Override
  public Icon getIcon() {
    return PdlIcons.FILE;
  }

  @NotNull
  @Override
  public com.intellij.openapi.fileTypes.SyntaxHighlighter getHighlighter() {
    return new PdlSyntaxHighlighter();
  }

  @NotNull
  @Override
  public String getDemoText() {
    return PdlFileType.SAMPLE_CODE;
  }

  @Nullable
  @Override
  public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
    return null;
  }

  @NotNull
  @Override
  public AttributesDescriptor[] getAttributeDescriptors() {
    return DESCRIPTORS;
  }

  @NotNull
  @Override
  public ColorDescriptor[] getColorDescriptors() {
    return ColorDescriptor.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return message("plugin.title");
  }
}
