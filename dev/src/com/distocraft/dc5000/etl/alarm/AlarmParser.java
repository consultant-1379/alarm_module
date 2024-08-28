package com.distocraft.dc5000.etl.alarm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import ssc.rockfactory.RockFactory;

import com.distocraft.dc5000.etl.parser.Main;
import com.distocraft.dc5000.etl.parser.MeasurementFile;
import com.distocraft.dc5000.etl.parser.Parser;
import com.distocraft.dc5000.etl.parser.SourceFile;
import com.ericsson.eniq.common.DatabaseConnections;

/**
 * This class parses alarm files to measurement data.
 * 
 * @author berggren, lmfakos
 * 
 */
public class AlarmParser extends DefaultHandler implements Parser {

  private SourceFile sourceFile;

  private String dateFormat;

  private String tagID;

  private String periodDuration;

  private String timelevel;

  private Logger log;

  private Map<String, MeasurementFile> eventTimes;

  // ***************** Worker stuff ****************************

  private String techPack;

  private String setType;

  private String setName;

  private int status = 0;

  private Main mainParserObject = null;

  private String workerName = "";
  
  private HashMap<String,ENMServerDetails> ce;
  
  static Thread thread;
  
  /**
   * Parameters for throughput measurement.
   */
  private long parseStartTime;
  private long totalParseTime;
  private long fileSize;
  private int fileCount;

  public void init(final Main main, final String techPack, final String setType, final String setName,
      final String workerName) {
    this.log = Logger.getLogger("etl." + techPack + "." + setType + "." + setName + ".AlarmParser");

    this.mainParserObject = main;
    this.techPack = techPack;
    this.setType = setType;
    this.setName = setName;
    this.status = 1;
    this.workerName = workerName;
  }

  public int status() {
    return status;
  }

  public void run() {

	  try {

		  this.status = 2;
		  SourceFile sf = null;
		  try{
			  ce=CacheENMServerDetails.getInstance(log);

		  }
		  catch(IOException e)
		  {
			  log.info("Exception while reading the ENM server details file:"+e);
		  }

		  parseStartTime = System.currentTimeMillis();
		  while ((sf = mainParserObject.nextSourceFile()) != null) {
			  eventTimes = new HashMap<String, MeasurementFile>();
			  try {
				  fileCount++;
				  fileSize += sf.fileSize();
				  thread=Thread.currentThread();
				  mainParserObject.preParse(sf);
				  parse(sf, techPack, setType, setName);
				  mainParserObject.postParse(sf);
			  } catch (Exception e) {
				  mainParserObject.errorParse(e, sf);
			  } finally {
				  mainParserObject.finallyParse(sf);
			  }
		  }
		  totalParseTime = System.currentTimeMillis() - parseStartTime;
		  if (totalParseTime != 0) {
			  log.info("Parsing Performance :: " + fileCount + " files parsed in " + totalParseTime
  					+ " milliseconds, filesize is " + fileSize + " bytes and throughput : " + (fileSize / totalParseTime)
  					+ " bytes/ms.");
		  }
	  } catch (Exception e) {
		  // Exception catched at top level. No good.
		  log.log(Level.WARNING, "Worker parser failed to exception", e);
	  } finally {
		  this.status = 3;
	  }
  }

  // ***************** Worker stuff ****************************

