package com.ericsson.eniq.alarmcfg.clientapi.exceptions;

/**
 * This exception is thrown when requested report is not found. For example it
 * could be misspelled or non existant.
 * 
 * @author eheijun
 * @copyright Ericsson (c) 2009
 */
public class ACReportNotFoundException extends ACException {

  /**
   * 
   */
  private static final long serialVersionUID = -7252807507443663176L;

  public ACReportNotFoundException() {
    super();
  }

  public ACReportNotFoundException(final String message) {
    super(message);
  }

  public ACReportNotFoundException(final Throwable throwable) {
    super(throwable);
  }

  public ACReportNotFoundException(final String message, final Throwable throwable) {
    super(message, throwable);
  }
}
