package com.ericsson.etl.alarm;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import ssc.rockfactory.RockException;
import ssc.rockfactory.RockFactory;
import com.distocraft.dc5000.etl.rock.Meta_transfer_actions;
import com.distocraft.dc5000.etl.rock.Meta_transfer_actionsFactory;

/**
 * Class is used to change the webportal connection properties using script change_alarm_property
 * @author 
 *
 */
public class ChangeAlarmConnProperty extends AlarmProperty {


  private RockFactory etlRockFactory = null;

  /**
   * Default constructor to call method to initialize database server
   */
  public ChangeAlarmConnProperty(){

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

    }

  }

  public static void main(final String[] args){
    /*    args = new String[]{
        "username",
        "eniq_alarm"
    };*/

    final ChangeAlarmConnProperty changeAlarmConnProperty = new ChangeAlarmConnProperty();
    final String listOfEditableProperties[] = {"authmethod","outputPath","username","cms","hostname","password","protocol","outputFilePrefix"};
    
    try{
      final String propertyName = args[0];
      if(propertyName!=null && !(propertyName.equals("")) 
         && propertyName.equals("showAlarmConnProperties")){
       changeAlarmConnProperty.showUsage(listOfEditableProperties);
     }

     final String newCmsHost = args[1];

     boolean propertyNameMatchStatus = false; 
     for (int i =0; i < listOfEditableProperties.length; i++){
       if(((listOfEditableProperties[i]).equals(propertyName))){
         propertyNameMatchStatus = true;
         break;
       }
     }
     if(!propertyNameMatchStatus){
       System.out.println("Property name is not matched with the given property name ");
       changeAlarmConnProperty.showUsage(listOfEditableProperties);
     }
     changeAlarmConnProperty.changeProperty(propertyName,newCmsHost);
    }
    catch (Exception e) {
      e.printStackTrace();
      changeAlarmConnProperty.showUsage(listOfEditableProperties);
    }
    
}


  /**
   * Method contains logic to change connection details to WebPortal
   * @param propertyName
   * @param newPropertyValue
   */
  public void changeProperty( final String propertyName, final String newPropertyValue){

    boolean commit = false;
    try {
      final List<Meta_transfer_actions> alarmActions = getTransferAction(etlRockFactory);
      for (Meta_transfer_actions action : alarmActions) {
        final Properties actionDetails = getActionProperties(action.getAction_contents());
        final String oldCmsHost = actionDetails.getProperty(propertyName);
        actionDetails.setProperty(propertyName, newPropertyValue);
        final String dbContents = saveActionDetails(actionDetails);
        action.setAction_contents(dbContents);
        System.out.println("Switching Property " + propertyName + "for " + action.getTransfer_action_name() + " from " +
            oldCmsHost + " to " + newPropertyValue);
        action.setIsDecryptionRequired(false);
        action.updateDB();
        commit = true;
      }
    } 
    catch(Exception e){

    }
    finally {
      if (commit) {
        try {
          etlRockFactory.commit();
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      } else {
        try {
          etlRockFactory.rollback();
        } catch (SQLException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Load Connection details into property object
   * @param actionDetails
   * @return
   */
  private Properties getActionProperties(final String actionDetails) {
    final Properties props = new Properties();
    try {
      final ByteArrayInputStream bais = new ByteArrayInputStream(actionDetails.getBytes());
      props.load(bais);
      bais.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return props;
  }

  /**
   * 
   * @param rf
   * @return
   * @throws RockException 
   * @throws SQLException 
   * @throws Exception
   */
  private static List<Meta_transfer_actions> getTransferAction(final RockFactory rf) throws SQLException, RockException{
    final Meta_transfer_actions transferWhereCollection = new Meta_transfer_actions(rf);
    transferWhereCollection.setAction_type("AlarmHandler");
    transferWhereCollection.setEnabled_flag("Y");
    final Meta_transfer_actionsFactory mtFactory = new Meta_transfer_actionsFactory(rf, transferWhereCollection);
    return mtFactory.get();
  }

  /**
   * Shows the list of valid editable properties
   * @param editableProperties
   */
  protected void showUsage(final String[] editableProperties) {
    System.out.println("Execute: ./change_alarm_property.bsh -alarmconn COMMAND");
    System.out.println("where COMMAND is:<propertyName> <value>");
    System.out.println("For Example: cms webportal:6400");
    System.out.println("  property Names are:");

    for(int i = 0; i < editableProperties.length; i++){

      System.out.println(" " + editableProperties[i] );
    }
    System.exit(1);
  }

}
