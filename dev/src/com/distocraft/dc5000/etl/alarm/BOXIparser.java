package com.distocraft.dc5000.etl.alarm;

//import java.io.FileInputStream;
import java.io.IOException;
//import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
//import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import ssc.rockfactory.RockException;
import ssc.rockfactory.RockFactory;

import com.distocraft.dc5000.common.HtmlEntities;
//import com.ericsson.eniq.common.DatabaseConnections;

/**
 * This class parses BO XI xml alarm files to measurement data.
 * 
 * @author lmfakos
 * 
 */
public class BOXIparser extends DefaultHandler {

  public String reportName;

  public String alarmName;

  public String dateFormat;

  public ArrayList<String> names; // The measurement column names

  public ArrayList<ArrayList<String>> values; // The measurement column values

  public HashMap<String, String> header; // The alarm header row

  public ArrayList<HashMap<String, String>> alarms; // The parsed valid alarms

  public Logger log;

  private int rowIndex; // Row counter

  private int parserState;

  private RockFactory dwhdb;

  /*
   * State variable: 0 == start 1 == row parsing 2 == row parsing end 3 == marker detected 4 == marker end 5 == header
   * parsing 6 == header parsing end
   */
  private String lineOfText; // Input line

  private boolean collectText; // Combine consecutive char fields

  private String key, value; // Header key and value

  private static final String DUPLICATE_CHECK_SQL = "select count(*) from DC_Z_ALARM_INFO_RAW where DATETIME_ID = ? "
      + "and REPORTTITLE = ? and OSSNAME = ? and OBJECTOFREFERENCE = ? and MANAGEDOBJECTINSTANCE = ? ";

  private PreparedStatement preparedSql;

  public BOXIparser(final RockFactory dwhdb) {
    this.dwhdb = dwhdb;
    try {
      this.preparedSql = this.dwhdb.createPreparedSqlQuery(DUPLICATE_CHECK_SQL);
    } catch (SQLException e) {
      log.warning("Prepared Statment cannot be prepared: " + e.getMessage());
    } catch (RockException e) {
      log.warning("Prepared Statment cannot be prepared: " + e.getMessage());
    }
  }

  // public BOXIparser() {
  // super();
  // }

  // private String printAttributes(Attributes atts) {
  // // Print all the attributes
  // String line = "";
  // int i;
  // if (atts == null) {
  // return "";
  // }
  // for (i = 0; i < atts.getLength(); i++) {
  // line += "[" + atts.getType(i) + "," + atts.getLocalName(i) + "," + atts.getValue(i) + "]";
  // }
  // return line;
  // }

  public void parse(final InputSource br) throws ParserConfigurationException, SAXException, IOException {
    // Initialize and parse xml file
    reportName = "";
    alarmName = "";
    names = new ArrayList<String>();
    values = new ArrayList<ArrayList<String>>();
    header = new HashMap<String, String>();
    alarms = new ArrayList<HashMap<String, String>>();
    rowIndex = 0;
    parserState = 0;
    key = null;
    value = null;
    collectText = false;
    lineOfText = "";
    final long start = System.currentTimeMillis();
    final SAXParserFactory spf = SAXParserFactory.newInstance();
    final SAXParser parser = spf.newSAXParser();
    final XMLReader xmlReader = parser.getXMLReader();
    xmlReader.setContentHandler(this);
    xmlReader.setErrorHandler(this);
    final long middle = System.currentTimeMillis();
    xmlReader.parse(br);
    final long end = System.currentTimeMillis();
    this.log.finer("Data parsed. Parser initialization took " + (middle - start) + " ms, parsing " + (end - middle)
        + " ms. Total: " + (end - start) + " ms.");
  }

