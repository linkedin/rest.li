package com.linkedin.intellij.pegasusplugin.formatter;

import com.intellij.formatting.Block;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.formatting.Indent;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.alignment.AlignmentStrategy;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.linkedin.intellij.pegasusplugin.PdlLanguage;
import com.linkedin.intellij.pegasusplugin.psi.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Spells out the code formatting rules for PDL.
 */
public class PdlFormattingModelBuilder implements FormattingModelBuilder {
  @NotNull
  @Override
  public FormattingModel createModel(
    @NotNull PsiElement element, @NotNull CodeStyleSettings settings) {
    Block block =
      PdlAbstractBlock.createBlock(
        element.getNode(),
        Indent.getAbsoluteNoneIndent(),
        null,
        AlignmentStrategy.getNullStrategy(),
        settings,
        createSpacingBuilder(settings));

    return FormattingModelProvider.createFormattingModelForPsiFile(
      element.getContainingFile(), block, settings);
  }

  @NotNull
  private static SpacingBuilder createSpacingBuilder(@NotNull CodeStyleSettings settings) {
    return new SpacingBuilder(settings, PdlLanguage.INSTANCE)
      .before(Types.COMMA).spaceIf(false)
      .after(Types.COMMA).spaceIf(true)
      .before(Types.COLON).spaceIf(false)
      .after(Types.COLON).spaceIf(true)
      .before(Types.EQUALS).spaceIf(true)
      .after(Types.EQUALS).spaceIf(true)
      .before(Types.OPTIONAL).spaceIf(false)
      .after(Types.OPTIONAL).spaceIf(true)
      .around(Types.DOT).none()

      .before(Types.NAMESPACE_DECLARATION).blankLines(0)
      .after(Types.NAMESPACE_DECLARATION).blankLines(1)
      .after(Types.NAMESPACE_KEYWORD).spaceIf(true)

      .around(Types.IMPORT_DECLARATIONS).blankLines(1)
      .around(Types.IMPORT_DECLARATION).lineBreakInCode()
      .after(Types.IMPORT_KEYWORD).spaceIf(true)

      .before(Types.TYPE_DECLARATION).lineBreakInCode()
      .after(Types.TYPE_DECLARATION).lineBreakInCode()
      .after(Types.FIELD_SELECTION_ELEMENT).lineBreakInCode()
      .after(Types.ENUM_SYMBOL_DECLARATION).lineBreakInCode()

      // Prevents 'union[ a, b]' when aliases are automatically removed
      .afterInside(Types.OPEN_BRACKET, Types.UNION_TYPE_ASSIGNMENTS).spaceIf(false)

      // Prevents 'union[A:int]' when aliases are automatically added
      .between(Types.UNION_MEMBER_ALIAS, Types.TYPE_ASSIGNMENT).spaceIf(true)

      .after(Types.LINE_COMMENT).lineBreakInCode()
      .after(Types.BLOCK_COMMENT).lineBreakInCode();
  }

  @Nullable
  @Override
  public TextRange getRangeAffectingIndent(PsiFile file, int offset, ASTNode elementAtOffset) {
    return null;
  }
}
