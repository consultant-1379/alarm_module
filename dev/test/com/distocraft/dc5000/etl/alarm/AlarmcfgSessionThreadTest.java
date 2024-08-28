/**
 * 
 */
package com.distocraft.dc5000.etl.alarm;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.logging.Logger;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgReport;
import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgReportRequest;
import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgSession;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACAuthenticateException;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACReportNotFoundException;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACSessionException;

/**
 * @author eheijun
 * 
 */
public class AlarmcfgSessionThreadTest {

  private static final String TEST_CASE_REPORT = "TEST_CASE_REPORT";

  private static final Object TEST_CASE_REPORT_XML = "<xml></xml>";

  private final Mockery context = new JUnit4Mockery() {

    {
      setImposteriser(ClassImposteriser.INSTANCE);
    }
  };

  private String action;

  private Thread maint;

  private Logger log;

  private IAlarmcfgSession alarmCfgSession;

  private IAlarmcfgReportRequest alarmCfgReportRequest;

  private IAlarmcfgReport alarmCfgReport;

  private StringBuffer xmlText;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {

    maint = context.mock(Thread.class);
    log = Logger.getLogger("TEST");
    alarmCfgSession = context.mock(IAlarmcfgSession.class);
    alarmCfgReportRequest = context.mock(IAlarmcfgReportRequest.class);
    alarmCfgReport = context.mock(IAlarmcfgReport.class);
    xmlText = new StringBuffer("");

    context.checking(new Expectations() {

      {
        allowing(alarmCfgReportRequest).getReportName();
        will(returnValue(TEST_CASE_REPORT));

        allowing(alarmCfgReport).getXML();
        will(returnValue(TEST_CASE_REPORT_XML));

        allowing(maint).interrupt();
      }
    });
  }

  /**
   * Test method for {@link com.distocraft.dc5000.etl.alarm.AlarmcfgSessionThread#run()}.
   * 
   * @throws ACAuthenticateException
   * @throws ACReportNotFoundException
   * @throws ACSessionException
   */
  @Test
  public void testRun() throws ACSessionException, ACReportNotFoundException, ACAuthenticateException {

    action = "get_report";

    context.checking(new Expectations() {

      {
        allowing(alarmCfgSession).getReport(alarmCfgReportRequest);
        will(returnValue(alarmCfgReport));
      }
    });

    AlarmcfgSessionThread thread = new AlarmcfgSessionThread(action, maint, log, alarmCfgSession,
        alarmCfgReportRequest, xmlText);
    thread.run();

    assertThat(xmlText.toString(), is(TEST_CASE_REPORT_XML));

  }

  /**
   * Test method for {@link com.distocraft.dc5000.etl.alarm.AlarmcfgSessionThread#run()}.
   * 
   * @throws ACAuthenticateException
   * @throws ACReportNotFoundException
   * @throws ACSessionException
   */
  @Test
  public void testRunFail() throws ACSessionException, ACReportNotFoundException, ACAuthenticateException {

    action = "get_report";

    context.checking(new Expectations() {

      {
        allowing(alarmCfgSession).getReport(alarmCfgReportRequest);
        will(throwException(new ACReportNotFoundException()));
      }
    });

    AlarmcfgSessionThread thread = new AlarmcfgSessionThread(action, maint, log, alarmCfgSession,
        alarmCfgReportRequest, xmlText);
    thread.run();

    assertThat(xmlText.toString(), not(TEST_CASE_REPORT_XML));
  }

  /**
   * Test method for {@link com.distocraft.dc5000.etl.alarm.AlarmcfgSessionThread#run()}.
   * 
   * @throws ACAuthenticateException
   * @throws ACReportNotFoundException
   * @throws ACSessionException
   */
  @Test
  public void testRunInvalidCall() throws ACSessionException, ACReportNotFoundException, ACAuthenticateException {

    action = "xet_report";

    AlarmcfgSessionThread thread = new AlarmcfgSessionThread(action, maint, log, alarmCfgSession,
        alarmCfgReportRequest, xmlText);
    thread.run();

    assertThat(xmlText.toString(), not(TEST_CASE_REPORT_XML));
  }

}
