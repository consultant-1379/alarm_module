/**
 * 
 */
package com.ericsson.eniq.etl.alarm;

import java.util.ArrayList;
import java.util.List;

/**
 * @author eheijun
 * 
 */
public class CachedAlarmInterface {

  private static final String ALARM_INTERFACE_RD = "AlarmInterface_RD";

  private String interfaceId;

  private Long collectionSetId = 0L;

  private Long collectionId = 0L;

  private Long queueNumber = 0L;

  private List<CachedAlarmReport> alarmReports;

  public CachedAlarmInterface(final String interfaceId) {
    this.interfaceId = interfaceId;
    alarmReports = new ArrayList<CachedAlarmReport>();
  }

  public String getInterfaceId() {
    return interfaceId;
  }

  public List<CachedAlarmReport> getAlarmReports() {
    return alarmReports;
  }

  public void addAlarmReport(final CachedAlarmReport alarmReport) {
    this.alarmReports.add(alarmReport);
  }

  public Long getCollectionSetId() {
    return collectionSetId;
  }

  public void setCollectionSetId(final Long collectionSetId) {
    this.collectionSetId = collectionSetId;
  }

  public Long getCollectionId() {
    return collectionId;
  }

  public void setCollectionId(Long collectionId) {
    this.collectionId = collectionId;
  }

  public Long getQueueNumber() {
    return queueNumber;
  }

  public void setQueueNumber(final Long queueNumber) {
    this.queueNumber = queueNumber;
  }

  public Boolean isScheduled() {
    return !ALARM_INTERFACE_RD.equalsIgnoreCase(interfaceId);
  }

}
