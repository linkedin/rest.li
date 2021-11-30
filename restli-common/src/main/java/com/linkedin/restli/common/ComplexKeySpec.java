/*
 Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.common;


import com.linkedin.data.template.RecordTemplate;


/**
 * Runtime representation of a rest.li complexKey within the context of a ResourceSpec.
 *
 * @author jbetz@linkedin.com
 */
public class ComplexKeySpec<KK extends RecordTemplate, KP extends RecordTemplate>
{
  /**
   * If the provided keyKeyClass is null, null is returned, otherwise the type is wrapped as a ComplexKeySpec.
   * The keyParamsClass is optional, if null, a  ComplexKeySpec is returned with the TypeSpec for the params left as null.
   * @param keyKeyClass A class, or null.
   * @param keyParamsClass A class, or null.
   * @param <KK> type of the keyKeyClass
   * @param <KP> type of the keyParamsClass
   * @return a ComplexKeySpec or null.
   */
  public static <KK extends RecordTemplate, KP extends RecordTemplate> ComplexKeySpec<KK, KP> forClassesMaybeNull(Class<KK> keyKeyClass,
                                                                                                                  Class<KP> keyParamsClass)
  {
    if(keyKeyClass == null)
    {
      return null;
    }
    else
    {
      if(keyParamsClass == null) throw new IllegalArgumentException("keyParamsClass must be non-null.");
      return new ComplexKeySpec<>(new TypeSpec<>(keyKeyClass), new TypeSpec<>(keyParamsClass));
    }
  }

  private final TypeSpec<KK> _keyType;
  private final TypeSpec<KP> _paramsType;

  public ComplexKeySpec(TypeSpec<KK> keyType, TypeSpec<KP> paramsType)
  {
    if(keyType == null) throw new IllegalArgumentException("keyType must be non-null.");
    if(paramsType == null) throw new IllegalArgumentException("paramsType must be non-null.");
    _keyType = keyType;
    _paramsType = paramsType;
  }

  public TypeSpec<KK> getKeyType()
  {
    return _keyType;
  }

  public TypeSpec<KP> getParamsType()
  {
    return _paramsType;
  }

  @Override
  public String toString()
  {
    return "ComplexKeySpec{" +
        "\n  _keyType=" + _keyType +
        ",\n  _paramsType=" + _paramsType +
        "\n}";
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean equals(Object o)
  {
    if (this == o) return true;
    if (!(o instanceof ComplexKeySpec)) return false;
    ComplexKeySpec<KK, KP> that = (ComplexKeySpec<KK, KP>) o;
    if (!_keyType.equals(that._keyType)) return false;
    if (_paramsType != null ? !_paramsType.equals(that._paramsType) : that._paramsType != null) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = _keyType.hashCode();
    result = 31 * result + (_paramsType != null ? _paramsType.hashCode() : 0);
    return result;
  }
}
