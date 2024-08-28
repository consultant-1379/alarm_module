package com.distocraft.dc5000.etl.alarm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import ssc.rockfactory.RockFactory;

import com.distocraft.dc5000.common.StaticProperties;
import com.distocraft.dc5000.etl.engine.common.EngineConstants;
import com.distocraft.dc5000.etl.engine.common.EngineException;
import com.distocraft.dc5000.etl.engine.common.EngineMetaDataException;
import com.distocraft.dc5000.etl.engine.common.SetContext;
import com.distocraft.dc5000.etl.engine.structure.TransferActionBase;
import com.distocraft.dc5000.etl.rock.Meta_collections;
import com.distocraft.dc5000.etl.rock.Meta_transfer_actions;
import com.distocraft.dc5000.etl.rock.Meta_versions;
import com.ericsson.eniq.etl.alarm.AlarmConfig;
import com.ericsson.eniq.etl.alarm.CachedAlarmInterface;
import com.ericsson.eniq.etl.alarm.CachedAlarmReport;

/**
 * AlarmHandlerAction executes alarm reports specified in AlarmReport table by using connection API with specified URL
 * from Alarmcfg. Results are stored into alarm parser in-directory. <br>
 * <br>
 * Copyright (c) 1999 - 2010 AB LM Ericsson Oy All rights reserved.
 * 
 * @author ejannbe, eheijun
 * 
 */
public class AlarmHandlerAction extends TransferActionBase {

	private static final String ALARM_CONFIG = "alarmConfig";

	private final Logger log;

	private Properties actionContents;

	private Map<String, String> reportsContents = Collections.synchronizedMap(new HashMap<String, String>());

	private final String rowstatus;

	private String skipExecution = "";

	private AlarmConfig alarmConfig;

	private Properties schedulingInfo;

	boolean foundMatch = false;

	public AlarmHandlerAction(final Meta_versions version, final Long collectionSetId,
			final Meta_collections collection, final RockFactory rockFact, final Meta_transfer_actions trActions,
			final SetContext setcontext, final Logger log) throws EngineMetaDataException {

		this.log = Logger.getLogger(log.getName() + ".AlarmHandlerAction");

		if (setcontext == null) {
			throw new EngineMetaDataException("Set context haven't been set", new Throwable(), "AlarmHanderAction");
		}

		if (setcontext.containsKey("rowstatus")) {
			if (setcontext.get("rowstatus") != null) {
				this.rowstatus = (String) setcontext.get("rowstatus");
			} else {
				this.rowstatus = "";
			}
		} else {
			this.rowstatus = "";
		}

		if (setcontext.containsKey("skipExecution")) {
			if (setcontext.get("skipExecution") != null) {
				this.skipExecution = (String) setcontext.get("skipExecution");
			}
		}

		// Check if the execution of this action should be skipped.
		if (this.skipExecution.equalsIgnoreCase("true")) {
			this.log.info("Skipping execution of AlarmHandlerAction.");
			return;
		}

		this.log.log(Level.FINE, "Starting AlarmHandlerAction.");

		if (trActions == null) {
			throw new EngineMetaDataException("Meta_transfer_actions is not available", new Throwable(),
					"AlarmHanderAction");
		}
		final String act_cont = trActions.getAction_contents();

		this.actionContents = new Properties();

		if ((act_cont != null) && !act_cont.isEmpty()) {
			try {
				final ByteArrayInputStream bais = new ByteArrayInputStream(act_cont.getBytes());
				actionContents.load(bais);
				bais.close();
				log.finest("ActionContent configuration read");
			} catch (final Exception e) {
				this.log.severe("AlarmHandlerAction constructor. Error reading ActionContent configuration.");
			}
		}

		if (setcontext.containsKey(ALARM_CONFIG)) {
			if (setcontext.get(ALARM_CONFIG) != null) {
				alarmConfig = (AlarmConfig) setcontext.get(ALARM_CONFIG);
			} else {
				alarmConfig = null;
			}
		} else {
			alarmConfig = null;
		}

		final String sch_info = collection.getScheduling_info();

		this.schedulingInfo = new Properties();

		if ((sch_info != null) && !sch_info.isEmpty()) {
			try {
				final ByteArrayInputStream bais = new ByteArrayInputStream(sch_info.getBytes());
				schedulingInfo.load(bais);
				bais.close();
				log.finest("SchedulingInfo configuration read");
			} catch (final Exception e) {
				this.log.severe("AlarmHandlerAction constructor. Error reading SchedulingInfo configuration.");
			}
		}

	}

