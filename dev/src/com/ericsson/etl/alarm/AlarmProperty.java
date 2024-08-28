package com.ericsson.etl.alarm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.ericsson.eniq.repository.ETLCServerProperties;

public abstract class AlarmProperty {

  private static final String CONF_DIR = "CONF_DIR";

  private static final String ENIQ_SW_CONF = "/eniq/sw/conf";

  private static final String ETLC_SERVER_PROPERTIES = "ETLCServer.properties";

  String DbUrl = null;

  String DBUserName = null;

  String DBPassword = null;

  String DBDriverName = null;

  /**
   * makes connection to ETL database
   */

  abstract void showUsage(String[] editableProperties);

  public void getServerProperties() {

    try {

      String etlcServerPropertyFile = System.getProperty(CONF_DIR, ENIQ_SW_CONF);
      if (!etlcServerPropertyFile.endsWith(File.separator)) {
        etlcServerPropertyFile += File.separator;
      }
      etlcServerPropertyFile += ETLC_SERVER_PROPERTIES;
      final Properties etlcServerProperties = new ETLCServerProperties(etlcServerPropertyFile);

      this.DbUrl = etlcServerProperties.getProperty("ENGINE_DB_URL");
      this.DBUserName = etlcServerProperties.getProperty("ENGINE_DB_USERNAME");
      this.DBPassword = etlcServerProperties.getProperty("ENGINE_DB_PASSWORD");
      this.DBDriverName = etlcServerProperties.getProperty("ENGINE_DB_DRIVERNAME");

    } catch (IOException e) {
      System.err.println("Failed to read " + ETLC_SERVER_PROPERTIES + ": " + e.getMessage());
    }
  }

  /**
   * Used to store the changed properties and return for saving to DB
   * 
   * @param actionDetails
   * @return
   */
  String saveActionDetails(final Properties actionDetails) {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      actionDetails.store(baos, "");
    } catch (IOException e) {

      e.printStackTrace();
    }
    return baos.toString();
  }

}
