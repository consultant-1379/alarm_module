/**
 * 
 */
package com.ericsson.etl.alarm;


/**
 * @author eheijun
 *
 */
public class DummyAlarmPropertyTest extends AlarmPropertyTest {
  
  private class DummyAlarmProperty extends AlarmProperty {

    @Override
    void showUsage(String[] editableProperties) {
      // NOTHINGTODO
    }
    
  }

  @Override
  AlarmProperty getAlarmProperty() {
    return new DummyAlarmProperty();
  }
  
}
