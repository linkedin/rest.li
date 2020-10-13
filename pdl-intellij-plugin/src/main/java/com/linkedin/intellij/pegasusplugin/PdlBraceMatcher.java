package com.linkedin.intellij.pegasusplugin;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.linkedin.intellij.pegasusplugin.psi.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PdlBraceMatcher implements PairedBraceMatcher {

  /**
   * Returns the array of definitions for brace pairs that need to be matched when
   * editing code in the language.
   *
   * @return the array of brace pair definitions.
   */
  @Override
  public BracePair[] getPairs() {
    return new BracePair[] {
      new BracePair(Types.OPEN_BRACE, Types.CLOSE_BRACE, true),
      new BracePair(Types.OPEN_BRACKET, Types.CLOSE_BRACKET, false),
      new BracePair(Types.OPEN_PAREN, Types.CLOSE_PAREN, false)
    };
  }

  /**
   * Returns true if paired rbrace should be inserted after lbrace of given type when lbrace is encountered before contextType token.
   * It is safe to always return true, then paired brace will be inserted anyway.
   * @param lbraceType lbrace for which information is queried
   * @param contextType token type that follows lbrace
   * @return true / false as described
   */
  @Override
  public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
    return true;
  }

  /**
   * Returns the start offset of the code construct which owns the opening structural brace at the specified offset. For example,
   * if the opening brace belongs to an 'if' statement, returns the start offset of the 'if' statement.
   *
   * @param file the file in which brace matching is performed.
   * @param openingBraceOffset the offset of an opening structural brace.
   * @return the offset of corresponding code construct, or the same offset if not defined.
   */
  @Override
  public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
    return openingBraceOffset;
  }
}
