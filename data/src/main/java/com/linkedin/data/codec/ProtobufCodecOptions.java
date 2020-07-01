/*
   Copyright (c) 2020 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.data.codec;

import com.linkedin.data.codec.symbol.EmptySymbolTable;
import com.linkedin.data.codec.symbol.SymbolTable;


/**
 * Encapsulates options to configure the behavior of {@link ProtobufDataCodec}
 *
 * @author kramgopa
 */
public class ProtobufCodecOptions
{
  /**
   * The symbol table to use for serialization and deserialization.
   *
   * <p>Set to {@link EmptySymbolTable#SHARED}, if unspecified.</p>
   */
  private final SymbolTable _symbolTable;

  /**
   * If true, then ASCII only strings are detected and encoded using a different ordinal. This serves as an
   * indication to decoders to use an optimized code path for decoding such strings without having to account
   * for multi-byte characters.
   *
   * <p>Disabled by default.</p>
   */
  private final boolean _enableASCIIOnlyStrings;

  /**
   * If true, then fixed length encoding is used for float and double values. If false, then variable length encoding
   * is used. In order to maintain wire protocol semantics, different marker ordinals are used for floats and doubles
   * depending on whether this is enabled or not.
   *
   * <p>This should be enabled ONLY when the payload comprises of a large number of high precision floating point
   * numbers. In all other cases variable length encoding will result in a more compact payload with better
   * performance.</p>
   *
   * <p>Disabled by default.</p>
   */
  private final boolean _enableFixedLengthFloatDoubles;

  /**
   * If true, then tolerates invalid surrogate pairs when serializing strings to UTF-8 bytes. Invalid characters are
   * replaced with the default replacement character. If false, then an exception is thrown when encountering such
   * sequences.
   *
   * <p>Enabled by default.</p>
   */
  private final boolean _shouldTolerateInvalidSurrogatePairs;

  private ProtobufCodecOptions(SymbolTable symbolTable,
                               boolean enableASCIIOnlyStrings,
                               boolean enableFixedLengthFloatDoubles,
                               boolean tolerateInvalidSurrogatePairs)
  {
    _symbolTable = symbolTable == null ? EmptySymbolTable.SHARED : symbolTable;
    _enableASCIIOnlyStrings = enableASCIIOnlyStrings;
    _enableFixedLengthFloatDoubles = enableFixedLengthFloatDoubles;
    _shouldTolerateInvalidSurrogatePairs = tolerateInvalidSurrogatePairs;
  }

  /**
   * @return The symbol table to use for serialization and deserialization.
   */
  public SymbolTable getSymbolTable()
  {
    return _symbolTable;
  }

  /**
   * @return If ASCII only strings should be detected and encoded using a different ordinal.
   */
  public boolean shouldEnableASCIIOnlyStrings()
  {
    return _enableASCIIOnlyStrings;
  }

  /**
   * @return True if floats and doubles shoukd be encoded as fixed length integers, false if they should be
   * encoded as variable length integers.
   */
  public boolean shouldEnableFixedLengthFloatDoubles()
  {
    return _enableFixedLengthFloatDoubles;
  }

  /**
   * @return True if invalid surrogate pairs should be tolerated when serializing strings to UTF-8, with all invalid
   * occurences replaced with the default replacement character. If false, then an exception will be thrown when
   * encountering such data.
   */
  public boolean shouldTolerateInvalidSurrogatePairs() {
    return _shouldTolerateInvalidSurrogatePairs;
  }

  /**
   * Builder to incrementally build options.
   */
  public static final class Builder
  {

    /**
     * The symbol table to use for serialization and deserialization.
     *
     * <p>Default value is null.</p>
     */
    private SymbolTable _symbolTable;

    /**
     * If true, then ASCII only strings are detected and encoded using a different ordinal. This serves as an
     * indication to decoders to use an optimized code path for decoding such strings without having to account
     * for multi-byte characters.
     *
     * <p>Disabled by default.</p>
     */
    private boolean _enableASCIIOnlyStrings;

    /**
     * If true, then fixed length encoding is used for float and double values. If false, then variable length encoding
     * is used. In order to maintain wire protocol semantics, different marker ordinals are used for floats and doubles
     * depending on whether this is enabled or not.
     *
     * <p>This should be enabled ONLY when the payload comprises of a large number of high precision floating point
     * numbers. In all other cases variable length encoding will result in a more compact payload with better
     * performance.</p>
     *
     * <p>Disabled by default.</p>
     */
    private boolean _enableFixedLengthFloatDoubles;

    /**
     * If true, then tolerates invalid surrogate pairs when serializing strings to UTF-8 bytes. Invalid characters are
     * replaced with the default replacement character. If false, then an exception is thrown when encountering such
     * sequences.
     *
     * <p>Enabled by default.</p>
     */
    private boolean _shouldTolerateInvalidSurrogatePairs;

    public Builder()
    {
      _symbolTable = null;
      _enableASCIIOnlyStrings = false;
      _enableFixedLengthFloatDoubles = false;
      _shouldTolerateInvalidSurrogatePairs = true;
    }

    /**
     * Sets the symbol table to use for serialization and deserialization.
     */
    public Builder setSymbolTable(SymbolTable symbolTable)
    {
      this._symbolTable = symbolTable;
      return this;
    }

    /**
     * If set to true, then ASCII only strings are detected and encoded using a different ordinal. This serves as an
     * indication to decoders to use an optimized code path for decoding such strings without having to account
     * for multi-byte characters.
     */
    public Builder setEnableASCIIOnlyStrings(boolean enableASCIIOnlyStrings)
    {
      this._enableASCIIOnlyStrings = enableASCIIOnlyStrings;
      return this;
    }

    /**
     * If set to true, then fixed length encoding is used for float and double values. If false, then variable length encoding
     * is used. In order to maintain wire protocol semantics, different marker ordinals are used for floats and doubles
     * depending on whether this is enabled or not.
     *
     * <p>This should be enabled ONLY when the payload comprises of a large number of high precision floating point
     * numbers. In all other cases variable length encoding will result in a more compact payload with better
     * performance.</p>
     */
    public Builder setEnableFixedLengthFloatDoubles(boolean enableFixedLengthFloatDoubles)
    {
      this._enableFixedLengthFloatDoubles = enableFixedLengthFloatDoubles;
      return this;
    }

    /**
     * If true, then tolerates invalid surrogate pairs when serializing strings to UTF-8 bytes. Invalid characters are
     * replaced with the default replacement character. If false, then an exception is thrown when encountering such
     * sequences.
     */
    public Builder setShouldTolerateInvalidSurrogatePairs(boolean tolerateInvalidSurrogatePairs)
    {
      this._shouldTolerateInvalidSurrogatePairs = tolerateInvalidSurrogatePairs;
      return this;
    }

    /**
     * Build an options instance.
     */
    public ProtobufCodecOptions build()
    {
      return new ProtobufCodecOptions(_symbolTable,
          _enableASCIIOnlyStrings,
          _enableFixedLengthFloatDoubles,
          _shouldTolerateInvalidSurrogatePairs);
    }
  }
}
