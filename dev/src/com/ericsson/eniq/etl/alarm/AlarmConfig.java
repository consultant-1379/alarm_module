/**
 * 
 */
package com.ericsson.eniq.etl.alarm;

import java.util.List;

/**
 * @author eheijun
 *
 */
public interface AlarmConfig {
  
  /**
   * Reload cached data from Dwhrep database
   */
  void reload();

  /**
   * Get all cached alarm interfaces
   * @return list of cached alarm interfaces
   */
  List<CachedAlarmInterface> getAlarmInterfaces();
  
  /**
   * Get all cached alarm report by base table name
   * @return list of cached alarm reports 
   */
  List<CachedAlarmReport> getAlarmReportsByBasetable(String baseTableName);
  
  /**
   * Get cached alarm interface by name
   * @param interfaceId
   * @return
   */
  CachedAlarmInterface getAlarmInterfaceById(String interfaceId);
  
  /**
   * Get cached alarm interface for collection set
   * @param collectionSetId
   * @param collectionId
   * @return
   */
  CachedAlarmInterface getAlarmInterfaceByCollection(Long collectionSetId, Long collectionId);
  
  /**
   * Get cached alarm report by interface id and report id
   * @param interfaceId
   * @param reportId
   * @return cached alarm report
   */
  CachedAlarmReport getAlarmReport(String interfaceId, String reportId);
  
  /**
   * Get cached alarm report by report id
   * @param reportId
   * @return cached alarm report
   */
  CachedAlarmReport getAlarmReportById(String reportId);
  
}
