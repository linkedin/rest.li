/*
 * Copyright 2016 Coursera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.intellij.pegasusplugin.schemadoc;

import com.intellij.lexer.LexerBase;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.text.CharArrayUtil;
import com.linkedin.intellij.pegasusplugin.schemadoc.psi.SchemadocTypes;
import java.io.IOException;

/**
 * Based on JavaDocLexer.java.  This lexer wraps /src/main/grammar/schemadoc.flex, tokenizing the contents
 * of a doc comment correctly.
 */
public class SchemadocLexer extends MergingLexerAdapter {
  public SchemadocLexer() {
    super(new AsteriskStripperLexer(new DocCommentLexer()),
      TokenSet.create(SchemadocTypes.DOC_COMMENT_CONTENT, TokenType.WHITE_SPACE));
  }

  private static class AsteriskStripperLexer extends LexerBase {
    private final DocCommentLexer _flex;
    private CharSequence _buffer;
    private int _bufferIndex;
    private int _bufferEndOffset;
    private int _tokenEndOffset;
    private int _state;
    private IElementType _tokenType;
    private boolean _afterLineBreak;
    private boolean _inLeadingSpace;

    public AsteriskStripperLexer(final DocCommentLexer flex) {
      _flex = flex;
    }

    public final void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
      _buffer = buffer;
      _bufferIndex =  startOffset;
      _bufferEndOffset = endOffset;
      _tokenType = null;
      _tokenEndOffset = startOffset;
      _flex.reset(_buffer, startOffset, endOffset, initialState);
    }

    public int getState() {
      return _state;
    }

    public CharSequence getBufferSequence() {
      return _buffer;
    }

    public int getBufferEnd() {
      return _bufferEndOffset;
    }

    public final IElementType getTokenType() {
      locateToken();
      return _tokenType;
    }

    public final int getTokenStart() {
      locateToken();
      return _bufferIndex;
    }

    public final int getTokenEnd() {
      locateToken();
      return _tokenEndOffset;
    }


    public final void advance() {
      locateToken();
      _tokenType = null;
    }

    protected final void locateToken() {
      if (_tokenType != null) {
        return;
      }
      doLocateToken();

      if (_tokenType == TokenType.WHITE_SPACE) {
        _afterLineBreak = CharArrayUtil.containLineBreaks(_buffer, getTokenStart(), getTokenEnd());
      }
    }

    private void doLocateToken() {
      if (_tokenEndOffset == _bufferEndOffset) {
        _tokenType = null;
        _bufferIndex = _bufferEndOffset;
        return;
      }

      _bufferIndex = _tokenEndOffset;

      if (_afterLineBreak) {
        _afterLineBreak = false;
        while (_tokenEndOffset < _bufferEndOffset && _buffer.charAt(_tokenEndOffset) == '*'
            &&  (_tokenEndOffset + 1 >= _bufferEndOffset || _buffer.charAt(_tokenEndOffset + 1) != '/')) {
          _tokenEndOffset++;
        }

        _inLeadingSpace = true;
        if (_bufferIndex < _tokenEndOffset) {
          _tokenType = SchemadocTypes.DOC_COMMENT_LEADING_ASTRISK;
          return;
        }
      }

      if (_inLeadingSpace) {
        _inLeadingSpace = false;
        boolean lf = false;
        while (_tokenEndOffset < _bufferEndOffset && Character.isWhitespace(_buffer.charAt(_tokenEndOffset))) {
          if (_buffer.charAt(_tokenEndOffset) == '\n') {
            lf = true;
          }
          _tokenEndOffset++;
        }

        final int state = _flex.yystate();
        if (state == DocCommentLexer.COMMENT_DATA || _tokenEndOffset < _bufferEndOffset) {
          _flex.yybegin(DocCommentLexer.COMMENT_DATA_START);
        }

        if (_bufferIndex < _tokenEndOffset) {
          _tokenType = lf
            ? TokenType.WHITE_SPACE
            : SchemadocTypes.DOC_COMMENT_CONTENT;

          return;
        }
      }

      flexLocateToken();
    }

    private void flexLocateToken() {
      try {
        _state = _flex.yystate();
        _flex.goTo(_bufferIndex);
        _tokenType = _flex.advance();
        _tokenEndOffset = _flex.getTokenEnd();
      } catch (IOException e) {
        // Can't be
      }
    }
  }
}
