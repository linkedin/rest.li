package com.linkedin.intellij.pegasusplugin.codestyle;

import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.linkedin.intellij.pegasusplugin.PdlLexerAdapter;
import com.linkedin.intellij.pegasusplugin.psi.PdlElementType;
import com.linkedin.intellij.pegasusplugin.psi.Types;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.*;

// TODO(jbetz): The layout of this code is from the intelliJ tutorial, but it's super redundant.
// Restructure this reduce all the redundancy.
public class PdlSyntaxHighlighter extends SyntaxHighlighterBase {
  public static final TextAttributesKey IDENTIFIER = createTextAttributesKey("PDL_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
  public static final TextAttributesKey KEYWORD = createTextAttributesKey("PDL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey DOC_COMMENT = createTextAttributesKey("PDL_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT);
  public static final TextAttributesKey STRING = createTextAttributesKey("PDL_STRING", DefaultLanguageHighlighterColors.STRING);
  public static final TextAttributesKey NUMBER = createTextAttributesKey("PDL_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
  public static final TextAttributesKey BUILTIN_TYPE_NAME = createTextAttributesKey("PDL_BUILTIN_TYPE_NAME", DefaultLanguageHighlighterColors.CLASS_REFERENCE);
  public static final TextAttributesKey COMMENT = createTextAttributesKey("PDL_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
  public static final TextAttributesKey TYPE_NAME = createTextAttributesKey("PDL_TYPE_NAME", DefaultLanguageHighlighterColors.CLASS_NAME);
  public static final TextAttributesKey TYPE_REFERENCE = createTextAttributesKey("PDL_TYPE_REFERENCE", DefaultLanguageHighlighterColors.CLASS_REFERENCE);
  public static final TextAttributesKey FIELD = createTextAttributesKey("PDL_FIELD", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
  public static final TextAttributesKey ENUM_SYMBOL = createTextAttributesKey("PDL_ENUM_SYMBOL", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
  public static final TextAttributesKey PROPERTY = createTextAttributesKey("PDL_PROPERTY", DefaultLanguageHighlighterColors.METADATA);
  public static final TextAttributesKey COLON = createTextAttributesKey("PDL_COLON", DefaultLanguageHighlighterColors.OPERATION_SIGN);
  public static final TextAttributesKey OPTIONAL = createTextAttributesKey("PDL_OPTIONAL", DefaultLanguageHighlighterColors.SEMICOLON);
  public static final TextAttributesKey BRACES = createTextAttributesKey("PDL_BRACES", DefaultLanguageHighlighterColors.BRACES);
  public static final TextAttributesKey BRACKETS = createTextAttributesKey("PDL_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
  public static final TextAttributesKey PARENTHESES = createTextAttributesKey("PDL_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
  public static final TextAttributesKey BAD_CHARACTER = createTextAttributesKey("PDL_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);

  // Errors and warnings
  public static final TextAttributesKey DEPRECATED = HighlightInfoType.DEPRECATED.getAttributesKey();
  public static final TextAttributesKey UNRESOLVED_REFERENCE = HighlightInfoType.WRONG_REF.getAttributesKey();
  public static final TextAttributesKey UNRESOLVED_IMPORT = HighlightInfoType.WRONG_REF.getAttributesKey();
  public static final TextAttributesKey NAMESPACE_MISMATCH = HighlightInfoType.WRONG_REF.getAttributesKey();
  public static final TextAttributesKey NAMESPACE_UNABLE_TO_VALIDATE = HighlightInfoType.WEAK_WARNING.getAttributesKey();

  private static final TextAttributesKey[] IDENTIFIER_KEYS = new TextAttributesKey[]{IDENTIFIER};
  private static final TextAttributesKey[] KEYWORD_KEYS = new TextAttributesKey[]{KEYWORD};
  private static final TextAttributesKey[] DOC_COMMENT_KEYS = new TextAttributesKey[]{DOC_COMMENT};
  private static final TextAttributesKey[] STRING_KEYS = new TextAttributesKey[]{STRING};
  private static final TextAttributesKey[] NUMBER_KEYS = new TextAttributesKey[]{NUMBER};
  private static final TextAttributesKey[] BUILTIN_TYPE_NAME_KEYS = new TextAttributesKey[]{BUILTIN_TYPE_NAME};
  private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{COMMENT};
  private static final TextAttributesKey[] COLON_KEYS = new TextAttributesKey[]{COLON};
  private static final TextAttributesKey[] OPTIONAL_KEYS = new TextAttributesKey[]{OPTIONAL};
  private static final TextAttributesKey[] BRACES_KEYS = new TextAttributesKey[]{BRACES};
  private static final TextAttributesKey[] BRACKETS_KEYS = new TextAttributesKey[]{BRACKETS};
  private static final TextAttributesKey[] PARENTHESES_KEYS = new TextAttributesKey[]{PARENTHESES};
  private static final TextAttributesKey[] BAD_CHAR_KEYS = new TextAttributesKey[]{BAD_CHARACTER};
  private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];

  private static final Set<IElementType> KEYWORDS = new HashSet<IElementType>();
  static {
    KEYWORDS.add(Types.NAMESPACE_KEYWORD);
    KEYWORDS.add(Types.IMPORT_KEYWORD);
    KEYWORDS.add(Types.ENUM_KEYWORD);
    KEYWORDS.add(Types.FIXED_KEYWORD);
    KEYWORDS.add(Types.RECORD_KEYWORD);
    KEYWORDS.add(Types.PACKAGE_KEYWORD);
    KEYWORDS.add(Types.INCLUDES_KEYWORD);
    KEYWORDS.add(Types.TYPEREF_KEYWORD);
    KEYWORDS.add(Types.TRUE);
    KEYWORDS.add(Types.FALSE);
    KEYWORDS.add(Types.NULL);
  }

  private static final Set<IElementType> BUILTIN_TYPES = new HashSet<IElementType>();
  static {
    BUILTIN_TYPES.add(Types.MAP_KEYWORD);
    BUILTIN_TYPES.add(Types.ARRAY_KEYWORD);
    BUILTIN_TYPES.add(Types.UNION_KEYWORD);
  }

  @NotNull
  @Override
  public Lexer getHighlightingLexer() {
    return new PdlLexerAdapter();
  }

  @NotNull
  @Override
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    if (KEYWORDS.contains(tokenType)) {
      return KEYWORD_KEYS;
    } else if (
      tokenType.equals(PdlElementType.DOC_COMMENT)) {
      return DOC_COMMENT_KEYS;
    } else if (tokenType.equals(Types.STRING)) {
      return STRING_KEYS;
    } else if (tokenType.equals(Types.NUMBER_LITERAL)) {
      return NUMBER_KEYS;
    } else if (BUILTIN_TYPES.contains(tokenType)) {
      return BUILTIN_TYPE_NAME_KEYS;
    } else if (
      tokenType.equals(Types.SINGLE_LINE_COMMENT)
      || tokenType.equals(Types.BLOCK_COMMENT_EMPTY)
      || tokenType.equals(Types.BLOCK_COMMENT_NON_EMPTY)
      || tokenType.equals(Types.COMMA)) {
      return COMMENT_KEYS;
    } else if (tokenType.equals(Types.COLON)) {
      return COLON_KEYS;
    } else if (tokenType.equals(Types.OPTIONAL)) {
      return OPTIONAL_KEYS;
    } else if (tokenType.equals(Types.OPEN_BRACE) || tokenType.equals(Types.CLOSE_BRACE)) {
      return BRACES_KEYS;
    } else if (tokenType.equals(Types.OPEN_BRACKET) || tokenType.equals(Types.CLOSE_BRACKET)) {
      return BRACKETS_KEYS;
    } else if (tokenType.equals(Types.OPEN_PAREN) || tokenType.equals(Types.CLOSE_PAREN)) {
      return PARENTHESES_KEYS;
    } else if (tokenType.equals(Types.IDENTIFIER)) {
      return IDENTIFIER_KEYS;
    } else if (tokenType.equals(TokenType.BAD_CHARACTER)) {
      return BAD_CHAR_KEYS;
    } else {
      return EMPTY_KEYS;
    }
  }
}
