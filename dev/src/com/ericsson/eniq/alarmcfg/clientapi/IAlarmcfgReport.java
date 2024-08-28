package com.ericsson.eniq.alarmcfg.clientapi;

/**
 * This class holds the XML document (report).
 * 
 * @author eheijun
 * @copyright Ericsson (c) 2009
 */
public interface IAlarmcfgReport {

  /**
   * To get XML report as string
   * 
   * @return returns report as a xml string
   */
  String getXML();

  /**
   * To set XML string
   * 
   * @param xml
   */
  void setXML(String xml);

  /**
   * Get name
   * 
   * @param xml
   */
  String getName();

  /**
   * Set the name
   * 
   * @param name
   */
  void setName(String name);

}
