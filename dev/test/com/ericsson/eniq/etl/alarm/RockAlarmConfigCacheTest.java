/**
 * 
 */
package com.ericsson.eniq.etl.alarm;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;


/**
 * @author eheijun
 *
 */
public class RockAlarmConfigCacheTest {
  
  private static final String SOME_BASE_TABLE = "SOME_BASE_TABLE";
  private final Mockery context = new JUnit4Mockery() {

    {
      setImposteriser(ClassImposteriser.INSTANCE);
    }
  };
  

  /**
   * Test method for {@link com.ericsson.eniq.etl.alarm.RockAlarmConfigCache#initialize(ssc.rockfactory.RockFactory)}.
   */
  @Test
  public void testInitialize() {
    RockAlarmConfigCache.initialize(null, Logger.getLogger(this.getClass().getName()));
    AlarmConfig cache = RockAlarmConfigCache.getInstance();
    assertNull(cache);
  }

  /**
   * Test method for {@link com.ericsson.eniq.etl.alarm.RockAlarmConfigCache#revalidate(ssc.rockfactory.RockFactory)}.
   */
  @Test
  public void testRevalidate() {
    final AlarmConfig mockInstance = context.mock(AlarmConfig.class);
    
    context.checking(new Expectations() {{
        allowing(mockInstance).reload();
    }});
    
    RockAlarmConfigCache.setInstance(mockInstance);
    RockAlarmConfigCache.revalidate();
    AlarmConfig cache = RockAlarmConfigCache.getInstance();
    assertEquals(cache, mockInstance);
  }

  /**
   * Test method for {@link com.ericsson.eniq.etl.alarm.RockAlarmConfigCache#getInstance()}.
   */
  @Test
  public void testGetInstance() {
    AlarmConfig mockInstance = context.mock(AlarmConfig.class);;
    RockAlarmConfigCache.setInstance(mockInstance);
    AlarmConfig cache = RockAlarmConfigCache.getInstance();
    assertEquals(cache, mockInstance);
  }

  /**
   * Test method for {@link com.ericsson.eniq.etl.alarm.RockAlarmConfigCache#hasSimultanousReport(java.lang.String)}.
   */
  @Test
  public void testHasSimultanousReport() {
    final AlarmConfig mockInstance = context.mock(AlarmConfig.class);
    final CachedAlarmReport mockCachedAlarmReport = context.mock(CachedAlarmReport.class);
    final CachedAlarmReport[] alarmReports = new CachedAlarmReport[] { mockCachedAlarmReport };
    
    context.checking(new Expectations() {{
      allowing(mockInstance).getAlarmReportsByBasetable(with(SOME_BASE_TABLE));
      will(returnValue(Arrays.asList(alarmReports)));
      allowing(mockCachedAlarmReport).isSimultaneous();
      will(returnValue(true));
    }});
  
    RockAlarmConfigCache.setInstance(mockInstance);
    Boolean testResult = RockAlarmConfigCache.hasSimultanousReport(SOME_BASE_TABLE);
    assertTrue(testResult);
  }

  /**
   * Test method for {@link com.ericsson.eniq.etl.alarm.RockAlarmConfigCache#hasSimultanousReport(java.lang.String)}.
   */
  @Test
  public void testHasNotSimultanousReport() {
    final AlarmConfig mockInstance = context.mock(AlarmConfig.class);
    final ArrayList<CachedAlarmReport> emptyList = new ArrayList<CachedAlarmReport>();
    
    context.checking(new Expectations() {{
      allowing(mockInstance).getAlarmReportsByBasetable(with(SOME_BASE_TABLE));
      will(returnValue(emptyList));
    }});
  
    RockAlarmConfigCache.setInstance(mockInstance);
    Boolean testResult = RockAlarmConfigCache.hasSimultanousReport(SOME_BASE_TABLE);
    assertFalse(testResult);
  }

}
