package com.ericsson.eniq.alarmcfg.clientapi;

import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACAuthenticateException;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACSessionException;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACReportNotFoundException;

/**
 * This is Alarmcfg connection interface. Use this for connecting to ENIQ
 * Alarmcfg and retrieving reports from it. Authentication to the Alarmcfg is
 * done when needed, so user of this class does not need to take care of the
 * authentication.
 * 
 * @author eheijun
 * @copyright Ericsson (c) 2009
 */
public interface IAlarmcfgSession {

  /**
   * To get some specific report from ENIQ Alarmcfg.
   * 
   * @param request
   *          the report that should be retrieved
   */
  IAlarmcfgReport getReport(IAlarmcfgReportRequest request) throws ACSessionException, ACReportNotFoundException,
      ACAuthenticateException;

  /**
   * Close ENIQ Alarmcfg session.
   */
  void close() throws ACSessionException;

}
