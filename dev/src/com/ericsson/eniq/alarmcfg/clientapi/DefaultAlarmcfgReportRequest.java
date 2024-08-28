package com.ericsson.eniq.alarmcfg.clientapi;

/**
 * This class is passed to connection to define the requested HTML page. This
 * version of DefaultAlarmcfgReportRequest only supports reports.
 * 
 * @author eheijun
 * @copyright Ericsson (c) 2009
 */
public class DefaultAlarmcfgReportRequest implements IAlarmcfgReportRequest {

  /**
   * The name of the report which should be fetched
   */
  private String report;

  /**
   * To set the requested report in create phase. No empty default constructor,
   * used factory does not support this at the moment.
   * 
   * @param report
   */
  protected DefaultAlarmcfgReportRequest(final String report) {
    super();
    this.report = report;
  }

  /**
   * To set the requested report name afterwards. We still support changing the
   * name of the report after it is created.
   * 
   * @param report
   *          the name of the report
   */
  public void setReportName(final String report) {
    this.report = report;
  }

  /**
   * To get the report name.
   * 
   * @return name of the report
   */
  public String getReportName() {
    return report;
  }
}