  /**
   * This method does the parsing of the alarmreportfile.
   * 
   * @param sourceFile
   *          is the sourcefile used to create the measurementfile.
   * @param techPack
   *          is the name of the used techpack.
   * @param setType
   *          is the type of the running set.
   * @param setName
   *          is the name of the running set.
   */
  public void parse(final SourceFile sourceFile, final String techPack, final String setType, final String setName)
      throws Exception {

	  this.sourceFile = sourceFile;
	  MeasurementFile workMeasFile = null;
	  RockFactory dwhdb = null;
    
	  try {
		  dwhdb = DatabaseConnections.getDwhDBConnection();
      
		  this.dateFormat = sourceFile.getProperty("dateformat", "yyyy.MM.dd HH:mm:ss");
		  // Basictest this.dateFormat = "yyyy.MM.dd HH:mm:ss";
		  this.log.finest("dateFormat: " + this.dateFormat);

		  this.tagID = sourceFile.getProperty("tag_id", "alarm");
		  // Basictest this.tagID = "alarm";
		  this.log.finest("tagID: " + this.tagID);

		  this.periodDuration = sourceFile.getProperty("periodDuration", "0");
		  // Basictest this.periodDuration = "0";
		  this.log.finest("periodDuration: " + this.periodDuration);

		  this.timelevel = sourceFile.getProperty("timelevel", "0MIN");
		  // Basictest this.timelevel = "0MIN";
		  this.log.finest("timelevel: " + this.timelevel);

		  final BOXIparser boxiParser = new BOXIparser(dwhdb);
		  boxiParser.log = this.log;
		  boxiParser.dateFormat = this.dateFormat;
		  final InputStreamReader isr = new InputStreamReader(this.sourceFile.getFileInputStream());
		  // Basictest InputStreamReader isr = new InputStreamReader(new
      

		  this.log.fine("Parsing File: " + sourceFile.getName());

		  // Do xml parsing and store data into the parser public interface
		  boxiParser.parse(new InputSource(isr));

		  
		  AlarmThreadHandling threadHandling=AlarmThreadHandling.getinstance();
      
		  for (int i = 0; i < boxiParser.alarms.size(); i++) {

			  final Map<String, String> alarmRow = new HashMap<String, String>();

			  alarmRow.putAll(boxiParser.alarms.get(i));
			  alarmRow.putAll(boxiParser.header);
        
			  log.finest("alarm row information for test"+alarmRow.toString());
			  alarmRow.put("ReportTitle", boxiParser.reportName);
			  this.log.finest("Session ID of the Alarm report  "+boxiParser.reportName);
			  alarmRow.put("AlarmName", boxiParser.alarmName);
			  this.log.finest("Name of the Alarm Report  "+boxiParser.alarmName);
			  log.finest("alarm information for test row "+i+":"+alarmRow.toString());
			  log.finest("alarm information event type for test 1"+alarmRow.get("EventTime"));
        
			  //checking whether it is ENM based alarms or OSSRC based alarms.
			  String OSSType=null;
			  if(ce.containsKey(alarmRow.get("OssName")))
			  {	
				  OSSType="ENM";
				  log.finest("Sending the alarm information for : " + alarmRow.get("OssName") + " to ENM host : " + (ce.get(alarmRow.get("OssName"))).getHost());
				  threadHandling.processMessage(alarmRow,log,ce.get(alarmRow.get("OssName")));
			  }
			  else
			  {
				  OSSType="OSSRC";
			  }
			  if (alarmRow.get("EventTime") != null) {
				  // Select the measurementfile according to the eventtime. We
				  // need different load table files for different times when
				  // alarm was created.
				  final String eventTime = (String) alarmRow.get("EventTime");
         
				  if (!this.eventTimes.containsKey(eventTime)) {
					  this.log.log(Level.FINEST, "Creating the measurementfile for alarms which occurred on " + eventTime);
					  this.eventTimes.put(eventTime, Main.createMeasurementFile(this.sourceFile, this.tagID, this.techPack,
							  this.setType, this.setName, this.workerName, this.log));
				  }
				  // Get the measurementfile for the specific eventtime.
				  workMeasFile = (MeasurementFile) this.eventTimes.get(eventTime);
				  // Set the alarmTemplate and baseDir also to data for the
				  // AlarmTransformation action.
				  final String alarmTemplate = sourceFile.getProperty("AlarmTemplate", "ericsson_template.vm");
				  // Basictest final String alarmTemplate = "ericsson_template.vm";
				  alarmRow.put("AlarmTemplate", alarmTemplate);
				  final String baseDir = sourceFile.getProperty("baseDir", "${PMDATA_DIR}");
				  // Basictest final String baseDir = "${PMDATA_DIR}";
				  alarmRow.put("baseDir", baseDir);
				  alarmRow.put("PERIOD_DURATION", this.periodDuration);
				  alarmRow.put("TIMELEVEL", this.timelevel);
				  alarmRow.put("OSSTYPE",OSSType);
				  this.log.log(Level.FINEST, "Setting alarmRow " + alarmRow.toString());
				  workMeasFile.setData(alarmRow);
				  this.log.log(Level.FINEST, "Saving data for EventTime " + eventTime);
				  try {
					  workMeasFile.saveData();
				  } 
				  catch (Exception e) {
					  this.log.log(Level.WARNING, "Saving data failed for EventTime " + eventTime, e);
					  this.log.log(Level.WARNING, e.getMessage());
					  this.log.log(Level.WARNING, "AlarmRow = " + alarmRow.toString());
				  }
				  // Put the measurementfile back to the hashmap for saving later.
				  this.eventTimes.put(eventTime, workMeasFile);
			  } 
			  else {
				  log.log(Level.WARNING, "EventTime not found for alarm data row. Alarm will not be created.");
			  }
		  }
    
      
   
      this.log.finest("Alarm data rows read.");

      // The whole file is read and parsed. Time to iterate through the
      // different measurementfiles and close (ie. save) them.
      final Set<String> eventTimeKeys = this.eventTimes.keySet();
      final Iterator<String> eventTimeKeysIter = eventTimeKeys.iterator();
      while (eventTimeKeysIter.hasNext()) {
        final String currEventTime = eventTimeKeysIter.next();

        if (currEventTime == null) {
          this.log.log(Level.WARNING, "EventTime was null. Skipping this EventTime.");
        } else {
          this.log.log(Level.FINEST, "Iterating at EventTime " + currEventTime);
          final MeasurementFile currMeasFile = (MeasurementFile) this.eventTimes.get(currEventTime);
          currMeasFile.close();
        }
      }
      
     
    } catch (Exception e) {
      this.log.log(Level.WARNING, "AlarmParser.parse failed.", e);
      this.log.log(Level.WARNING, e.getMessage());
    }
    finally{
    	dwhdb.getConnection().close();
    }
  }

  // main for testing only
  /*
   * public static void main(String[] args) { try { AlarmParser ap = new AlarmParser(); ap.log =
   * Logger.getLogger("etl.AlarmParser"); ap.parse(null, "", "", ""); } catch (Exception e) { e.printStackTrace(); } }
   */
}
