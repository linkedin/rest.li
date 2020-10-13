package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.linkedin.intellij.pegasusplugin.PdlLanguage;
import com.linkedin.intellij.pegasusplugin.psi.impl.PdlTypeNameDeclarationImpl;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

public class PdlTypeDeclarationStubType extends IStubElementType<PdlTypeNameDeclarationStub, PdlTypeNameDeclaration> {
  public PdlTypeDeclarationStubType(String debugName) {
    super(debugName, PdlLanguage.INSTANCE);
  }

  @Override
  public PdlTypeNameDeclaration createPsi(@NotNull PdlTypeNameDeclarationStub stub) {
    return new PdlTypeNameDeclarationImpl(stub, this);
  }

  @Override
  public PdlTypeNameDeclarationStub createStub(@NotNull PdlTypeNameDeclaration psi, StubElement parentStub) {
    return new PdlTypeNameDeclarationStubImpl(parentStub, psi.getFullname());
  }

  @NotNull
  @Override
  public String getExternalId() {
    return "pdl.typedecl";
  }

  @Override
  public void serialize(@NotNull PdlTypeNameDeclarationStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getFullname().toString());
  }

  @NotNull
  @Override
  public PdlTypeNameDeclarationStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    return new PdlTypeNameDeclarationStubImpl(parentStub, new PdlTypeName(dataStream.readName().getString()));
  }

  @Override
  public void indexStub(@NotNull PdlTypeNameDeclarationStub stub, @NotNull IndexSink sink) {
    sink.occurrence(PdlNameStubIndex.KEY, stub.getFullname().getName());
    sink.occurrence(PdlFullnameStubIndex.KEY, stub.getFullname().toString());
  }
}
