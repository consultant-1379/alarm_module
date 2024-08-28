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
public class CachedAlarmReport {

  private String reportId;

  private String interfaceId;

  private String reportName;

  private String url;

  private String baseTableName;

  private Boolean simultaneous;

  private List<CachedAlarmReportPrompt> alarmReportPrompts;
  
  public CachedAlarmReport(final String reportId, final String interfaceId) {
    this.reportId = reportId;
    this.interfaceId = interfaceId;
    this.reportName = "";
    this.url = "";
    this.baseTableName = "";
    this.simultaneous = Boolean.TRUE;
    this.alarmReportPrompts = new ArrayList<CachedAlarmReportPrompt>(); 
  }

  public String getReportId() {
    return reportId;
  }

  public void setReportId(final String reportId) {
    this.reportId = reportId;
  }

  public String getInterfaceId() {
    return interfaceId;
  }
  
  public String getReportName() {
    return reportName;
  }

  public void setReportName(final String reportName) {
    this.reportName = reportName;
  }

  public String getURL() {
    return url;
  }

  public void setURL(final String uRL) {
    url = uRL;
  }

  public String getBaseTableName() {
    return baseTableName;
  }

  public void setBaseTableName(final String baseTableName) {
    this.baseTableName = baseTableName;
  }

  public Boolean isScheduled() {
    return !simultaneous;
  }

  public Boolean isSimultaneous() {
    return simultaneous;
  }

  public void setSimultaneous(final Boolean simultaneous) {
    this.simultaneous = simultaneous;
  }

  public List<CachedAlarmReportPrompt> getAlarmReportPrompts() {
    return alarmReportPrompts;
  }

  public void setAlarmReportPrompts(final List<CachedAlarmReportPrompt> alarmReportPrompts) {
    this.alarmReportPrompts = alarmReportPrompts;
  }

  public void addAlarmReportPrompt(final CachedAlarmReportPrompt alarmReportPrompt) {
    this.alarmReportPrompts.add(alarmReportPrompt);
  }

}
