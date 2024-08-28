package com.distocraft.dc5000.etl.alarm;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgReport;
import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgReportRequest;
import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgSession;

/**
 * This class is a thread class that contains functionality to handle data
 * change between Eniq and Alarmcfg with Alarmcfg API.
 * 
 * @author ejannbe
 * @author eheijun
 * 
 */
class AlarmcfgSessionThread extends Thread {

  final String action;

  final Logger log;

  final Thread maint;

  final IAlarmcfgSession alarmCfgSession;

  final IAlarmcfgReportRequest alarmCfgReportRequest;

  final StringBuffer xmlText;

  /**
   * Creates thread for alarmcfg session
   * @param action
   * @param maint
   * @param log
   * @param alarmCfgSession
   * @param alarmCfgReportRequest
   * @param xmlText
   */
  AlarmcfgSessionThread(final String action, final Thread maint, final Logger log,
      final IAlarmcfgSession alarmCfgSession, final IAlarmcfgReportRequest alarmCfgReportRequest, final StringBuffer xmlText) {
    this.action = action;
    this.maint = maint;
    this.log = log;
    this.alarmCfgSession = alarmCfgSession;
    this.alarmCfgReportRequest = alarmCfgReportRequest;
    this.xmlText = xmlText;
  }

  /**
   * Gets report form alarmcfg
   */
  public void run() {
    try {
      if (this.action.equalsIgnoreCase("get_report")) {
        this.log.log(Level.FINEST, "AlarmcfgSessionThread trying to get html-source of report named " + alarmCfgReportRequest.getReportName()
            + " from alarmcfg...");
        final IAlarmcfgReport report = alarmCfgSession.getReport(alarmCfgReportRequest);
        this.xmlText.append(report.getXML());
        this.log.log(Level.FINEST, "AlarmcfgSessionThread retrieved report " + alarmCfgReportRequest.getReportName() + " contents successfully.");
        maint.interrupt();
      } else {
        this.log.log(Level.SEVERE, "AlarmcfgSessionThread received unknown action " + action);
      }
    } catch (Exception e) {
      this.log.log(Level.SEVERE, "AlarmcfgSessionThread action " + action + " failed.", e);
      this.xmlText.append("");
      maint.interrupt();
    }
  }
}
