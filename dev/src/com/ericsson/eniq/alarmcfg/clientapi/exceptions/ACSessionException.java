package com.ericsson.eniq.alarmcfg.clientapi.exceptions;

/**
 * This exception is thrown when connection to Alarmcfg cannot established.
 * 
 * @author eheijun
 * @copyright Ericsson (c) 2009
 */
public class ACSessionException extends ACException {

  /**
   * 
   */
  private static final long serialVersionUID = 8304525361653022213L;

  public ACSessionException() {
    super();
  }

  public ACSessionException(final String message) {
    super(message);
  }

  public ACSessionException(final Throwable throwable) {
    super(throwable);
  }

  public ACSessionException(final String message, final Throwable throwable) {
    super(message, throwable);
  }
}
