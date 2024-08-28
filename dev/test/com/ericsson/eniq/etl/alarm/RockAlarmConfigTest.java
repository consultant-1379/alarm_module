/**
 * 
 */
package com.ericsson.eniq.etl.alarm;

import static org.junit.Assert.*;

import com.ericsson.eniq.common.testutilities.MemoryDatabaseUtility;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ssc.rockfactory.RockFactory;


/**
 * @author eheijun
 *
 */
public class RockAlarmConfigTest {

  private static final String TEST_BASETABLE_NAME = "DC_E_RAN_CCDEVICE_RAW";

  private static final String TEST_REPORT_ID = "02afd646-cab9-471d-bb02-3e58ae2e3d8d";

  private static final String ALARM_INTERFACE_15MIN = "AlarmInterface_15min";

  public static final String TEST_APPLICATION = RockAlarmConfigTest.class.getName();

  private static RockFactory testEtlRep;

  private static RockFactory testDwhRep;

  private static RockFactory testDwh;

  private static Logger testLogger;

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {

    testEtlRep = new RockFactory(MemoryDatabaseUtility.TEST_ETLREP_URL, MemoryDatabaseUtility.TESTDB_USERNAME,
        MemoryDatabaseUtility.TESTDB_PASSWORD, MemoryDatabaseUtility.TESTDB_DRIVER, TEST_APPLICATION, true);
    final URL etlrepsqlurl = ClassLoader.getSystemResource(MemoryDatabaseUtility.TEST_ETLREP_BASIC);
    MemoryDatabaseUtility.loadSetup(testEtlRep, etlrepsqlurl);
    testDwhRep = new RockFactory(MemoryDatabaseUtility.TEST_DWHREP_URL, MemoryDatabaseUtility.TESTDB_USERNAME,
        MemoryDatabaseUtility.TESTDB_PASSWORD, MemoryDatabaseUtility.TESTDB_DRIVER, TEST_APPLICATION, true);
    final URL dwhrepsqlurl = ClassLoader.getSystemResource(MemoryDatabaseUtility.TEST_DWHREP_BASIC);
    MemoryDatabaseUtility.loadSetup(testDwhRep, dwhrepsqlurl);
    testDwh = new RockFactory(MemoryDatabaseUtility.TEST_DWH_URL, MemoryDatabaseUtility.TESTDB_USERNAME,
        MemoryDatabaseUtility.TESTDB_PASSWORD, MemoryDatabaseUtility.TESTDB_DRIVER, TEST_APPLICATION, true);
    final URL dwhsqlurl = ClassLoader.getSystemResource(MemoryDatabaseUtility.TEST_DWH_BASIC);
    MemoryDatabaseUtility.loadSetup(testDwh, dwhsqlurl);
    
    testLogger = Logger.getLogger("TEST");
    
  }
  
  
  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    MemoryDatabaseUtility.shutdown(testDwh);
    MemoryDatabaseUtility.shutdown(testDwhRep);
    MemoryDatabaseUtility.shutdown(testEtlRep);
  }

  
  /**
   * Test method for {@link com.ericsson.eniq.etl.alarm.RockAlarmConfig#reload()}.
   */
  @Test
  public void testReload() {
    AlarmConfig config = new RockAlarmConfig(testDwhRep, testLogger);
    config.reload();

    List<CachedAlarmInterface> actual1 = config.getAlarmInterfaces();
    assertTrue(actual1.size() == 2);

    CachedAlarmInterface actual2 = config.getAlarmInterfaceById(ALARM_INTERFACE_15MIN);
    assertTrue(actual2.getInterfaceId().equals(ALARM_INTERFACE_15MIN));
    assertTrue(actual2.getAlarmReports().size() > 0);
    assertTrue(actual2.getCollectionSetId().equals(9L));
    assertTrue(actual2.getCollectionId().equals(99L));
    assertTrue(actual2.isScheduled());

    CachedAlarmReport actual3 = config.getAlarmReport(ALARM_INTERFACE_15MIN, TEST_REPORT_ID);
    assertTrue(actual3.getReportId().equals(TEST_REPORT_ID));
    assertTrue(actual3.getInterfaceId().equals(ALARM_INTERFACE_15MIN));
    assertNotNull(actual3.getBaseTableName());
    assertNotNull(actual3.getReportName());
    assertNotNull(actual3.getURL());
    assertFalse(actual3.isSimultaneous());
    assertTrue(actual3.isScheduled());
    assertTrue(actual3.getAlarmReportPrompts().size() > 0);
    assertNotNull(actual3.getAlarmReportPrompts().get(0).getName());
    assertNotNull(actual3.getAlarmReportPrompts().get(0).getValue());
    
    CachedAlarmReport actual4 = config.getAlarmReportById(TEST_REPORT_ID);
    assertTrue(actual4.getReportId().equals(TEST_REPORT_ID));
    List<CachedAlarmReport> actual5 = config.getAlarmReportsByBasetable(TEST_BASETABLE_NAME);
    assertTrue(actual5.size() == 2);
  }

}
