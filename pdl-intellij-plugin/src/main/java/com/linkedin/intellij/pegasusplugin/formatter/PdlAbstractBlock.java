package com.linkedin.intellij.pegasusplugin.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.formatting.alignment.AlignmentStrategy;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import com.linkedin.intellij.pegasusplugin.schemadoc.psi.PsiSchemadocElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Base PSI block type for both grammars this plugin provides: PDL and PDL's "schemadoc" sub-grammar for documentation
 * comment strings.
 */
public abstract class PdlAbstractBlock extends AbstractBlock {
  @NotNull protected final Indent _indent;
  @NotNull protected final CodeStyleSettings _settings;
  @NotNull protected final SpacingBuilder _spacingBuilder;

  public PdlAbstractBlock(ASTNode node,
        Wrap wrap,
        Alignment alignment,
        Indent indent,
        CodeStyleSettings settings,
        SpacingBuilder spacingBuilder) {
    super(node, wrap, alignment);
    this._indent = indent;
    this._settings = settings;
    this._spacingBuilder = spacingBuilder;
  }

  /**
   * Manages block instantiation.
   */
  @NotNull
  public static Block createBlock(@NotNull ASTNode child,
        @Nullable Indent indent,
        Wrap wrap,
        @NotNull AlignmentStrategy alignmentStrategy,
        CodeStyleSettings settings,
        SpacingBuilder spacingBuilder) {
    final IElementType elementType = child.getElementType();
    Alignment alignment = alignmentStrategy.getAlignment(elementType);

    // Delegate to the schemadoc grammar for documentation comments. This is required by IntelliJ to identify
    // documentation comments regions and format them correctly.
    if (child.getPsi() instanceof PsiSchemadocElement) {
      return new SchemadocBlockPdl(child, wrap, alignment, indent, settings, spacingBuilder);
    } else {
      return new PdlSimpleBlock(child, wrap, alignment, indent, settings, spacingBuilder);
    }
  }

  @Override
  public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
    return _spacingBuilder.getSpacing(this, child1, child2);
  }

  @Override
  public Indent getIndent() {
    return _indent;
  }

  @Override
  public boolean isLeaf() {
    return myNode.getFirstChildNode() == null;
  }
}
