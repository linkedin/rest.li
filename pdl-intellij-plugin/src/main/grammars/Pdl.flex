package com.linkedin.intellij.pegasusplugin;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.linkedin.intellij.pegasusplugin.psi.Types;
import com.intellij.psi.TokenType;
import com.linkedin.intellij.pegasusplugin.psi.PdlElementType;

%%

%class PdlLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

LineTerminator = \n|\r|\r\n
InputCharacter = [^\r\n]
WhiteSpace = {LineTerminator} | [ \t\f]
Comma = ","

BlockCommentEmpty = "/**/"
BlockCommentNonEmpty = "/*" [^*] ~ "*/"
// Comment can be the last line of the pdlPsiFile, without line terminator.
LineComment = "//" {InputCharacter}* {LineTerminator}?

// Based on _JavaLexer.flex:
DOC_COMMENT="/*""*"+("/"|([^"/""*"]{COMMENT_TAIL}))?
COMMENT_TAIL=([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?

Identifier = [A-Za-z_] [A-Za-z0-9_-]* // Avro/Pegasus identifiers

EscapedIdentifier = "`" {Identifier} "`"
PropertyKey = [A-Za-z0-9_-]+  // Property keys that are not escaped.
EscapedPropertyKey = "`" [^`]+ "`"  // Escaped property keys

// From json.org:
NonNegativeIntegerLiteral = 0 | [1-9][0-9]*
IntegerLiteral = -? {NonNegativeIntegerLiteral}
NumberLiteral = {IntegerLiteral} (\.[0-9]+)? ([eE][+-]?[0-9]+)?
StringLiteral = \" ( [^\"\\] | \\ ( [\"\\/bfnrt] | u[0-9]{4} ) )* \"

%state PROP_DEF

%%

/* Standard lexing context */
<YYINITIAL> {
  /* Keywords */
  "namespace"            { return Types.NAMESPACE_KEYWORD; }
  "package"              { return Types.PACKAGE_KEYWORD; }
  "import"               { return Types.IMPORT_KEYWORD; }
  "includes"             { return Types.INCLUDES_KEYWORD; }
  "optional"             { return Types.OPTIONAL; }
  "record"               { return Types.RECORD_KEYWORD; }
  "enum"                 { return Types.ENUM_KEYWORD; }
  "fixed"                { return Types.FIXED_KEYWORD; }
  "typeref"              { return Types.TYPEREF_KEYWORD; }
  "union"                { return Types.UNION_KEYWORD; }
  "map"                  { return Types.MAP_KEYWORD; }
  "array"                { return Types.ARRAY_KEYWORD; }

  "null"                 { return Types.NULL; }
  "true"                 { return Types.TRUE; }
  "false"                { return Types.FALSE; }

  "("                    { return Types.OPEN_PAREN; }
  ")"                    { return Types.CLOSE_PAREN; }
  "{"                    { return Types.OPEN_BRACE; }
  "}"                    { return Types.CLOSE_BRACE; }
  "["                    { return Types.OPEN_BRACKET; }
  "]"                    { return Types.CLOSE_BRACKET; }
  ":"                    { return Types.COLON; }
  "="                    { return Types.EQUALS; }
  "@"                    { yybegin(PROP_DEF); return Types.AT; }  // Begin the property key context
  "."                    { return Types.DOT; }

  /* Identifiers */
  {Identifier}           { return Types.IDENTIFIER; }
  {EscapedIdentifier}    { return Types.IDENTIFIER; }

  /* Literals */
  {NumberLiteral}        { return Types.NUMBER_LITERAL; }
  {StringLiteral}        { return Types.STRING; }
}

/* Property key definition lexing context */
<PROP_DEF> {
  {Identifier}           { return Types.IDENTIFIER; }
  {EscapedIdentifier}    { return Types.IDENTIFIER; }
  {PropertyKey}          { return Types.PROPERTY_KEY; }
  {EscapedPropertyKey}   { return Types.ESCAPED_PROPERTY_KEY; }
  "."                    { return Types.DOT; }

  // This context can be ended with an equals sign, or with whitespace-like tokens (see below)
  "="                    { yybegin(YYINITIAL); return Types.EQUALS; }
}

/* These rules apply in all contexts, and they all revert back to the standard context if matched */
{DOC_COMMENT}?           { yybegin(YYINITIAL); return PdlElementType.DOC_COMMENT; }
{LineComment}            { yybegin(YYINITIAL); return Types.SINGLE_LINE_COMMENT; }
{BlockCommentEmpty}      { yybegin(YYINITIAL); return Types.BLOCK_COMMENT_EMPTY; }
{BlockCommentNonEmpty}   { yybegin(YYINITIAL); return Types.BLOCK_COMMENT_NON_EMPTY; }
{Comma}                  { yybegin(YYINITIAL); return Types.COMMA; }  // Commas are effectively treated as whitespace
{WhiteSpace}+            { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

/* Error fallback */
[^]                      { return TokenType.BAD_CHARACTER; }
