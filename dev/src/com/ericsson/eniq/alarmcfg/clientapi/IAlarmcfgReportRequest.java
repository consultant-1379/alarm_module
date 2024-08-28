package com.ericsson.eniq.alarmcfg.clientapi;

/**
 * This class is report request interface. All requests should implement this.
 * This kind of classes are passed to connection in order to get response class.
 * 
 * @author eheijun
 * @copyright Ericsson (c) 2009
 */
public interface IAlarmcfgReportRequest {

  /**
   * To set requested report name.
   * 
   * @param name
   *          the reportname that is requested
   */
  void setReportName(String name);

  /**
   * To get requested report name.
   * 
   * @return the reportname that is requested
   */
  String getReportName();
}