  // main method is only for the testing
  /*public static void main(final String[] args) {

    final String DEBUG_FILE = "C:\\temp\\alarm_AM_DC_E_RAN_IULINK_RAW_pmInOutOfSequenceFrames_1288253195662";

    class Helper {

      public HashMap<String, String> alarmRow; // = new HashMap<String, String>();
    }
    ;

    try {
      final InputStreamReader isr = new InputStreamReader(new FileInputStream(DEBUG_FILE));
      final BOXIparser bparser = new BOXIparser(DatabaseConnections.getDwhDBConnection());
      bparser.log = Logger.getLogger(BOXIparser.class.getName());
      bparser.log.setLevel(java.util.logging.Level.FINEST);
      bparser.dateFormat = "yyyy-MM-dd HH:mm:ss";
      bparser.parse(new InputSource(isr));
      final HashMap<String, Helper> eventTimes = new HashMap<String, Helper>();
      for (int i = 0; i < bparser.alarms.size(); ++i) {
        final HashMap<String, String> alarmRow = new HashMap<String, String>();
        alarmRow.putAll(bparser.alarms.get(i));
        alarmRow.putAll(bparser.header);
        alarmRow.put("ReportTitle", bparser.reportName);
        if (alarmRow.get("EventTime") != null) {
          // Select the measurementfile according to the eventtime. We
          // need different load table files for different times when
          // alarm was created.
          final String eventTime = (String) alarmRow.get("EventTime");
          if (!eventTimes.containsKey(eventTime)) {
            final Helper tmp = new Helper();
            eventTimes.put(eventTime, tmp);
          }
          // Get the measurementfile for the specific eventtime.
          final Helper workMeasFile = eventTimes.get(eventTime);
          // Set the alarmTemplate and baseDir also to data for the
          // AlarmTransformation action.
          alarmRow.put("AlarmTemplate", "ericsson_template.vm");
          alarmRow.put("baseDir", "${PMDATA_DIR}");
          alarmRow.put("PERIOD_DURATION", "1");
          alarmRow.put("TIMELEVEL", "2");
          workMeasFile.alarmRow = alarmRow;
          // Put the measurementfile back to the hashmap for saving later.
          eventTimes.put(eventTime, workMeasFile);
        }
      }
      // The whole file is read and parsed. Time to iterate through the
      // different measurementfiles and close (ie. save) them.
      final Set<String> eventTimeKeys = eventTimes.keySet();
      final Iterator<String> eventTimeKeysIter = eventTimeKeys.iterator();
      while (eventTimeKeysIter.hasNext()) {
        final String currEventTime = eventTimeKeysIter.next();
        final Helper currMeasFile = eventTimes.get(currEventTime);
        System.out.println(currMeasFile.alarmRow);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }*/

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException {
    if (!collectText) {
      // Start a new line of input text
      lineOfText = "";
    }
    // Basictest System.out.print("Characters:    \"");
    for (int i = start; i < start + length; i++) {
      /*
       * Basictest switch (ch[i]) { case '\\': System.out.print("\\\\"); break; case '"': System.out.print("\\\"");
       * break; case '\n': System.out.print("\\n"); break; case '\r': System.out.print("\\r"); break; case '\t':
       * System.out.print("\\t"); break; default: System.out.print(ch[i]); break; }
       */
      // If no control char add to the line
      if (ch[i] != '\\' && ch[i] != '\n' && ch[i] != '\r' && ch[i] != '\t') {
        lineOfText += ch[i];
      }
    }
    if (lineOfText.indexOf("DCDATA_START") > -1 && parserState == 2) {
      // Set the marker flag found state
      parserState = 3;
    }
    // Combine consecutive character fields
    collectText = true;
    // Basictest System.out.print("\"\n");
  }

