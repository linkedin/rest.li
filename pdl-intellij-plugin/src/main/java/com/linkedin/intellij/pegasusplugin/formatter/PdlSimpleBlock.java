package com.linkedin.intellij.pegasusplugin.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.formatting.alignment.AlignmentStrategy;
import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.linkedin.intellij.pegasusplugin.psi.Types;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Block for all PDL code. Responsible for indentation.
 */
public class PdlSimpleBlock extends PdlAbstractBlock {
  public PdlSimpleBlock(ASTNode node,
      Wrap wrap,
      Alignment alignment,
      Indent indent,
      CodeStyleSettings settings,
      SpacingBuilder spacingBuilder) {
    super(node, wrap, alignment, indent, settings, spacingBuilder);
  }

  public static final TokenSet BLOCKS_TOKEN_SET = TokenSet.create(
    Types.FIELD_SELECTION,
    Types.ENUM_SYMBOL_DECLARATIONS,
    Types.UNION_TYPE_ASSIGNMENTS,
    Types.PROP_JSON_VALUE,
    Types.ARRAY_TYPE_ASSIGNMENTS,
    Types.MAP_TYPE_ASSIGNMENTS,
    Types.JSON_OBJECT,
    Types.JSON_ARRAY
  );

  public static final TokenSet BRACES_TOKEN_SET = TokenSet.create(
    Types.OPEN_BRACE,
    Types.CLOSE_BRACE,
    Types.OPEN_BRACKET,
    Types.CLOSE_BRACKET,
    Types.OPEN_PAREN,
    Types.CLOSE_PAREN
  );

  @Override
  protected List<Block> buildChildren() {
    final ArrayList<Block> result = new ArrayList<Block>();

    ASTNode child = getNode().getFirstChildNode();
    while (child != null) {
      IElementType childType = child.getElementType();
      if (childType != TokenType.WHITE_SPACE && !FormatterUtil.containsWhiteSpacesOnly(child) && !child.getText().trim().isEmpty()) {
        result.add(
          createBlock(child, calcIndent(child), null, AlignmentStrategy.getNullStrategy(), _settings, _spacingBuilder));
      }

      child = child.getTreeNext();
    }
    return result;
  }

  @NotNull
  private Indent calcIndent(@NotNull ASTNode child) {
    IElementType parentType = myNode.getElementType();
    if (parentType == Types.TOP_LEVEL) {
      return Indent.getAbsoluteNoneIndent();
    }
    if (BLOCKS_TOKEN_SET.contains(parentType)) {
      return indentIfNotBrace(child);
    } else {
      return Indent.getNoneIndent();
    }
  }

  @NotNull
  private static Indent indentIfNotBrace(@NotNull ASTNode child) {
    if (BRACES_TOKEN_SET.contains(child.getElementType())) {
      return Indent.getNoneIndent();
    } else {
      return Indent.getNormalIndent();
    }
  }

  @Nullable
  protected Indent getChildIndent() {
    IElementType parentType = myNode.getElementType(); // always the parent since isIncomplete is false

    if (BLOCKS_TOKEN_SET.contains(parentType)) {
      return Indent.getNormalIndent();
    } else {
      return Indent.getNoneIndent();
    }
  }
}
