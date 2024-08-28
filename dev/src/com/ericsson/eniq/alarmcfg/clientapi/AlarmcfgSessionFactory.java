package com.ericsson.eniq.alarmcfg.clientapi;

import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACAuthenticateException;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACSessionException;

/**
 * Factory class which creates session to the alarmcfg
 * 
 * @author eheijun
 * @copyright Ericsson (c) 2009
 */
public class AlarmcfgSessionFactory {
  
  private static IAlarmcfgSession nonDefaultSession;

  /**
   * Do NOT instantiate this.
   */
  private AlarmcfgSessionFactory() {
  }

  /**
   * Returns the session to the alarmcfg.
   * 
   * @param host
   *          BO CMS to where to connect, with port number ("localhost:6400")
   * @param username
   *          BO username
   * @param password
   *          BO password
   * @param authmethod
   *          BO authentication method
   * 
   * @return IAlarmcfgSession interface for the session
   * @throws ACAuthenticateException
   * @throws ACSessionException
   */
  public static IAlarmcfgSession createSession(final String host, final String cms, final String username,
      final String password, final String authmethod) throws ACAuthenticateException, ACSessionException {
    if (nonDefaultSession != null) {
      return nonDefaultSession;
    }
    return new DefaultAlarmcfgSession(host, cms, username, password, authmethod);
  }

  /**
   * Returns the session to the Alarmcfg.
   * 
   * @param protocol
   *          HTTP or HTTPS
   * @param host
   *          BO host to where to connect, with port number
   * @param username
   *          BO username
   * @param password
   *          BO password
   * @param authmethod
   *          BO authentication method
   * 
   * @return IAlarmcfgSession interface for the session
   * @throws ACAuthenticateException
   * @throws ACSessionException
   */
  public static IAlarmcfgSession createSession(final String protocol, final String host, final String cms,
      final String username, final String password, final String authmethod) throws ACAuthenticateException,
      ACSessionException {
    if (nonDefaultSession != null) {
      return nonDefaultSession;
    }
    return new DefaultAlarmcfgSession(protocol, host, cms, username, password, authmethod);
  }

  /**
   * Create report request. Pass report name in order to create new report
   * request. Method returns the request object that can be used with connection
   * to retrieve the actual BO document (report).
   * 
   * @param reportName
   *          name of the report as it is in BO repository
   * @return the request object
   */
  public static IAlarmcfgReportRequest createRequest(final String reportName) {
    return new DefaultAlarmcfgReportRequest(reportName);
  }

  /**
   * getter for junit test session 
   * @return
   */
  public static IAlarmcfgSession getNonDefaultSession() {
    return nonDefaultSession;
  }

  /**
   * setter for junit test session
   * @param nonDefaultSession
   */
  public static void setNonDefaultSession(IAlarmcfgSession alarmcfgSession) {
    nonDefaultSession = alarmcfgSession;
  }
  
}