  @Override
  public void endDocument() throws SAXException {
    collectText = false;
    // Do some post-processing
    if (header.containsKey("CurrentTime")) {
      final String dateValue = header.get("CurrentTime");
      if (dateValue != null) {
        header.put("CurrentTime", formatDate(dateValue));
      }
    }
    // Create alarms list
    int i, j;
    for (i = 0; i < values.size(); i++) {
      final HashMap<String, String> oneRow = new HashMap<String, String>();
      for (j = 0; j < names.size(); j++) {
        try {
          if (names.size() > j) {
            final String name = names.get(j);
            if (values.get(i).size() > j) {
              final String value = values.get(i).get(j);
              oneRow.put(name, value);
            } else {
              oneRow.put(name, "");
            }
          } else {
            oneRow.put(Integer.toString(j), "");
          }
        } catch (Exception e) {
          this.log.info("Alarm row ERROR in (" + i + "," + j + "): " + e);
          e.printStackTrace();
        }
      }

      final String alarmCriteria = oneRow.get("AlarmCriteria");
      final String eventTime = oneRow.get("EventTime");

      boolean skip = false;

      if (eventTime.equals("")) {
        // Rows without date_id are invalid
        skip = true;
      } else if (!alarmCriteria.equals("1")) {
        // Only rows where alarmCriteria is "1" are needed
        skip = true;
      } else if (duplicateCheck(oneRow)) {
        // Only rows which are not alarmed are needed
        skip = true;
      }

      if (!skip) {
        alarms.add(oneRow);
      }

      // debug stuff
      // if (Level.FINEST.equals(this.log.getLevel())) {
      if (this.log.isLoggable(Level.FINEST)) {
        String logrow = "";
        for (final Iterator<String> iter = oneRow.keySet().iterator(); iter.hasNext();) {
          final String key = iter.next();
          final String value = oneRow.get(key);
          if (logrow.length() > 0) {
            logrow += "; ";
          }
          logrow += key + "=" + value;
        }
        this.log.finest((skip ? "skipped" : "alarmed") + " " + logrow);
      }

    }
  }

  @Override
  public void endElement(final String uri, final String name, final String qName) throws SAXException {
    collectText = false;
    /*
     * Basictest if ("".equals (uri)) System.out.println("End element (" + parserState + "): " + qName); else
     * System.out.println("End element (" + parserState + "):   {" + uri + "}" + name);
     */
    if (parserState == 1) {
      if (qName.equals("ct")) {
        if (rowIndex == 1) {
          // Add measurement names
          names.add(HtmlEntities.convertHtmlEntities(lineOfText, log));
        } else { // rowIndex > 1
          // Add measurement values
          values.get(rowIndex - 2).add(HtmlEntities.convertHtmlEntities(lineOfText, log));
        }
      } else if (qName.equals("table")) {
        // End rows table
        parserState = 2;
      }
    }
    // parserState == 2 skipped here because it is used in character call back
    else if (parserState == 3) {
      if (qName.equals("ct")) {
        // Marker end
        parserState = 4;
      }
    } else if (parserState == 5) {
      // Collect alarm headers
      if (qName.equals("ct")) {
        if (key == null) {
          // Alarm key field
          key = HtmlEntities.convertHtmlEntities(lineOfText, log);
          value = "";
        } else {
          // Alarm value for key, store into database
          value = HtmlEntities.convertHtmlEntities(lineOfText, log);
          header.put(key, value);
          key = null;
        }
      } else if (qName.equals("table")) {
        // End header table
        parserState = 6;
      }
    } else if (parserState == 6) {
      if (qName.equals("ct")) {
        // The alarm name
        alarmName = HtmlEntities.convertHtmlEntities(lineOfText, log);
      }
    }
  }

  @Override
  public void endPrefixMapping(final String arg0) throws SAXException {
    collectText = false;
    // Basictest System.out.println("endPrefixMapping: " + arg0);
  }

  @Override
  public void ignorableWhitespace(final char[] arg0, final int arg1, final int arg2) throws SAXException {
    collectText = false;
    // Basictest System.out.println("ignorableWhitespace");
  }

  @Override
  public void processingInstruction(final String arg0, final String arg1) throws SAXException {
    collectText = false;
    // Basictest System.out.println("processingInstruction: " + arg0 + ", " +
    // arg1);
  }

  @Override
  public void setDocumentLocator(final Locator arg0) {
    collectText = false;
    // Basictest System.out.println("setDocumentLocator: " + arg0);
  }

