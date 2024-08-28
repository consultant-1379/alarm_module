package com.distocraft.dc5000.etl.alarm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import ssc.rockfactory.RockFactory;

import com.distocraft.dc5000.etl.rock.Meta_transfer_actions;
import com.distocraft.dc5000.etl.rock.Meta_transfer_actionsFactory;
import com.ericsson.eniq.repository.ETLCServerProperties;

/**
 * This class is a custom made ANT-task that changes the password for alarm
 * interfaces. Copyright (c) 1999 - 2007 AB LM Ericsson Oy All rights reserved.
 * 
 * @author ejannbe
 */
public class ChangeAlarmPassword extends Task { // NOPMD by eheijun on 15/07/11 16:16

  private static final String CONF_DIR = "CONF_DIR";

  private static final String ENIQ_SW_CONF = "/eniq/sw/conf";

  private static final String ETLC_SERVER_PROPERTIES = "ETLCServer.properties";

  private String newAlarmPassword = "";

  /**
   * This function starts the execution of task.
   */
  public void execute() throws BuildException {
    this.changeAlarmPassword();
    System.out.println("Finished changing alarm interface passwords.");
  }

  /**
   * This function creates the rockfactory object to etlrep from the database
   * connection details read from ETLCServer.properties file.
   * 
   * @param databaseConnectionDetails
   * @return Returns the created RockFactory.
   */
  private RockFactory createEtlrepRockFactory() throws BuildException {
    RockFactory rockFactory = null;

    try {
      
      String etlcServerPropertyFile = System.getProperty(CONF_DIR, ENIQ_SW_CONF);
      if (!etlcServerPropertyFile.endsWith(File.separator)) {
        etlcServerPropertyFile += File.separator;
      }
      etlcServerPropertyFile += ETLC_SERVER_PROPERTIES;
      final Properties etlcServerProperties = new ETLCServerProperties(etlcServerPropertyFile);

      final String databaseUrl = etlcServerProperties.getProperty("ENGINE_DB_URL");
      final String databaseUsername = etlcServerProperties.getProperty("ENGINE_DB_USERNAME");
      final String databasePassword = etlcServerProperties.getProperty("ENGINE_DB_PASSWORD");
      final String databaseDriver = etlcServerProperties.getProperty("ENGINE_DB_DRIVERNAME");
      
      rockFactory = new RockFactory(databaseUrl, databaseUsername, databasePassword, databaseDriver,
          "ChangeAlarmPassword", true);

    } catch (Exception e) {
      e.printStackTrace();
      throw new BuildException("Unable to initialize database connection.", e);
    }

    return rockFactory;
  }

  public void changeAlarmPassword() throws BuildException { // NOPMD by eheijun on 15/07/11 16:16

    if (this.newAlarmPassword == null || this.newAlarmPassword.equalsIgnoreCase("")) {
      throw new BuildException("New alarm password is empty. Password not changed.");
    }

    RockFactory etlrepRockFactory = createEtlrepRockFactory();
    try {
      final Meta_transfer_actions whereAction = new Meta_transfer_actions(etlrepRockFactory);
      whereAction.setEnabled_flag("Y");
      whereAction.setAction_type("AlarmHandler");

      final Meta_transfer_actionsFactory actionsFactory = new Meta_transfer_actionsFactory(etlrepRockFactory,
          whereAction);
      final Vector<Meta_transfer_actions> alarmHandlerActions = actionsFactory.get();

      if (alarmHandlerActions.size() <= 0) {
        throw new Exception("No active AlarmHandler actions not found. Password cannot be changed.");
      } else {
        System.out.println("Found " + alarmHandlerActions.size()
            + " AlarmHandler actions. Changing password for these actions.");
      }

      for (Meta_transfer_actions currAction : alarmHandlerActions) {
        
        System.out.println("Changing password for action " + currAction.getTransfer_action_name());

        final Properties currActionProperties = new Properties(); // NOPMD by eheijun on 15/07/11 16:16
        
        final String actionContents = currAction.getAction_contents();

        if (actionContents != null && actionContents.length() > 0) {

          try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(actionContents.getBytes()); // NOPMD by eheijun on 15/07/11 16:16
            currActionProperties.load(bais);
            bais.close();
          } catch (Exception e) {
            throw new BuildException("Error reading action contents.");
          }
        }

        if (currActionProperties.containsKey("password")) {
          // Change the alarm handler password to the database.
          currActionProperties.setProperty("password", this.newAlarmPassword);
          
          final ByteArrayOutputStream baos = new ByteArrayOutputStream(); // NOPMD by eheijun on 15/07/11 16:16
          currActionProperties.store(baos, "");
          currAction.setAction_contents(baos.toString());
          currAction.setIsDecryptionRequired(false);
          currAction.updateDB();

          System.out.println("Updated password for " + currAction.getTransfer_action_name());
        } else {
          System.out.println("Could not find parameter \"password\" from action contents of "
              + currAction.getTransfer_action_name() + ". Password not updated for this action.");
        }

      }

    } catch (Exception e) {
      throw new BuildException(e);
    } finally {
      try {
        etlrepRockFactory.getConnection().close();
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

  }

  public String getNewAlarmPassword() {
    return newAlarmPassword;
  }

  public void setNewAlarmPassword(final String newAlarmPassword) {
    this.newAlarmPassword = newAlarmPassword;
  }

}