	@Override
	public void execute() throws EngineException {

		if (this.skipExecution.equalsIgnoreCase("true")) {
			// Don't do anything during execution call.
			this.log.finest("Skipping execution of AlarmHandlerAction.execute().");
			return;
		}

		// CR-118 -- Starts--
		final File inputFile = new File("/eniq/sw/runtime/tomcat/webapps/alarmcfg/WEB-INF/web.xml");
		try {
			final FileInputStream fstream = new FileInputStream(inputFile);
			final DataInputStream in = new DataInputStream(fstream);
			final BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while (((strLine = br.readLine()) != null) && !foundMatch) {
				if (strLine.contains("CONFIDENTIAL")) {
					foundMatch = true;
					break;
				}
			}
			in.close();
			fstream.close();

		} catch (final Exception e) {
			log.finest("Exception Occured while checking web.xml file : " + e.getMessage());
		}

		String protocol = actionContents.getProperty("protocol", "http");
		String hostname = actionContents.getProperty("hostname", "localhost:8080");
		final String cms = actionContents.getProperty("cms", "webportal:6400");
		final String username = actionContents.getProperty("username", "eniq_alarm");
		final String password = actionContents.getProperty("password", "eniq_alarm");
		final String auth = actionContents.getProperty("authmethod", "secEnterprise");
		final String outputPath = actionContents.getProperty("outputPath", "");
		final String interfaceId = actionContents.getProperty("interfaceId", "");
		final Integer maxSimulDownloads = new Integer(actionContents.getProperty("maxSimulDownloads", "5"));

		if (foundMatch) {
			protocol = "https";
			hostname = "webserver:8443";
		}

		// log.finest("Protocol :" + protocol + " and Hostname :" + hostname);
		// CR-118 --End--

		// timeout can be configured in StaticProperties
		long timeoutTemp = 180 * 1000;
		try {
			if (actionContents.getProperty("alarmTimeout") != null) {
				timeoutTemp = Long.parseLong(actionContents.getProperty("alarmTimeout")) * 1000;
			} else {
				timeoutTemp = Long.parseLong(StaticProperties.getProperty("alarmTimeout", "180")) * 1000;
			}
		} catch (final NumberFormatException e) {
			this.log.log(Level.FINEST, "StaticProperties timeout is not number format."
					+ " Using default timeout 180*1000 seconds");
		} catch (final Exception e) {
			this.log.log(Level.FINEST, "Timeout parameter not defined." + " Using default timeout 180*1000 seconds");
		}
		final long timeout = timeoutTemp;

		log.finest("AlarmHandlerAction");
		log.finest("interfaceId:" + interfaceId);
		log.finest("outputPath:" + outputPath);
		log.finest("protocol:" + protocol);
		log.finest("hostname:" + hostname);
		log.finest("cms:" + cms);
		log.finest("username:" + username);
		log.finest("password:" + (password != null ? "********" : "null"));
		log.finest("auth:" + auth);
		log.finest("timeout:" + timeout);
		log.finest("Maximum amount of simultaneous report downloads is " + maxSimulDownloads);

		// Sanity check
		if ((outputPath == null) || (protocol == null) || (hostname == null) || (cms == null) || (username == null)
				|| (password == null) || (auth == null) || (interfaceId == null) || (maxSimulDownloads == null)) {
			log.severe("AlarmHandlerAction.execute: Insufficient configuration. Exiting...");
			return;
		}

		//this.reportsContents = new HashMap<String, String>();
				//Sychronizing Map for TR HV21631
				this.reportsContents = Collections.synchronizedMap(new HashMap<String, String>());



		// Get the AlarmInterface identified by the parameter interfaceId.
		final CachedAlarmInterface targetAlarmInterface = this.alarmConfig.getAlarmInterfaceById(interfaceId);

		if (targetAlarmInterface != null) {

			final List<CachedAlarmReport> alarmReports = targetAlarmInterface.getAlarmReports();

			if (alarmReports == null) {
				throw new EngineException("Failed to load cached alarm reports", new Throwable(), this, this.getClass()
						.getName(), EngineConstants.ERR_TYPE_EXECUTION);
			}

			if (targetAlarmInterface.isScheduled()) {

				this.log.log(Level.FINE,
						"Scheduled interfaceId is " + interfaceId + " and it returned " + alarmReports.size()
								+ " reports.");

				final Map<CachedAlarmReport, String> reportBasetables = new HashMap<CachedAlarmReport, String>();

				// Get the names of the basetables these reports use.
				for (final CachedAlarmReport currentAlarmReport : alarmReports) {
					reportBasetables.put(currentAlarmReport, currentAlarmReport.getBaseTableName());
				}

				final String dotAlarm = System.getProperty(".alarm", ".alarm");
				final String lockedBasetablesFilepath = System.getProperty("CONF_DIR") + File.separator + dotAlarm
						+ File.separator + targetAlarmInterface.getInterfaceId() + "_lockedbasetables.state";

				final File lockedBasetablesFile = new File(lockedBasetablesFilepath);
				Properties lockedBasetables = new Properties();

				try {
					lockedBasetables = AlarmMarkupAction.loadLockedBasetablesFile(lockedBasetablesFile, this.log);
				} catch (final EngineMetaDataException e) {
					throw new EngineException("Failed to load locked basetables file.", new String[] { "" }, e, this,
							this.getClass().getName(), EngineConstants.ERR_TYPE_EXECUTION);
				}
				if (lockedBasetables == null) {
					throw new EngineException("Locked basetables are not available.", new String[] { "" },
							new Throwable(), this, this.getClass().getName(), EngineConstants.ERR_TYPE_EXECUTION);
				}

				Set<Object> lockedBasetablesKeySet = lockedBasetables.keySet();
				Iterator<Object> lockedBasetablesIterator = lockedBasetablesKeySet.iterator();
				final Vector<String> removeList = new Vector<String>();
				Integer boundReportsCounter = 0;
				Integer latchCount = 0;

				// First check the amount of alarm reports that should be downloaded and set the latchcount.
				while (lockedBasetablesIterator.hasNext()) {
					final String currentBasetable = (String) lockedBasetablesIterator.next();
					final String currentBasetableStatus = lockedBasetables.getProperty(currentBasetable);
					if (currentBasetableStatus.equalsIgnoreCase("USED")) {
						if (reportBasetables.containsValue(currentBasetable)) {
							for (final Entry<CachedAlarmReport, String> entry : reportBasetables.entrySet()) {
								final String currentAlarmReportBasetable = entry.getValue();
								// Check if this alarm report uses the currently iterated basetable.
								if (currentBasetable.equalsIgnoreCase(currentAlarmReportBasetable)) {
									latchCount += 1;
								}
							}
						}
					}
				}

				lockedBasetablesKeySet = lockedBasetables.keySet();
				lockedBasetablesIterator = lockedBasetablesKeySet.iterator();
				final CountDownLatch boundReportsLatch = new CountDownLatch(latchCount);
				this.log.log(Level.FINEST, "Latch created for bound reports with latch count " + latchCount);
				final Semaphore sem = new Semaphore(maxSimulDownloads, true);
				final AlarmReportDownloadThread threads[] = new AlarmReportDownloadThread[latchCount];

				// First check alarm reports should be downloaded and set the
				// latchcount.
				while (lockedBasetablesIterator.hasNext()) {
					final String currentBasetable = (String) lockedBasetablesIterator.next();
					final String currentBasetableStatus = lockedBasetables.getProperty(currentBasetable);
					if (currentBasetableStatus.equalsIgnoreCase("USED")) {
						if (reportBasetables.containsValue(currentBasetable)) {
							for (final Entry<CachedAlarmReport, String> entry : reportBasetables.entrySet()) {
								final CachedAlarmReport currentAlarmReport = entry.getKey();
								final String currentAlarmReportBasetable = entry.getValue();
								if (currentBasetable.equalsIgnoreCase(currentAlarmReportBasetable)) {
									// Ok, this alarm report uses this basetable. Load the report contents from the
									// alarmcfg.
									final String reportUrl = createAlarmReportUrl(currentAlarmReport);
									threads[boundReportsCounter] = new AlarmReportDownloadThread(boundReportsCounter,
											sem, boundReportsLatch, currentAlarmReport, reportUrl, protocol, hostname,
											cms, username, password, timeout, auth, log, reportsContents);
									threads[boundReportsCounter].start();
									boundReportsCounter = boundReportsCounter + 1;
								}
							}
						}
					}

					// Add the basetable to a list for removal after while loop.
					removeList.add(currentBasetable);
				}

				for (int i = 0; i < removeList.size(); i++) {
					// Remove the USED from the lockedBasetables Properties object.
					lockedBasetables.remove(removeList.get(i));
				}

				// Wait for all other threads to finish before continuing.
				try {
					this.log.log(Level.FINEST,
							"AlarmHandlerAction waiting for threads to finish downloading boundreports.");
					boundReportsLatch.await();
					this.log.log(Level.FINEST, "Threads have finished downloading boundreports.");
					this.log.log(Level.FINEST, "Reportcontents contains " + reportsContents.size() + " entries.");
					


				} catch (final Exception e) {
					this.log.log(Level.SEVERE,
							"Exception occurred while awaiting AlarmReportDownloadThreads to finish.", e);
				}

				try {
					AlarmMarkupAction.saveLockedBasetablesFile(lockedBasetablesFile, lockedBasetables, this.log);
				} catch (final EngineMetaDataException e) {
					throw new EngineException("Failed to save locked basetables file.", new String[] { "" }, e, this,
							this.getClass().getName(), EngineConstants.ERR_TYPE_EXECUTION);
				}

			} else {
				// reduced delay alarms

				final String setName = schedulingInfo.getProperty("setName", "");
				final String setType = schedulingInfo.getProperty("setType", "");
				final String baseTable = schedulingInfo.getProperty("setBaseTable", "");

				log.finest("setName:" + setName);
				log.finest("setType:" + setType);
				log.finest("baseTable:" + baseTable);

				final List<CachedAlarmReport> reducedDelayReports = new ArrayList<CachedAlarmReport>();

				for (final CachedAlarmReport currentAlarmReport : alarmReports) {
					if (baseTable.equalsIgnoreCase(currentAlarmReport.getBaseTableName())) {
						reducedDelayReports.add(currentAlarmReport);
					}
				}

				final Integer nbrOfReducedDelayReports = reducedDelayReports.size();

				this.log.log(Level.FINE, "Reduced delay interfaceId is " + interfaceId + " and it returned "
						+ nbrOfReducedDelayReports + " reports.");

				if (nbrOfReducedDelayReports > 0) {
					this.log.log(Level.FINE, "Starting to load reduced delay reports.");
					try {
						final CountDownLatch reducedDelayReportsLatch = new CountDownLatch(nbrOfReducedDelayReports);
						this.log.log(Level.FINEST, "Latch created for reduced delay reports with latch count "
								+ nbrOfReducedDelayReports);
						final Semaphore downloadSemaphore = new Semaphore(maxSimulDownloads, true);
						final AlarmReportDownloadThread downloadThreads[] = new AlarmReportDownloadThread[nbrOfReducedDelayReports];
						int index = 0;
						for (final CachedAlarmReport reducedDelayReport : reducedDelayReports) {
							final String reportUrl = createAlarmReportUrl(reducedDelayReport);
							downloadThreads[index] = new AlarmReportDownloadThread(index, downloadSemaphore,
									reducedDelayReportsLatch, reducedDelayReport, reportUrl, protocol, hostname, cms,
									username, password, timeout, auth, log, reportsContents);
							downloadThreads[index].start();
							index++;
						}
						reducedDelayReportsLatch.await();
						this.log.log(Level.FINEST, "Threads have finished downloading reports.");
						this.log.log(Level.FINEST, "Reportcontents contains " + reportsContents.size() + " entries.");
					} catch (final Exception e) {
						this.log.log(Level.SEVERE,
								"Exception occurred while awaiting AlarmReportDownloadThreads to finish.", e);
					}
				}
			}

		}

		try {
			// Save the reports to outputpath.
			if (!this.saveReportsToOutputpath()) {
				this.log.severe("AlarmHandlerAction.execute saveReportsToOutputpath failed!");
			}
		} catch (final Exception e) {
			this.log.severe("AlarmHandlerAction.execute saveReportsToOutputpath failed. " + e.getMessage());
		}
	}

