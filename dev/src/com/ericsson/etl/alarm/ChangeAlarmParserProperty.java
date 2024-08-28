package com.ericsson.etl.alarm;


import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Vector;

import ssc.rockfactory.RockException;
import ssc.rockfactory.RockFactory;

import com.distocraft.dc5000.etl.rock.Meta_transfer_actions;
import com.distocraft.dc5000.etl.rock.Meta_transfer_actionsFactory;

/**
 * This class is used to change Alarm Parser property using script change_alarm_property
 * @author 
 */
public class ChangeAlarmParserProperty extends AlarmProperty{

  private RockFactory etlRockFactory = null;
  //final private Meta_transfer_actions transferAction = null;
  //final private Properties actionDetails = null;

  /**
   *  USed to get Server properties and initialize Rock factory
   * @throws Exception
   */
  public ChangeAlarmParserProperty(){

    getServerProperties();
    
    
    try{

      this.etlRockFactory = new RockFactory(
          DbUrl,
          DBUserName,
          DBPassword,
          DBDriverName,
          "Test", false);
    }

    catch(Exception e){
      System.err.println("Failed to create database connection " + DbUrl + ", " + DBUserName + " ," + DBDriverName);
    }

  }

  public static void main(final String[] args){
  /*     args = new String[]{
        "AlarmInterface_15min",
        "dirThreshold",
         "0"
    };*/
    final ChangeAlarmParserProperty changeAlarmParseProperty = new ChangeAlarmParserProperty();
    final String listOfEditableProperties[] = {"outDir","maxFilesPerRun","dublicateCheck","thresholdMethod","inDir","ProcessedFiles.fileNameFormat","AlarmTemplate","minFileAge","periodDuration","baseDir","useZip","archivePeriod", "loaderDir", "tag_id", "doubleCheckAction", "dateformat","ProcessedFiles.processedDir", "failedAction", "dirThreshold", "workers", "afterParseAction"};
    try{

      String interfaceName = null;
      if(args[0]!=null && !(args[0].equals("")) && args[0].equals("showAlarmParserProperties")){
        changeAlarmParseProperty.showUsage(listOfEditableProperties);
      }
      else{

        interfaceName = args[0]; 
      }

      final String propertyName = args[1];
      final String newPropertyValue = args[2];
      
      if(propertyName == null || propertyName.equals("")){
        System.out.println("Argument can not be blanked");
        changeAlarmParseProperty.showUsage(listOfEditableProperties);
      }

      boolean propertyNameMatchStatus = false; 
      for (int i =0; i < listOfEditableProperties.length; i++){
        if(propertyName.equals(listOfEditableProperties[i])){
          propertyNameMatchStatus=true;
        }
      }
      if(!propertyNameMatchStatus){
        System.out.println("Property name is not matched with the given property name ");
        changeAlarmParseProperty.showUsage(listOfEditableProperties);
      }
      try {
        changeAlarmParseProperty.changePropertyValue(interfaceName, propertyName, newPropertyValue);
      } catch (Exception e) {
    
        e.printStackTrace();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      changeAlarmParseProperty.showUsage(listOfEditableProperties);
    }
  }

  /**
   * 
   * @param interfaceName
   * @param propertyName
   * @param action
   */
  public boolean changePropertyValue(final String interfaceName, final String propertyName, final String action){

    if (propertyName.equals("afterParseAction")){
      if(!action.equals("move") && !action.equals("delete")){
        System.out.println("move or delete");
        return false;
      }
    }
    

    boolean commit = false;
    try{
      final Meta_transfer_actions transferActions = getTransferAction(interfaceName, etlRockFactory);
      if(transferActions!=null){

        final Properties actionDetails = getActionProperties(transferActions);
         final String dbPropertyName = actionDetails.getProperty(propertyName, "Undefined");
        if(dbPropertyName.equals(action)){
          System.out.println("Property=" + propertyName + " "+ "Already have value" +" "+ action);
          return false;
        }
        System.out.println("Property Name = " + dbPropertyName);
        System.out.print("Change to "+action+"? ");
       final char res = (char)System.in.read();
        if(res == 'y'){
          actionDetails.setProperty(propertyName, action);
          final String dbContents = saveActionDetails(actionDetails);
          transferActions.setAction_contents(dbContents);
          transferActions.setIsDecryptionRequired(false);
          transferActions.updateDB();
          final Meta_transfer_actions ta = getTransferAction(interfaceName, etlRockFactory);
          final Properties ad = getActionProperties(ta);
          final String apa = ad.getProperty(propertyName, "Undefined");
          System.out.println("Property changed to = " + apa);
          commit = true;
        } else if (res == 'n') {
          etlRockFactory.rollback();
          return false;
        }
        else{
          System.out.println("Please enter only 'y' or 'n'");
          etlRockFactory.rollback();
          return false;
        }
        return true;
      }
      else{
        System.out.println("Please check the given Interface name. It should be AlarmInterface_Xmin or Xh");
        return false;
      }

    } catch (Exception e) {
       e.printStackTrace();
    } finally {
      if(commit){
        try {
          etlRockFactory.commit();
        } catch (SQLException e) {
           e.printStackTrace();
        }
      } else {
        try {
          etlRockFactory.rollback();
        } catch (SQLException e) {
           e.printStackTrace();
        }
      }
    }
    return false;
  }

  /**
   * 
   * @param action
   * @return
   */
  Properties getActionProperties(final Meta_transfer_actions action) {
    final String actionDetails = action.getAction_contents();
    final Properties props = new Properties();
    try {
     final ByteArrayInputStream bais = new ByteArrayInputStream(actionDetails.getBytes());
      props.load(bais);
      bais.close();
    } catch (Exception e) {
      //
    }
    return props;
  }

  /**
   * 
   * @param alarmInterface
   * @param rf
   * @return
   * @throws Exception
   */
  private Meta_transfer_actions getTransferAction( final String alarmInterface, final RockFactory rf){
    final Meta_transfer_actions transferWhereCollection = new Meta_transfer_actions(rf);
    transferWhereCollection.setTransfer_action_name("Parser_"+alarmInterface);
    transferWhereCollection.setEnabled_flag("Y");
    Meta_transfer_actionsFactory mtFactory = null;
    try {
      mtFactory = new Meta_transfer_actionsFactory(rf, transferWhereCollection);
    } catch (SQLException e) {
        e.printStackTrace();
    } catch (RockException e) {
      e.printStackTrace();
    }
    final Vector<Meta_transfer_actions> actions = mtFactory.get();
    if(actions.isEmpty()){
      return null;
    }
    return actions.get(0);
  }

  protected void showUsage(final String[] editableProperties) {
    System.out.println("Execute: ./change_alarm_property.bsh -alarmparser COMMAND");
    System.out.println("where COMMAND is: <interfaceName> <propertyName> <value>");
    System.out.println("For Example: AlarmInterface_15min afterParseAction move");
    System.out.println("  property Names are:");

    for(int i = 0; i < editableProperties.length; i++){

      System.out.println(" " + editableProperties[i] );
    }
    System.exit(1);
  }

}
