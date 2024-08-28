/**
 * 
 */
package com.ericsson.etl.alarm;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author eheijun
 *
 */
@Ignore
public abstract class AlarmPropertyTest {

  private static final String ETLDB_URL = "etldbUrl";
  private static final String ETLREP_USERNAME = "etlrep";
  private static final String ETLREP_PASSWORD = "etlrep";
  private static final String ETLREP_CRYPTED_PASSWORD = "6GKOICE3nLQ=";
  private static final String ETLDB_DRIVER = "etldbDriver";

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    System.setProperty("CONF_DIR", System.getProperty("user.dir"));
    final File etlcProperties = new File(System.getProperty("CONF_DIR"), "ETLCServer.properties");
    try {
      final PrintWriter pw = new PrintWriter(new FileWriter(etlcProperties));
      pw.println("ENGINE_DB_URL = " + ETLDB_URL);
      pw.println("ENGINE_DB_USERNAME = " + ETLREP_USERNAME);
      pw.println("ENGINE_DB_PASSWORD = " + ETLREP_CRYPTED_PASSWORD);
      pw.println("ENGINE_DB_DRIVERNAME = " + ETLDB_DRIVER);
      pw.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    File etlcProperties = new File(System.getProperty("CONF_DIR"), "ETLCServer.properties");
    etlcProperties.delete();
    System.clearProperty("CONF_DIR");
  }

  abstract AlarmProperty getAlarmProperty();

  /**
   * Test method for {@link com.ericsson.etl.alarm.AlarmProperty#getServerProperties()}.
   */
  @Test
  public void testGetServerProperties() {
    AlarmProperty alarmProperty = getAlarmProperty();
    alarmProperty.getServerProperties();
    assertThat(alarmProperty.DbUrl, is(ETLDB_URL));
    assertThat(alarmProperty.DBUserName, is(ETLREP_USERNAME));
    assertThat(alarmProperty.DBPassword, is(ETLREP_PASSWORD));
    assertThat(alarmProperty.DBDriverName, is(ETLDB_DRIVER));
  }

  /**
   * Test method for {@link com.ericsson.etl.alarm.AlarmProperty#saveActionDetails(java.util.Properties)}.
   */
  @Test
  public void testSaveActionDetails() {
    Properties actionDetails = new Properties();
    actionDetails.setProperty("x", "y");
    AlarmProperty alarmProperty = getAlarmProperty();
    String testResult = alarmProperty.saveActionDetails(actionDetails);
    assertThat(testResult, notNullValue());
  }

}
