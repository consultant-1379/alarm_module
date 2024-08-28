package com.ericsson.eniq.alarmcfg.clientapi.exceptions;

/**
 * This is the superclass of all BO API exceptions.
 * 
 * @author eheijun
 * @copyright Ericsson (c) 2009
 */
public class ACException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -7528202546992334170L;

  public ACException() {
    super();
  }

  public ACException(final String message) {
    super(message);
  }

  public ACException(final Throwable throwable) {
    super(throwable);
  }

  public ACException(final String message, final Throwable throwable) {
    super(message, throwable);
  }
}
