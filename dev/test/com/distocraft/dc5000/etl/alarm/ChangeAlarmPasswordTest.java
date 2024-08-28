package com.distocraft.dc5000.etl.alarm;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ssc.rockfactory.RockFactory;

import com.ericsson.eniq.common.testutilities.MemoryDatabaseUtility;
import com.ericsson.eniq.common.testutilities.UnitDatabaseTestCase;


public class ChangeAlarmPasswordTest extends UnitDatabaseTestCase {

  private static final URL etlrepurl = ClassLoader.getSystemResource("setupSQL/ETLREP/alarm");
  
  private static final String NEW_PASSWORD = "new_password";

  private static RockFactory etlrep;
  
  private static String checksql = "SELECT ACTION_CONTENTS_01 FROM META_TRANSFER_ACTIONS"
    + " WHERE VERSION_NUMBER = '((7))'"
    + " AND ACTION_TYPE = 'AlarmHandler'"
    + " AND ENABLED_FLAG = 'Y'";
  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    System.setProperty("CONF_DIR", System.getProperty("java.io.tmpdir"));
    setup(TestType.unit);
    etlrep = getRockFactory(Schema.etlrep);
    MemoryDatabaseUtility.loadSetup(etlrep, etlrepurl);
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    System.clearProperty("CONF_DIR");
  }

  @Test
  public void testExecuteWorks() throws SQLException, IOException {
    ChangeAlarmPassword task = new ChangeAlarmPassword();
    task.setNewAlarmPassword(NEW_PASSWORD);
    task.execute();
    Statement stmt = etlrep.getConnection().createStatement();
    try {
      ResultSet results = stmt.executeQuery(checksql);
      try {
        while (results.next()) {
          Properties parameters = new Properties(); 
          final ByteArrayInputStream bais = new ByteArrayInputStream(results.getString(1).getBytes());
          try {
            parameters.load(bais);
          } finally {
            bais.close();
          }
          assertThat(parameters.getProperty("password"), is(NEW_PASSWORD));
        }
      } finally {
        results.close();
      }
    } finally {
      stmt.close();
    }
  }
  
  @Test(expected=BuildException.class)
  public void testExecuteWithoutNewPasswordFail() {
    ChangeAlarmPassword task = new ChangeAlarmPassword();
    task.execute();
  }

  @Test
  public void testSetNewAlarmPassword() {
    ChangeAlarmPassword task = new ChangeAlarmPassword();
    task.setNewAlarmPassword(NEW_PASSWORD);
    String expected = task.getNewAlarmPassword();
    assertThat(expected, is(NEW_PASSWORD));
  }

}
