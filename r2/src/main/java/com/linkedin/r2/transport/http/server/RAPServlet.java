package com.linkedin.r2.transport.http.server;

import com.linkedin.r2.transport.common.bridge.server.TransportDispatcher;

/**
 * As RAPServlet has been renamed R2Servlet, adding this class for backward compatibility, to be removed later.
 * @author adubman
 *
 */
@Deprecated
public class RAPServlet extends R2Servlet
{
  private static final long serialVersionUID = 1L;

  public RAPServlet(HttpDispatcher dispatcher)
  {
    super(dispatcher);
  }

  public RAPServlet(TransportDispatcher dispatcher)
  {
    super(dispatcher);
  }

  /**
   * Initialize the R2Servlet.
   * @see AbstractR2Servlet#AbstractR2Servlet(boolean, int)
   */
  public RAPServlet(HttpDispatcher dispatcher,
                    boolean useAsync,
                    int timeOut)
  {
    super(dispatcher, useAsync, timeOut);
  }

  /**
   * Initialize the R2Servlet.
   * @see AbstractR2Servlet#AbstractR2Servlet(boolean, int)
   */
  @Deprecated
  public RAPServlet(TransportDispatcher dispatcher,
                    boolean useAsync,
                    int timeOut,
                    int timeOutDelta)
  {
    super(dispatcher, useAsync, timeOut);
  }

  /**
   * Initialize the R2Servlet.
   * @see AbstractR2Servlet#AbstractR2Servlet(boolean, int)
   */
  public RAPServlet(TransportDispatcher dispatcher,
                    boolean useAsync,
                    int timeOut)
  {
    super(dispatcher, useAsync, timeOut);
  }
}
