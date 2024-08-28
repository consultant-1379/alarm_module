package com.distocraft.dc5000.etl.alarm;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ericsson.eniq.alarmcfg.clientapi.AlarmcfgSessionFactory;
import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgReportRequest;
import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgSession;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACAuthenticateException;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACSessionException;
import com.ericsson.eniq.etl.alarm.CachedAlarmReport;

/**
 * This class is a thread class that contains functionality to download alarm report from the alarmcfg.
 * 
 * @author ejannbe
 * @author eheijun
 */
class AlarmReportDownloadThread extends Thread {

  private final String hostname;

  private final String username;

  private final String password;

  private final long timeout;

  private final Logger log;

  private final CachedAlarmReport targetAlarmReport;

  private final String reportUrl;

  private final Integer index;

  private final Semaphore sem;

  private final CountDownLatch latch;

  private final Map<String, String> reportsContents;

  private final String protocol;

  private final String cms;

  private final String auth;

  /**
   * Method is used to download Report TR: HK66224: start Put timeout value which will be 180000 by default. timeout
   * value is used here by AlarmHandlerAction class
   * 
   * @param index
   * @param sem
   * @param latch
   * @param targetAlarmReport
   * @param reportUrl
   * @param protocol
   * @param hostname
   * @param cms
   * @param username
   * @param password
   * @param timeout
   * @param auth
   * @param log
   * @param reportsContents
   */
  public AlarmReportDownloadThread(final int index, final Semaphore sem, final CountDownLatch latch,
      final CachedAlarmReport targetAlarmReport, final String reportUrl, final String protocol, final String hostname,
      final String cms, final String username, final String password, final long timeout, final String auth,
      final Logger log, final Map<String, String> reportsContents) {
    this.index = index;
    this.sem = sem;
    this.latch = latch;
    this.targetAlarmReport = targetAlarmReport;
    this.reportUrl = reportUrl;
    this.protocol = protocol;
    this.hostname = hostname;
    this.cms = cms;
    this.username = username;
    this.password = password;
    this.timeout = timeout;
    this.auth = auth;
    this.log = log;
    this.reportsContents = reportsContents;
  }

  public void run() {

    IAlarmcfgSession session = null;

    try {

      session = AlarmcfgSessionFactory.createSession(protocol, hostname, cms, username, password, auth);

      final long startTime = new Date().getTime();

      final String reportName = targetAlarmReport.getURL();
      // make report request
      final IAlarmcfgReportRequest req = AlarmcfgSessionFactory.createRequest(reportUrl);

      try {
        // Wait for turn to download the report.
        sem.acquire();
      } catch (InterruptedException e) {
        this.log.log(Level.INFO, "AlarmReportDownloadThread with index " + index
            + " interrupted while waiting for semaphore.");
      }

      this.log.finest("AlarmReportDownloadThread with index " + index + " starting");

      final StringBuffer reportXml = new StringBuffer("");

      final AlarmcfgSessionThread boundReportThread = new AlarmcfgSessionThread("get_report", Thread.currentThread(),
          this.log, session, req, reportXml);
      boundReportThread.start();

      boolean maxTimeoutExceed = false;

      synchronized (reportXml) {
        try {
          // Try to get the report for value set in timeout variable
          reportXml.wait(timeout);
          this.log.log(Level.WARNING,
              "Waited" + timeout / 1000 + " seconds to get bound report " + targetAlarmReport.getReportName()
                  + " from alarmcfg.");
          maxTimeoutExceed = true;
        } catch (InterruptedException e) {
          // Interrupted by the thread before timeout. All
          // should be fine
        }
      }

      long execTime = new Date().getTime();
      execTime = execTime - startTime;

      final String resultString = reportXml.toString().trim();

      if (resultString.equals("")) {
        this.log.log(Level.WARNING, "Report " + targetAlarmReport.getReportName()
            + " content was empty. Please check status of the BI LaunchPad.");
      } else {
        if (maxTimeoutExceed) {
          this.log.log(Level.WARNING, "Report " + targetAlarmReport.getReportName() + " content not retrieved in "
              + timeout / 1000 + " seconds. Execution terminated.");
        } else {
          this.log.log(Level.FINEST, "AlarmReportDownloadThread with index " + index
              + " finished downloading alarm report in " + execTime + " ms.");
          this.reportsContents.put(targetAlarmReport.getReportName(), resultString);
          this.log.log(Level.FINE, "AlarmReportDownloadThread with index " + index + ": Report " + reportName
              + " contents retrieved successfully.");
        }
      }

    } catch (ACAuthenticateException e) {
      this.log.log(Level.SEVERE, "Connecting to alarmcfg failed. Authentication failed.", e);
    } catch (ACSessionException e) {
      this.log.log(Level.SEVERE, "Connecting to alarmcfg failed. Could not create connection.", e);
    } catch (Exception e) {
      this.log.finest("AlarmReportDownloadThread with index " + index + " failed to download report "
          + targetAlarmReport.getReportName());
      this.log.severe("Failed to get bound report " + targetAlarmReport.getReportName() + " from alarmcfg");
      this.log.severe(e.getMessage());
    } finally {

      this.log.finest("AlarmReportDownloadThread with index " + index
          + " releasing semaphore and reducing countdownlatch.");
      try {
        if (session != null) {
          session.close();
        }
      } catch (Exception e) {
        // No problem here
      }
      sem.release();
      latch.countDown();
    }
  }
}