	/**
	 * This method saves the reportfiles to the specified reportpath.
	 * 
	 * @return Returns true if the reportfiles are successfully saved to outputpath. Otherwise returns false.
	 */
	private boolean saveReportsToOutputpath() {

		String outputPath = actionContents.getProperty("outputPath");

		if (outputPath == null) {
			this.log.severe("AlarmHandlerAction.saveReportsToOutputpath: outputPath is null! Exiting...");
			return false;
		}

		final String outputFilePrefix = actionContents.getProperty("outputFilePrefix");

		// Replace the variable (For example: ${PMDATA_DIR}) with the specified
		// value in niq.rc.
		outputPath = replacePathVariables(outputPath, this.log);

		// Check if the outputfolder exists on the server.
		final File outputDir = new File(outputPath);
		
		if (!outputDir.exists()) {
			this.log.fine("AlarmHandlerAction.saveReportsToOutputpath: outputPath(" + outputPath
					+ ") doesn't exists! Creating outputPath.");
			if (!outputDir.mkdirs()) {
				this.log.severe("AlarmHandlerAction.saveReportsToOutputpath: Unable to create outputPath(" 
						+ outputPath + ")! Exiting...");
				return false;
			}
		}
		else if (!outputDir.isDirectory()) {
			this.log.severe("AlarmHandlerAction.saveReportsToOutputpath: outputPath(" + outputPath
					+ ") is not a directory! Exiting...");
			return false;
		}

		if (outputDir.canWrite()) {
			// Do nothing...
		} else {
			this.log.severe("AlarmHandlerAction.saveReportsToOutputpath: outputPath(" + outputPath
					+ ") cannot be written! Exiting...");
			return false;
		}

		// Make sure the outputPath contains slash-character (/) as it's last
		// character.
		if (outputPath.charAt(outputPath.length() - 1) != '/') {
			outputPath = outputPath + "/";
		}

		if (this.reportsContents.size() == 0) {
			this.log.log(Level.WARNING, "No alarm reports executed at this time.");
		}

		final Set<String> reportNames = this.reportsContents.keySet();
		final Iterator<String> it = reportNames.iterator();

		Long prevTimestamp = 0L;

		while (it.hasNext()) {
			final String currentReportName = it.next();
			final String currentReportContents = this.reportsContents.get(currentReportName);
			final Date currentTime = new Date();
			Long timestamp = Long.valueOf(currentTime.getTime());

			// Make sure that the timestamp is unique for the filename. We don't want
			// to overwrite report files because they are written at the same
			// millisecond!
			if (timestamp.equals(prevTimestamp)) {
				this.log.info("Alarm files were tried to be written at the same millisecond. Fixing timestamp from "
						+ timestamp + " to " + Long.valueOf(timestamp + 1));
				timestamp = timestamp + 1;
			}

			prevTimestamp = timestamp;

			final String timestampString = timestamp.toString();
			final String filename = outputPath + outputFilePrefix + currentReportName + "_" + timestampString;

			try {
				final BufferedWriter out = new BufferedWriter(new FileWriter(filename));
				try {
					out.write(currentReportContents);
					log.fine("AlarmHandlerAction.saveReportsToOutputpath: File " + filename + " written successfully.");
				} catch (final IOException e) {
					log.severe("AlarmHandlerAction.saveReportsToOutputpath: IOException");
					log.severe(e.getMessage());
				} finally {
					out.close();
				}
			} catch (final Exception e) {
				log.severe("AlarmHandlerAction.saveReportsToOutputpath: File output error: " + e.getMessage() + "");
				return false;
			}
		}
		// Reports saved successfully.
		return true;
	}

