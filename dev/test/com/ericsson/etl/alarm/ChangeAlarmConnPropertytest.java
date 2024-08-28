/**
 * 
 */
package com.ericsson.etl.alarm;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.ericsson.etl.alarm.ChangeAlarmConnProperty;

/**
 * @author eninkar
 * 
 */
public class ChangeAlarmConnPropertytest {

  ChangeAlarmConnProperty changeAlarmConnProperty;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {

    changeAlarmConnProperty = new ChangeAlarmConnProperty();

  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  /**
   * Test method for
   * {@link com.ericsson.etl.alarm.ChangeAlarmConnProperty#changeProperty(java.lang.String, java.lang.String)}.
   */
  @Test
  @Ignore("Needs to be fixed")
  public final void testChangePropertyName() {

    final String propertyName = "cms";
    String expectedProperty = "";
    final String listOfEditableProperties[] = { "authmethod", "outputPath", "username", "cms", "hostname", "password",
        "protocol", "outputFilePrefix" };

    try {
      for (int i = 0; i < listOfEditableProperties.length; i++) {
        if (propertyName.equals(listOfEditableProperties[i])) {
          expectedProperty = propertyName;
        }
      }
      assertSame("Property name is not matched", expectedProperty, propertyName);
    } catch (Exception e) {
    }
  }

  /**
   * Test method for
   * {@link com.ericsson.etl.alarm.ChangeAlarmConnProperty#changeProperty(java.lang.String, java.lang.String)}.
   */
  @Test
  @Ignore("Needs to be fixed")
  public final void testChangeProperty() {

    final String propertyName = "password";
    final String newPropertyValue = "eniqalarm";
    String expectedProperty = null;
    final String listOfEditableProperties[] = { "authmethod", "outputPath", "username", "cms", "hostname", "password",
        "protocol", "outputFilePrefix" };

    try {
      for (int i = 0; i < listOfEditableProperties.length; i++) {
        if (propertyName.equals(listOfEditableProperties[i])) {
          expectedProperty = propertyName;
        }
      }

      if (expectedProperty.equals("") || expectedProperty == null) {
        throw new Exception();
      }
      changeAlarmConnProperty.changeProperty(propertyName, newPropertyValue);
      assertTrue("Property Change", true);
    } catch (Exception e) {
      e.printStackTrace();
      assertTrue(false);

    }
  }
}
