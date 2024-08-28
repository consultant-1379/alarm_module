/**
 * 
 */
package com.ericsson.eniq.etl.alarm;

import java.util.List;
import java.util.logging.Logger;

import ssc.rockfactory.RockFactory;

/**
 * @author eheijun
 * 
 */
public class RockAlarmConfigCache {

  private static AlarmConfig _instance;
  
  private RockAlarmConfigCache() {
  }

  /**
   * Initialises instance with new rockFactory 
   * @param dwhrep
   */
  public static void initialize(final RockFactory dwhrep, final Logger logger) {
    if (dwhrep != null) {
      _instance = new RockAlarmConfig(dwhrep, logger);
    } else {
      _instance = null;
    }
  }
  
  /**
   * Revalidates cached alarm configuration
   * @param dwhrep
   */
  public static void revalidate() {
    _instance.reload();
  }

  /**
   * Getter for the AlarmConfig instance
   * @return
   */
  public static AlarmConfig getInstance() {
    return _instance;
  }

  /**
   * Setter for the AlarmConfig instance
   * @param instance
   */
  public static void setInstance(final AlarmConfig instance) {
    _instance = instance;
  }

  /**
   * Checks simultaneus report from cached alarm configuration
   * @param dwhrep
   */
  public static Boolean hasSimultanousReport(final String baseTable) {
    if (_instance != null) {
      final List<CachedAlarmReport> cachedAlarmReports = _instance.getAlarmReportsByBasetable(baseTable);
      if (cachedAlarmReports != null && cachedAlarmReports.size() > 0) {
        for (CachedAlarmReport cachedAlarmReport : cachedAlarmReports) {
          if (cachedAlarmReport.isSimultaneous()) {
            return true;
          }
        }
      }
    }
    return false;
  }

}
