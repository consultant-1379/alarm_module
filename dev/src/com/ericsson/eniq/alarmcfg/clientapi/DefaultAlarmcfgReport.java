package com.ericsson.eniq.alarmcfg.clientapi;

/**
 * This class holds the XML page that has been retrieved. Basically in here is
 * the BO report that has been requested. It is in XML format
 * 
 * @author eheijun
 * @copyright Ericsson (c) 2009
 */
public class DefaultAlarmcfgReport implements IAlarmcfgReport {

  /**
   * Contains name of the report
   */
  private String name;

  /**
   * Contains the actual report (XML document).
   */
  private String xml;

  /**
   * Created only from this package.
   */
  protected DefaultAlarmcfgReport() {
    super();
  }

  /**
   * To get HTML page as it is.
   * 
   * @return retrieved HTML page or null if none
   */
  public String getXML() {
    return xml;
  }

  /**
   * To set XML page.
   * 
   * @param xml
   *          to set XML page
   */
  public void setXML(final String xml) {
    this.xml = xml;
  }

  /**
   * Get the name
   */

  public String getName() {
    return name;
  }

  /**
   * Set the name
   * 
   * @param name
   *          new name
   */
  public void setName(final String name) {
    this.name = name;
  }
}