  @Override
  public void skippedEntity(final String arg0) throws SAXException {
    collectText = false;
    // Basictest System.out.println("skippedEntity: " + arg0);
  }

  @Override
  public void startDocument() throws SAXException {
    collectText = false;
    // Basictest System.out.println("Start document");
  }

  @Override
  public void startElement(final String uri, final String name, final String qName, final Attributes atts)
      throws SAXException {
    collectText = false;
    /*
     * Basictest if ("".equals (uri)) System.out.println("Start element (" + parserState + "): " + qName +
     * printAttributes(atts)); else System.out.println("Start element (" + parserState + "): {" + uri + "}" + name +
     * printAttributes(atts));
     */
    if (qName.equals("report")) {
      // Get the report name
      if (atts.getLength() > 0) {
        reportName = atts.getValue(0);
      }
    }
    if (qName.equals("tr") && parserState == 1) {
      // Collecting names (tableRow == 1) and values (tableRow > 1) table rows
      rowIndex += 1;
      if (rowIndex > 1) {
        values.add(new ArrayList<String>());
      }
    }
    if (qName.equals("table") && parserState == 0) {
      // Start row parsing
      parserState = 1;
    }
    if (qName.equals("table") && parserState == 4) {
      // Start collecting header
      parserState = 5;
    }
  }

  @Override
  public void startPrefixMapping(final String arg0, final String arg1) throws SAXException {
    collectText = false;
    // Basictest System.out.println("startPrefixMapping: " + arg0 + ", " +
    // arg1);
  }

  /**
   * This method converts datestring from given dateformat to format "yyyy-MM-dd HH:mm:ss".
   * 
   * @param dateString
   *          String to parse date from.
   * @return Returns the converted timestring.
   */
  private String formatDate(final String dateString) {
    try {
      final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
      final Date tempDate = simpleDateFormat.parse(dateString);
      final SimpleDateFormat measurementDataDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      final String parsedDate = measurementDataDateFormat.format(tempDate);
      return parsedDate;
    } catch (ParseException e) {
      this.log.log(Level.WARNING, "Error parsing dateString: " + dateString, e);
      return "";
    }
  }

  /**
   * This checks the DC_Z_ALARM_INFO_RAW view for a duplicate row.
   * 
   * @param alarmRow
   * @return boolean duplicate
   */
  private boolean duplicateCheck(final Map<String, String> alarmRow) {
    boolean duplicate = false;

    try {
      final String datetimeId = alarmRow.get("EventTime");
      final String reportTitle = reportName;
      final String ossName = alarmRow.get("OssName");
      final String objectOfReference = alarmRow.get("ObjectOfReference");
      final String managedObjectInstance = alarmRow.get("ManagedObjectInstance");

      log.finest("Duplicate Check Query:\n " + DUPLICATE_CHECK_SQL);

      final Vector<Object> duplicateCheckSQLParameters = new Vector<Object>();
      duplicateCheckSQLParameters.add(datetimeId);
      duplicateCheckSQLParameters.add(reportTitle);
      duplicateCheckSQLParameters.add(ossName);
      duplicateCheckSQLParameters.add(objectOfReference);
      duplicateCheckSQLParameters.add(managedObjectInstance);

      for (Object parameter : duplicateCheckSQLParameters) {
        try {
          log.finest("With parameter: " + parameter.toString());
        } catch (Exception e) {
          log.warning("Duplicate Check problem with parameter(s): " + e.getMessage());
        }
      }

      try {
        final Vector<Vector<Object>> results = this.dwhdb.executePreparedSqlQuery(this.preparedSql,
            duplicateCheckSQLParameters);
        Integer count = new Integer(results.firstElement().firstElement().toString());
        log.finest("Duplicate Check found " + count + " rows.");
        duplicate = !count.equals(0);
      } catch (Exception e) {
        log.severe("Duplicate check problem " + e.getMessage());
      }
    } catch (Exception e) {
      log.severe("Duplicate check problem " + e.getMessage());
    }
    return duplicate;
  }

}
