/**
 * 
 */
package com.ericsson.eniq.alarmcfg.clientapi.exceptions;

/**
 * Authentication exception. Occurs if there is a problem with authentication.
 * 
 * @author eheijun
 * @copyright Ericsson (c) 2009
 */
public class ACAuthenticateException extends ACException {

  /**
   * 
   */
  private static final long serialVersionUID = 4276390701096050622L;

  /**
	 * 
	 */
  public ACAuthenticateException() {
    super();
  }

  /**
   * @param message
   */
  public ACAuthenticateException(final String message) {
    super(message);
  }

  /**
   * @param throwable
   */
  public ACAuthenticateException(final Throwable throwable) {
    super(throwable);
  }

  /**
   * @param message
   * @param throwable
   */
  public ACAuthenticateException(final String message, final Throwable throwable) {
    super(message, throwable);
  }
}
