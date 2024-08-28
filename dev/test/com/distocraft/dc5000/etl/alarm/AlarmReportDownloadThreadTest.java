/**
 * 
 */
package com.distocraft.dc5000.etl.alarm;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ericsson.eniq.alarmcfg.clientapi.AlarmcfgSessionFactory;
import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgReport;
import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgReportRequest;
import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgSession;
import com.ericsson.eniq.etl.alarm.CachedAlarmReport;


/**
 * @author eheijun
 *
 */
public class AlarmReportDownloadThreadTest {

  private static final String TEST = "TEST";
  private static final String TEST_URL = "TEST_URL";
  private static final String TEST_REPORT = "TEST_REPORT";
  private static final String TEST_RESULT = "<xml></xml>";
  private final Mockery context = new JUnit4Mockery() {

    {
      setImposteriser(ClassImposteriser.INSTANCE);
    }
  };
  
  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  private final static int INDEX = 1;
  private Semaphore mockSem;
  private CountDownLatch mockLatch;
  private CachedAlarmReport mockTargetAlarmReport;
  private String reportUrl;
  private String protocol;
  private String hostname;
  private String cms;
  private String username;
  private String password;
  private long timeout;
  private String auth;
  private Logger log;
  private Map<String, String> reportsContents;
  
  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    
    final IAlarmcfgSession mockAlarmcfgSession = context.mock(IAlarmcfgSession.class);
    final IAlarmcfgReport mockAlarmcfgReport  = context.mock(IAlarmcfgReport.class);;
    
    mockTargetAlarmReport = context.mock(CachedAlarmReport.class);
    mockSem = context.mock(Semaphore.class);
    mockLatch = context.mock(CountDownLatch.class);
    
    timeout = 2000;
    log = Logger.getLogger(TEST);
    reportsContents = new HashMap<String, String>();
    
    AlarmcfgSessionFactory.setNonDefaultSession(mockAlarmcfgSession);
    
    context.checking(new Expectations() {

      {
        allowing(mockTargetAlarmReport).getURL();
        will(returnValue(TEST_URL));
        allowing(mockTargetAlarmReport).getReportName();
        will(returnValue(TEST_REPORT));
        
        allowing(mockSem).acquire();
        allowing(mockSem).release();
        
        allowing(mockLatch).countDown();
        
        allowing(mockAlarmcfgReport).getXML();
        will(returnValue(TEST_RESULT));
        
        allowing(mockAlarmcfgSession).getReport(with(any(IAlarmcfgReportRequest.class)));
        will(returnValue(mockAlarmcfgReport));
        allowing(mockAlarmcfgSession).close();
        
      }
    });
    
    
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test method for {@link com.distocraft.dc5000.etl.alarm.AlarmReportDownloadThread#run()}.
   */
  @Test
  public void testRun() {

    AlarmReportDownloadThread thread = new AlarmReportDownloadThread(INDEX, mockSem, mockLatch, mockTargetAlarmReport, reportUrl, protocol, hostname,
        cms, username, password, timeout, auth, log, reportsContents);
    thread.run();
    
    assertThat(this.reportsContents.get(TEST_REPORT), is(TEST_RESULT));
  }

}