	/**
	 * This function adds the rowstatus to alarm report's url.
	 * 
	 * @param currentAlarmReport
	 *            Alarmreport object containing the target alarm report.
	 * @return Returns the modified alarm report url.
	 */
	private String createAlarmReportUrl(final CachedAlarmReport currentAlarmReport) {
		String alarmReportUrl = currentAlarmReport.getURL();

		if ((this.rowstatus != null) && !currentAlarmReport.isSimultaneous()) {
			alarmReportUrl += "&promptValue_Row Status:=" + this.rowstatus;
		}

		this.log.log(Level.FINEST, "createAlarmReportUrl returned url " + alarmReportUrl);
		return alarmReportUrl;
	}

	/**
	 * This function replaces path variables like ${PMDATA_DIR} to a real value like /eniq/data/pmdata.
	 * 
	 * @param path
	 *            is the path to use.
	 * @param log
	 *            is the Logger to use.
	 * @return Returns the modified path.
	 * 
	 */
	public static String replacePathVariables(String path, final Logger log) {
		while (path.indexOf("${") >= 0) {
			final int start = path.indexOf("${");
			final int end = path.indexOf("}", start);

			if (end >= 0) {
				final String variable = path.substring(start + 2, end);
				final String val = System.getProperty(variable);
				final String result = path.substring(0, start) + val + path.substring(end + 1);
				log.log(Level.FINEST, "Replaced from " + path + " to " + result);
				path = result;
			}
		}

		return path;
	}

}
