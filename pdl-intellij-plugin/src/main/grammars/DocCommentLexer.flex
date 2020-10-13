package com.linkedin.intellij.pegasusplugin.schemadoc;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import com.linkedin.intellij.pegasusplugin.schemadoc.psi.SchemadocTypes;
import com.intellij.psi.TokenType;
%%

%{
  public DocCommentLexer() {
    this((java.io.Reader)null);
  }

  public void goTo(int offset) {
    zzCurrentPos = zzMarkedPos = zzStartRead = offset;
    zzAtEOF = false;
  }
%}

%class DocCommentLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

%state COMMENT_DATA_START
%state COMMENT_DATA
%state COMMENT_DATA_LINE_START

WHITE_DOC_SPACE_CHAR=[\ \t\f\n\r]
WHITE_DOC_SPACE_NO_LR=[\ \t\f]

%%

// Based on _JavaDocLexer.flex

// Similar to _JavaDocLexer.flex (which is wrapped by DocCommentLexer.java),
// this lexer is wrapped by DocLexer, which goes on
// to reprocess the token stream, and we do not currently reprocess the token stream.
<YYINITIAL> "/**" { yybegin(COMMENT_DATA_START); return SchemadocTypes.DOC_COMMENT_START; }
<COMMENT_DATA_START> {WHITE_DOC_SPACE_CHAR}+ { return TokenType.WHITE_SPACE; }

<COMMENT_DATA, COMMENT_DATA_LINE_START>  {WHITE_DOC_SPACE_NO_LR}+ { return SchemadocTypes.DOC_COMMENT_CONTENT; }
<COMMENT_DATA, COMMENT_DATA_LINE_START>  [\n\r]+{WHITE_DOC_SPACE_CHAR}* { yybegin(COMMENT_DATA_LINE_START); return TokenType.WHITE_SPACE; }
<COMMENT_DATA_LINE_START> "*" { yybegin(COMMENT_DATA); return SchemadocTypes.DOC_COMMENT_LEADING_ASTRISK; }
<COMMENT_DATA_START, COMMENT_DATA, COMMENT_DATA_LINE_START> . { yybegin(COMMENT_DATA); return SchemadocTypes.DOC_COMMENT_CONTENT; }

"*"+"/" { return SchemadocTypes.DOC_COMMENT_END; }
[^] { return TokenType.BAD_CHARACTER; }
