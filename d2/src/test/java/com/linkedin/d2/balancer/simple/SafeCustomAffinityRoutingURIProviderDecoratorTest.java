package com.linkedin.d2.balancer.simple;

import com.linkedin.d2.balancer.util.CustomAffinityRoutingURIProvider;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class SafeCustomAffinityRoutingURIProviderDecoratorTest {

  private CustomAffinityRoutingURIProvider _mockDelegate;
  private SafeCustomAffinityRoutingURIProviderDecorator _decorator;

  @BeforeMethod
  public void setUp() {
    _mockDelegate = Mockito.mock(CustomAffinityRoutingURIProvider.class);
    _decorator = new SafeCustomAffinityRoutingURIProviderDecorator(_mockDelegate);
  }

  @Test
  public void testIsEnabledWithNullDelegate() {
    _decorator = new SafeCustomAffinityRoutingURIProviderDecorator(null);
    Assert.assertFalse(_decorator.isEnabled());
  }

  @Test
  public void testIsEnabledWithDelegate() {
    when(_mockDelegate.isEnabled()).thenReturn(true);
    Assert.assertTrue(_decorator.isEnabled());
    verify(_mockDelegate, times(1)).isEnabled();
  }

  @Test
  public void testIsEnabledThrowsException() {
    when(_mockDelegate.isEnabled()).thenThrow(new RuntimeException("Mock exception"));
    Assert.assertFalse(_decorator.isEnabled());
    verify(_mockDelegate, times(1)).isEnabled();
  }

  @Test
  public void testGetTargetHostURIWithNullDelegate() {
    _decorator = new SafeCustomAffinityRoutingURIProviderDecorator(null);
    Assert.assertEquals(_decorator.getTargetHostURI("testCluster"), Optional.empty());
  }

  @Test
  public void testGetTargetHostURIWithDelegate() {
    URI mockURI = URI.create("http://example.com");
    when(_mockDelegate.getTargetHostURI("testCluster")).thenReturn(Optional.of(mockURI));
    Assert.assertEquals(_decorator.getTargetHostURI("testCluster"), Optional.of(mockURI));
    verify(_mockDelegate, times(1)).getTargetHostURI("testCluster");
  }

  @Test
  public void testGetTargetHostURIThrowsException() {
    when(_mockDelegate.getTargetHostURI("testCluster")).thenThrow(new RuntimeException("Mock exception"));
    Assert.assertEquals(_decorator.getTargetHostURI("testCluster"), Optional.empty());
    verify(_mockDelegate, times(1)).getTargetHostURI("testCluster");
  }

  @Test
  public void testSetTargetHostURIWithNullDelegate() {
    _decorator = new SafeCustomAffinityRoutingURIProviderDecorator(null);
    _decorator.setTargetHostURI("testCluster", URI.create("http://example.com"));
    // No exception should be thrown, and no interaction with the delegate
  }

  @Test
  public void testSetTargetHostURIWithDelegate() {
    URI mockURI = URI.create("http://example.com");
    _decorator.setTargetHostURI("testCluster", mockURI);
    verify(_mockDelegate, times(1)).setTargetHostURI("testCluster", mockURI);
  }

  @Test
  public void testSetTargetHostURIThrowsException() {
    doThrow(new RuntimeException("Mock exception")).when(_mockDelegate).setTargetHostURI(anyString(), any(URI.class));
    _decorator.setTargetHostURI("testCluster", URI.create("http://example.com"));
    verify(_mockDelegate, times(1)).setTargetHostURI("testCluster", URI.create("http://example.com"));
  }
}