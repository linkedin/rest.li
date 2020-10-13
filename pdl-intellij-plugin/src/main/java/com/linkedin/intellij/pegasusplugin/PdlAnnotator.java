package com.linkedin.intellij.pegasusplugin;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.linkedin.intellij.pegasusplugin.codestyle.PdlSyntaxHighlighter;
import com.linkedin.intellij.pegasusplugin.pdsc.PdscReferenceUtils;
import com.linkedin.intellij.pegasusplugin.psi.PdlEnumSymbol;
import com.linkedin.intellij.pegasusplugin.psi.PdlFieldName;
import com.linkedin.intellij.pegasusplugin.psi.PdlImportDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlNamespaceDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlPropNameDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeName;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeNameDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeReference;
import com.linkedin.intellij.pegasusplugin.psi.PdlUnionMemberAliasName;
import com.linkedin.intellij.pegasusplugin.psi.PdlVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.linkedin.intellij.pegasusplugin.messages.PdlPluginMessages.*;


/**
 * Add highlighting to PDL source beyond what the syntax highlighter, which works off only lexemes, can highlight.
 * e.g.: Highlight the 'identifier' lexeme differently if it is a record field name.
 */
public class PdlAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull final PsiElement element, @NotNull final AnnotationHolder holder) {
    element.accept(new AnnotatorHighlighterVisitor(holder));
  }

  private static class AnnotatorHighlighterVisitor extends PdlVisitor {
    final AnnotationHolder _holder;

    public AnnotatorHighlighterVisitor(AnnotationHolder holder) {
      this._holder = holder;
    }

    @Override
    public void visitFieldName(@NotNull PdlFieldName fieldName) {
      super.visitFieldName(fieldName);
      setHighlighting(fieldName, _holder, PdlSyntaxHighlighter.FIELD);
    }

    @Override
    public void visitTypeNameDeclaration(@NotNull PdlTypeNameDeclaration typeNameDeclaration) {
      super.visitTypeNameDeclaration(typeNameDeclaration);
      setHighlighting(typeNameDeclaration, _holder, PdlSyntaxHighlighter.TYPE_NAME);
      if (typeNameDeclaration.isDeprecated()) {
        setHighlighting(typeNameDeclaration, _holder, PdlSyntaxHighlighter.DEPRECATED,
            message("annotations.resolve.type.deprecated", typeNameDeclaration.getFullname().toString()));
      }
    }

    @Override
    public void visitTypeReference(@NotNull PdlTypeReference typeReference) {
      super.visitTypeReference(typeReference);
      setHighlighting(typeReference, _holder, PdlSyntaxHighlighter.TYPE_REFERENCE);
      PsiElement decl = PdlIdentifierResolver.findTypeDeclaration(typeReference.getProject(),
          typeReference.getPdlFile(),
          typeReference.getFullname());
      if (decl == null) {
        if (!typeReference.getFullname().isPrimitive()) {
          setErrorHighlighting(typeReference, _holder, PdlSyntaxHighlighter.UNRESOLVED_REFERENCE,
              message("annotations.resolve.type.error", typeReference.getFullyQualifiedName().getText()));
        }
      } else if (decl instanceof PdlTypeNameDeclaration && ((PdlTypeNameDeclaration) decl).isDeprecated()) {
        String mesage = message("annotations.resolve.type.deprecated", typeReference.getFullyQualifiedName().getText());
        setHighlighting(typeReference, _holder, PdlSyntaxHighlighter.DEPRECATED, mesage);
        if (typeReference.getParent() != null) {
          _holder.createWarningAnnotation(typeReference.getParent(), mesage);
        }
      }
    }

    @Override
    public void visitPropNameDeclaration(@NotNull PdlPropNameDeclaration propNameDeclaration) {
      super.visitPropNameDeclaration(propNameDeclaration);
      setHighlighting(propNameDeclaration, _holder, PdlSyntaxHighlighter.PROPERTY);
    }

    @Override
    public void visitEnumSymbol(@NotNull PdlEnumSymbol enumSymbol) {
      super.visitEnumSymbol(enumSymbol);
      setHighlighting(enumSymbol, _holder, PdlSyntaxHighlighter.FIELD);
      if (enumSymbol.isDeprecated()) {
        setHighlighting(enumSymbol, _holder, PdlSyntaxHighlighter.DEPRECATED,
            message("annotations.resolve.type.deprecated", enumSymbol.getText()));
      }
    }

    @Override
    public void visitUnionMemberAliasName(@NotNull PdlUnionMemberAliasName unionMemberAliasName) {
      super.visitUnionMemberAliasName(unionMemberAliasName);
      setHighlighting(unionMemberAliasName, _holder, PdlSyntaxHighlighter.FIELD);
    }

    @Override
    public void visitImportDeclaration(@NotNull PdlImportDeclaration importDeclaration) {
      super.visitImportDeclaration(importDeclaration);
      // Show an error if this type is not resolvable from this file
      if (!PdlIdentifierResolver.isResolvableTypeDeclaration(importDeclaration.getPdlFile(), importDeclaration.getFullname())) {
        // TODO: scope error on import to first namespace part that does not match
        setErrorHighlighting(importDeclaration.getFullyQualifiedName(), _holder, PdlSyntaxHighlighter.UNRESOLVED_IMPORT,
            message("annotations.resolve.import.error", importDeclaration.getFullyQualifiedName().getText()));
      }
    }

    @Override
    public void visitNamespaceDeclaration(@NotNull PdlNamespaceDeclaration namespaceDeclaration) {
      super.visitNamespaceDeclaration(namespaceDeclaration);
      PdlPsiFile file = (PdlPsiFile) namespaceDeclaration.getContainingFile();
      // only inspect the top level namespace
      if (file.getNamespace() == namespaceDeclaration.getNamespace()) {
        PsiDirectory pdlFileDirectory = file.getContainingDirectory();
        PsiPackage sourceSetPackage = JavaDirectoryService.getInstance().getPackage(pdlFileDirectory);
        if (sourceSetPackage != null) {
          String declaredNamespace = PdlTypeName.escape(namespaceDeclaration.getNamespace().getText());
          String sourceSetNamespace = sourceSetPackage.getQualifiedName();

          if (PdscReferenceUtils.isJarFile(pdlFileDirectory.getVirtualFile())) {
            sourceSetNamespace = sourceSetNamespace.replaceFirst("pegasus\\.", "");
          }
          if (!declaredNamespace.equals(sourceSetNamespace)) {
            setErrorHighlighting(namespaceDeclaration.getNamespace(), _holder, PdlSyntaxHighlighter.NAMESPACE_MISMATCH,
                message("annotations.namespace.mismatch.error", declaredNamespace, sourceSetNamespace));
          }
        } else {
          // No source set, can't verify if namespace matches containing directories.
          setHighlighting(namespaceDeclaration.getNamespace(), _holder, PdlSyntaxHighlighter.NAMESPACE_UNABLE_TO_VALIDATE, message(
              "annotations.sourceset.unset.warning"));
        }
      }
    }
  }

  private static void setHighlighting(@NotNull PsiElement element, @NotNull AnnotationHolder holder,
      @NotNull TextAttributesKey key) {
    setHighlighting(element, holder, key, null);
  }

  private static void setHighlighting(@NotNull PsiElement element, @NotNull AnnotationHolder holder,
                                      @NotNull TextAttributesKey key, @Nullable  String message) {
    Annotation annotation = holder.createInfoAnnotation(element, message);
    annotation.setTextAttributes(key);
  }

  private static void setErrorHighlighting(@NotNull PsiElement element, @NotNull AnnotationHolder holder,
      @NotNull TextAttributesKey key, String message) {
    Annotation annotation = holder.createErrorAnnotation(element, message);
    annotation.setTextAttributes(key);
  }
}
