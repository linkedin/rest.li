package com.linkedin.pegasus.gradle;

public enum SchemaFileType {
  PDSC("pdsc"),
  PDL("pdl");

  SchemaFileType(String fileExtension) {
    _fileExtension = fileExtension;
  }

  public final String _fileExtension;

  public String getFileExtension() {
    return _fileExtension;
  }
}
