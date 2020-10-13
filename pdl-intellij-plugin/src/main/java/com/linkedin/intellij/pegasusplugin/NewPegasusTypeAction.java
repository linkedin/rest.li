package com.linkedin.intellij.pegasusplugin;

import com.intellij.ide.actions.CreateFileFromTemplateDialog;
import com.intellij.ide.actions.CreateTemplateInPackageAction;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayFactory;
import com.intellij.util.IncorrectOperationException;
import com.linkedin.intellij.pegasusplugin.psi.PdlEnumDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlNamedTypeDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import com.linkedin.intellij.pegasusplugin.psi.PdlRecordDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlTopLevel;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeAssignment;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeReference;
import com.linkedin.intellij.pegasusplugin.psi.PdlUnionDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.Types;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaResourceRootType;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import static com.linkedin.intellij.pegasusplugin.messages.PdlPluginMessages.*;


/**
 * Action to create .pdl schema files from the templates in src/main/resources/fileTemplates/internal.
 *
 * The action creates a dialog asking for the name and type of the schema to create.
 * The namespace is inferred from the relative path from the nearest 'sources root' parent directory.
 */
public class NewPegasusTypeAction extends CreateTemplateInPackageAction<PdlTopLevel> {
  private static final Logger LOG = Logger.getInstance(NewPegasusTypeAction.class);
  private static final Set<? extends JpsModuleSourceRootType<?>> ALL_SOURCE_TYPES = new HashSet<>(Arrays.asList(
      JavaSourceRootType.SOURCE, JavaSourceRootType.TEST_SOURCE,
      JavaResourceRootType.RESOURCE, JavaResourceRootType.TEST_RESOURCE));

  public NewPegasusTypeAction() {
    super(message("newtype.action.title"), message("newtype.action.description"), PdlIcons.FILE, ALL_SOURCE_TYPES);
  }

  @Override
  protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
    builder
      .setTitle(message("newtype.build_dialog.title"))
      .addKind(message("newtype.build_dialog.option.record"), PdlIcons.FILE, "Record.pdl")
      .addKind(message("newtype.build_dialog.option.enum"), PdlIcons.FILE, "Enum.pdl")
      .addKind(message("newtype.build_dialog.option.union"), PdlIcons.FILE, "Union.pdl")
      .addKind(message("newtype.build_dialog.option.typeref"), PdlIcons.FILE, "Typeref.pdl");
    for (FileTemplate template : FileTemplateManager.getInstance(project).getAllTemplates()) {
      com.intellij.openapi.fileTypes.FileType fileType = FileTypeManagerEx.getInstanceEx().getFileTypeByExtension(template.getExtension());
      if (fileType.equals(PdlFileType.INSTANCE) && JavaDirectoryService.getInstance().getPackage(directory) != null) {
        builder.addKind(template.getName(), PdlIcons.FILE, template.getName());
      }
    }
  }

  @Override
  protected boolean checkPackageExists(PsiDirectory directory) {
    return JavaDirectoryService.getInstance().getPackage(directory) != null;
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName, String templateName) {
    return message("newtype.action.name");
  }

  @Override
  protected PsiElement getNavigationElement(@NotNull PdlTopLevel createdElement) {
    return createdElement;
  }

  @Override
  protected final PdlTopLevel doCreate(
      PsiDirectory dir, String className, String templateName)
      throws IncorrectOperationException {
    try {
      final String fileName = PdlFileType.toFilename(className);
      Project project = dir.getProject();

      FileTemplateManager fileTemplateManager = FileTemplateManager.getInstance(project);
      final FileTemplate fileTemplate = fileTemplateManager.getInternalTemplate(templateName);
      final PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);

      Properties properties = new Properties(fileTemplateManager.getDefaultProperties());
      properties.setProperty("PACKAGE_NAME", getNamespace(dir));
      properties.setProperty("NAME", className);

      PsiFile file =
        psiFileFactory.createFileFromText(
          fileName, PdlFileType.INSTANCE, fileTemplate.getText(properties));

      if (file instanceof PdlPsiFile) {
        PdlPsiFile pdlPsiFile = (PdlPsiFile) dir.add(file);
        CodeStyleManager.getInstance(file.getManager()).reformat(file);
        return pdlPsiFile
          .calcTreeElement()
          .getChildrenAsPsiElements(Types.TOP_LEVEL, _arrayFactory)[0];
      } else {
        throw new IncorrectOperationException(".pdl extension is not mapped to Pdl file type: " + fileTemplate.getClass());
      }
    } catch (IOException e) {
      LOG.debug(e);
      throw new IncorrectOperationException(e);
    }
  }

  public static String getNamespace(@NotNull PsiDirectory directory) {
    PsiPackage ns = JavaDirectoryService.getInstance().getPackage(directory);
    return ns != null ? ns.getQualifiedName() : "UNKNOWN";
  }

  @Override
  protected void postProcess(PdlTopLevel createdElement, String templateName, Map<String, String> customProperties) {
    super.postProcess(createdElement, templateName, customProperties);

    final Project project = createdElement.getProject();
    final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    if (editor != null) {
      PdlNamedTypeDeclaration decl = PsiTreeUtil.findChildOfType(createdElement, PdlNamedTypeDeclaration.class);
      if (decl != null) {
        if (decl.getRecordDeclaration() != null) {
          moveCaretToRecordFields(editor, decl.getRecordDeclaration());
        } else if (decl.getEnumDeclaration() != null) {
          moveCaretToEnumSymbols(editor, decl.getEnumDeclaration());
        } else if (decl.getTyperefDeclaration() != null && decl.getTyperefDeclaration().getTypeAssignment() != null) {
          PdlTypeAssignment assignment = decl.getTyperefDeclaration().getTypeAssignment();
          if (assignment.getTypeDeclaration() != null) {
            // Named unions are declared within typrefs. Since unions ane anonymous, the typeref provides the name.
            moveCaretToTyperefUnionDeclaration(editor, assignment.getTypeDeclaration());
          } else if (assignment.getTypeReference() != null) {
            // For typerefs, the developer will next want to provide an actual referenced type.
            selectTyperefRef(editor, assignment.getTypeReference());
          }
        }
      }
    }
  }

  private void moveCaretToRecordFields(Editor editor, PdlRecordDeclaration record) {
    PsiElement offset = record.getFieldSelection().getFirstChild();
    editor.getCaretModel().moveToOffset(offset.getTextRange().getEndOffset());
  }

  private void moveCaretToEnumSymbols(Editor editor, PdlEnumDeclaration enumeration) {
    PsiElement offset = enumeration.getEnumSymbolDeclarations().getFirstChild();
    editor.getCaretModel().moveToOffset(offset.getTextRange().getEndOffset());
  }

  private void moveCaretToTyperefUnionDeclaration(Editor editor, PdlTypeDeclaration typeref) {
    PdlUnionDeclaration unionDecl = PsiTreeUtil.findChildOfType(typeref, PdlUnionDeclaration.class);
    if (unionDecl != null) {
      PsiElement offset = unionDecl.getUnionTypeAssignments().getFirstChild();
      editor.getCaretModel().moveToOffset(offset.getTextRange().getEndOffset());
    }
  }

  private void selectTyperefRef(Editor editor, PdlTypeReference refType) {
    TextRange range = refType.getTextRange();
    editor.getCaretModel().moveToOffset(range.getStartOffset());
    editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
  }

  private static ArrayFactory<PdlTopLevel> _arrayFactory = count -> new PdlTopLevel[count];
}
