package com.linkedin.restli.examples.custom.types;

import com.linkedin.data.ByteString;
import com.linkedin.data.Data;
import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DirectCoercer;
import com.linkedin.data.template.TemplateOutputCastException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;

/**
 * Test class for custom coercing
 *
 * @author Soojung Ha
 */
@SuppressWarnings("rawtypes")
public class IPAddressSimpleCoercer implements DirectCoercer<InetAddress>
{
  private static final Object REGISTER_COERCER = Custom.registerCoercer(new IPAddressSimpleCoercer(), InetAddress.class);

  @Override
  public ByteString coerceInput(InetAddress address) throws ClassCastException
  {
    byte[] addressBytes = address.getAddress();
    return ByteString.copy(addressBytes);
  }

  @Override
  public InetAddress coerceOutput(Object object) throws TemplateOutputCastException
  {
    try
    {
      byte[] addressBytes;
      Class<?> objectType = object.getClass();
      if (objectType == String.class)
      {
        addressBytes = Data.stringToBytes((String) object, true);
      }
      else if (objectType == ByteString.class)
      {
        addressBytes = ((ByteString) object).copyBytes();
      }
      else
      {
        throw new TemplateOutputCastException("Invalid type");
      }
      return InetAddress.getByAddress(addressBytes);
    }
    catch (UnknownHostException e)
    {
      throw new TemplateOutputCastException("Invalid host", e);
    }
  }
}
