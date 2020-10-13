package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.linkedin.intellij.pegasusplugin.PdlFileType;
import com.linkedin.intellij.pegasusplugin.PdlLanguage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Models a .pdl file. Provides utilities for accessing the namespace for managing imports.
 */
public class PdlPsiFile extends PsiFileBase {
  public PdlPsiFile(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, PdlLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public com.intellij.openapi.fileTypes.FileType getFileType() {
    return PdlFileType.INSTANCE;
  }

  @Override
  public String toString() {
    return String.format("PdlPsiFile(%s)", getName());
  }

  @Override
  public Icon getIcon(int flags) {
    return super.getIcon(flags);
  }

  public PdlTypeNameDeclaration getTopLevelTypeDeclaration() {
    PdlTopLevel root = getRoot();
    if (root == null) {
      return null;
    }

    PdlTypeDeclaration typeDeclaration = root.getTypeDeclaration();
    if (typeDeclaration == null) {
      return null;
    }

    PdlNamedTypeDeclaration namedTypeDeclaration = typeDeclaration.getNamedTypeDeclaration();
    if (namedTypeDeclaration == null) {
      return null;
    }

    return namedTypeDeclaration.getTypeNameDeclaration();
  }

  @NotNull
  public Collection<PdlTypeReference> getTypeReferences() {
    return PsiTreeUtil.findChildrenOfType(this, PdlTypeReference.class);
  }

  // TODO: if the namespace is absent, it should implicitly be ""
  @Nullable
  public PdlNamespace getNamespace() {
    PdlTopLevel root = getRoot();

    if (root == null) {
      return null;
    }

    PdlNamespaceDeclaration namespaceDeclaration = root.getNamespaceDeclaration();

    if (namespaceDeclaration == null) {
      return null;
    }

    return namespaceDeclaration.getNamespace();
  }

  @NotNull
  public String getNamespaceText() {
    PdlNamespace namespace = getNamespace();

    if (namespace == null) {
      return "";
    }

    String namespaceText = namespace.getText();

    if (namespaceText == null) {
      return "";
    }

    return namespaceText;
  }

  /**
   * Add an import declaration for a given type name to this file.
   * @param typeName type name of the import
   */
  public void addImport(@NotNull PdlTypeName typeName) {
    addImport(PdlElementFactory.createImport(getProject(), typeName));
  }

  /**
   * Add an import declaration to this file.
   * @param importDecl import declaration to add
   */
  public void addImport(PdlImportDeclaration importDecl) {
    PdlTopLevel root = getRoot();
    PdlImportDeclarations importDecls = root.getImportDeclarations();
    if (importDecls.getImportDeclarationList().size() == 0) {
      importDecls = addFirstImport(importDecl);
    } else {
      importDecls = addNthImport(importDecl);
    }
    CodeStyleManager.getInstance(importDecls.getProject()).reformat(importDecls);
  }

  public PdlTopLevel getRoot() {
    return PsiTreeUtil.findChildOfType(this, PdlTopLevel.class);
  }

  // TODO: Look into using ImportHelper to replace the below functionality.
  // Modifying the grammar to include a ImportList ast node looks helpful, and necessary to leverage the helper.

  /**
   * Position imports correctly when adding the first import.
   */
  private PdlImportDeclarations addFirstImport(PdlImportDeclaration importDecl) {
    PdlTypeName first = importDecl.getFullname();
    PdlTopLevel root = getRoot();
    PdlImportDeclarations importDecls = root.getImportDeclarations();
    if (importDecls.getImportDeclarationList().size() == 0) {
      importDecls.delete();
      PdlImportDeclarations emptyImports =
        PdlElementFactory.createImports(getProject(), Collections.singleton(first));

      PdlPackageDeclaration packageDecl = root.getPackageDeclaration();
      PdlNamespaceDeclaration namespaceDecl = root.getNamespaceDeclaration();
      if (packageDecl != null) {
        root.addAfter(emptyImports, packageDecl);
      } else if (namespaceDecl != null) {
        root.addAfter(emptyImports, namespaceDecl);
      } else {
        PsiElement firstChild = root.getFirstChild();
        if (firstChild != null) {
          root.addBefore(emptyImports, firstChild);
        } else {
          root.add(emptyImports);
        }
      }
      return emptyImports;
    } else {
      return importDecls;
    }
  }

  /**
   * Add an import to an existing import list, attempting to keep the list sorted.
   *
   * Note: this does not change the sort order of the existing import list.  It only attempts
   * to keep the list sorted if it already is by adding the import at the correct position.
   *
   */
  private PdlImportDeclarations addNthImport(PdlImportDeclaration importDecl) {
    PdlTopLevel root = getRoot();
    PdlImportDeclarations importDecls = root.getImportDeclarations();
    if (importDecl.getFullname() == null) {
      return importDecls;
    }

    boolean added = false;
    for (PdlImportDeclaration existing : importDecls.getImportDeclarationList()) {
      if (existing.getFullname() != null && importDecl.getFullname().toString().compareTo(existing.getFullname().toString()) < 0) {
        importDecls.addBefore(importDecl, existing);
        added = true;
        break;
      }
    }
    if (!added) {
      importDecls.add(importDecl);
    }
    return importDecls;
  }

  public Collection<PdlImportDeclaration> getImports() {
    return PsiTreeUtil.findChildrenOfType(getContainingFile(), PdlImportDeclaration.class);
  }

  public void optimizeImports() {
    PdlImportDeclarations importGroup = PsiTreeUtil.findChildOfType(getContainingFile(), PdlImportDeclarations.class);
    if (importGroup != null) {
      List<PdlImportDeclaration> importDecls = importGroup.getImportDeclarationList();
      if (importDecls.size() > 0) {
        for (PdlImportDeclaration importDecl : importDecls) {
          if (!importDecl.isUsed()) {
            importDecl.delete();
          }
        }

        List<PdlImportDeclaration> remaining = importGroup.getImportDeclarationList();
        if (remaining.size() > 0) {
          List<PdlTypeName> typeNames = new ArrayList<>();
          for (PdlImportDeclaration importDecl: remaining) {
            typeNames.add(importDecl.getFullname());
          }
          Collections.sort(typeNames);
          importGroup.replace(PdlElementFactory.createImports(getProject(), typeNames));
        }
      }
    }
  }

  public PdlTypeName lookupImport(String name) {
    Collection<PdlImportDeclaration> importDecls = getImports();
    for (PdlImportDeclaration importDecl : importDecls) {
      String importName = importDecl.getName();
      if (importName != null && importName.equals(name)) {
        return importDecl.getFullname();
      }
    }
    return null;
  }

  /**
   * Determines if two file objects represent the same logical file, even if they aren't the same instance.
   * This is useful in certain instances where duplicate files temporarily exist, such as during completion.
   * @param otherFile other file
   * @return whether these files represent the same file
   */
  public boolean isSameFile(@Nullable PdlPsiFile otherFile) {
    return otherFile != null && getOriginalFile() == otherFile.getOriginalFile();
  }
}
