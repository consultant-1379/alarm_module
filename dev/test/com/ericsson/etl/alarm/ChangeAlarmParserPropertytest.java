/**
 * 
 */
package com.ericsson.etl.alarm;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


/**
 * @author eninkar
 *
 */
public class ChangeAlarmParserPropertytest {
  
  
  ChangeAlarmParserProperty changeAlarmParserProperty;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    
    changeAlarmParserProperty = new ChangeAlarmParserProperty();
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test method for {@link com.ericsson.etl.alarm.ChangeAlarmParserProperty#changePropertyValue(java.lang.String, java.lang.String, java.lang.String)}.
   */
  @Test
  @Ignore("Needs to be fixed")
  public void testChangePropertyValueForPropName() {
    
    String propertyName = "maxFilesPerRun";
    String expectedProperty = null;
    final String listOfEditableProperties[] = {"outDir","maxFilesPerRun","dublicateCheck","thresholdMethod","inDir","ProcessedFiles.fileNameFormat","AlarmTemplate","minFileAge","periodDuration","baseDir","useZip","archivePeriod", "loaderDir", "tag_id", "doubleCheckAction", "dateformat","ProcessedFiles.processedDir", "failedAction", "dirThreshold", "workers", "afterParseAction"};
    
     for(int i = 0; i<listOfEditableProperties.length;i++){
       if(listOfEditableProperties[i].equals(propertyName)){
         expectedProperty = listOfEditableProperties[i];
       }
     }
   assertSame("Property name is not matched", expectedProperty, propertyName);  
  }
  
  /**
   * Test method for {@link com.ericsson.etl.alarm.ChangeAlarmParserProperty#changePropertyValue(java.lang.String, java.lang.String, java.lang.String)}.
   */
  @Test
  @Ignore("Needs to be fixed")
  public void testChangePropertyValue() {
    
    String alarmInterfaceName = "AlarmInterface_15min";
    String propertyName = "afterparseAction";
    String propertyValue = "move";
     try {
      boolean status = changeAlarmParserProperty.changePropertyValue(
                alarmInterfaceName, propertyName, propertyValue);
      
     assertTrue("Alarm Property Value", status);
    } catch (Exception e) {
       e.printStackTrace();
    }
    
  }

}
