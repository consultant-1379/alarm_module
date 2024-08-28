package com.distocraft.dc5000.etl.alarm;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import ssc.rockfactory.RockFactory;

import com.distocraft.dc5000.etl.engine.common.*;
import com.distocraft.dc5000.etl.engine.executionslots.ExecutionSlot;
import com.distocraft.dc5000.etl.engine.executionslots.ExecutionSlotProfileHandler;
import com.distocraft.dc5000.etl.engine.main.EngineThread;
import com.distocraft.dc5000.etl.engine.structure.TransferActionBase;
import com.distocraft.dc5000.etl.rock.*;
import com.distocraft.dc5000.repository.dwhrep.*;
import com.ericsson.eniq.alarmcfg.clientapi.AlarmcfgSessionFactory;
import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgSession;
import com.ericsson.eniq.etl.alarm.*;

/**
 * AlarmMarkupAction performs some initial tasks before the alarms can be executed. These tasks include updating rowstatuses of basetables used by
 * alarm reports, creating/updating internal alarm reports and their dependencies to basetables. Copyright (c) 1999 - 2007 AB LM Ericsson Oy All
 * rights reserved.
 * 
 * @author ejannbe
 */
public class AlarmMarkupAction extends TransferActionBase {

    public static final String ALARM_CONFIG = "alarmConfig";

    private final Logger log;

    private Long queueNumber = new Long(0);

    private Long nLatestDays = new Long(0);

    private final Properties parameters;

    private final RockFactory etlrepRockFactory;

    private RockFactory dwhrepRockFactory;

    private final Map<String, String> boundReports = new HashMap<String, String>();

    private final Map<String, String> availableBoundReports = new HashMap<String, String>();

    private final Long collectionSetId;

    private final Meta_collections collection;

    private String alarmInterfaceId = null;

    private String rowstatus = "";

    private Connection dwhConnection;

    private String beginStatuses = "";

    private String formattedBeginStatuses = "";

    private final SetContext setContext;

    private static final String USED = "USED";

    private String skipExecution = "";

    private final AlarmConfig alarmConfig;

    /**
     * This function is the constructor of this AlarmMarkupAction class.
     * 
     * @param version
     * @param collectionSetId
     * @param collection
     * @param transferActionId
     * @param transferBatchId
     * @param connectId
     * @param rockFact
     * @param trActions
     * @param setcontext
     * @throws EngineMetaDataException
     */
    public AlarmMarkupAction(final Long collectionSetId, final Meta_collections collection, final RockFactory etlrepRockFactory,
                             final Meta_transfer_actions trActions, final SetContext setcontext, final Logger log) throws EngineMetaDataException {

        this.log = Logger.getLogger(log.getName() + ".AlarmMarkupAction");
        this.log.log(Level.FINE, "Starting AlarmMarkupAction.");

        final String actionContents = trActions.getAction_contents();
        this.setContext = setcontext;

        if (this.setContext.containsKey("skipExecution")) {
            if (this.setContext.get("skipExecution") != null) {
                this.skipExecution = (String) this.setContext.get("skipExecution");
            }
        }

        parameters = new Properties();

        this.collectionSetId = collectionSetId;
        this.collection = collection;

        if ((actionContents != null) && (actionContents.length() > 0)) {
            try {
                final ByteArrayInputStream bais = new ByteArrayInputStream(actionContents.getBytes());
                parameters.load(bais);
                bais.close();
                log.finest("Configuration read");
            } catch (final Exception e) {
                this.log.severe("AlarmMarkupAction constructor. Error reading configuration.");
            }
        }

        if (this.setContext.containsKey(ALARM_CONFIG)) {
            if (this.setContext.get(ALARM_CONFIG) != null) {
                alarmConfig = (AlarmConfig) this.setContext.get(ALARM_CONFIG);
            } else {
                alarmConfig = null;
            }
        } else {
            alarmConfig = null;
        }

        this.log.log(Level.FINE, "Creating database connections.");

        // Save the rockfactory for later usage in execute.
        this.etlrepRockFactory = etlrepRockFactory;
        this.log.log(Level.FINE, "Finished creating database connections.");

        // Check if the execution of this action should be skipped.
        if (this.skipExecution.equalsIgnoreCase("true")) {
            this.log.info("Skipping execution of AlarmMarkupAction.");
            return;
        }

        this.checkAlarmcfgConnection(collectionSetId, collection);

        // Check type of the set (scheduled/reduced delay)
        if (alarmConfig == null) {
            this.log.severe("AlarmMarkupAction constructor. Error reading alarm cache.");
        } else {
            final CachedAlarmInterface alarmInterface = alarmConfig.getAlarmInterfaceByCollection(collection.getCollection_set_id(),
                    collection.getCollection_id());
            // No need to execute markup action for non scheduled interfaces
            if (!alarmInterface.isScheduled()) {
                this.skipExecution = "true";
            }
        }
    }

    /**
     * This function starts the execution of AlarmMarkup action.
     * 
     * @throws EngineException
     */
    @Override
    public void execute() throws EngineException {
        if (this.skipExecution.equalsIgnoreCase("true")) {
            // Don't do anything during execution call.
            this.log.finest("Skipping execution of AlarmMarkupAction.execute().");
        } else {

            try {
                this.nLatestDays = new Long(parameters.getProperty("nLatestDays"));
                this.beginStatuses = new String(parameters.getProperty("beginStatuses")).replaceAll("'", "");

                this.log.log(Level.FINEST, "Parameter nLatestDays = " + this.nLatestDays);
                this.log.log(Level.FINEST, "Parameter beginStatuses = " + this.beginStatuses);

                // Start formatting the beginStatuses to format
                // 'ROWSTATUS_1','ROWSTATUS_2'.
                this.formattedBeginStatuses = "";
                final String[] splittedBeginStatuses = this.beginStatuses.replaceAll(" ", "").split(",");

                for (int i = 0; i < splittedBeginStatuses.length; i++) {
                    if (i > 0) {
                        this.formattedBeginStatuses += ",";
                    }
                    final String currentBeginStatus = splittedBeginStatuses[i];
                    this.formattedBeginStatuses += "'" + currentBeginStatus + "'";
                }

            } catch (final Exception e) {
                throw new EngineException("Getting AlarmMarkup parameters failed. Aborting execution of AlarmMarkup.",
                        new String[] { new String("") }, e, this, this.getClass().getName(), EngineConstants.ERR_TYPE_EXECUTION);
            }

            // Some sanity checking.
            if (this.nLatestDays.longValue() == 0) {
                throw new EngineException("AlarmMarkup parameter nLatestDays is not set. Aborting execution of AlarmMarkup.",
                        new String[] { new String("") }, new Exception(), this, this.getClass().getName(), EngineConstants.ERR_TYPE_EXECUTION);

            }
            if (this.etlrepRockFactory == null) {
                throw new EngineException("AlarmMarkupAction etlrepRockFactory is null! Aborting execution of AlarmMarkup.",
                        new String[] { new String("") }, new Exception(), this, this.getClass().getName(), EngineConstants.ERR_TYPE_EXECUTION);
            }

            // Get the alarm reports of this alarm interface.

            try {
                dwhrepRockFactory = createDwhrepRockFactory();
                dwhConnection = createDwhConnection();
                final CachedAlarmInterface targetAlarmInterface = alarmConfig.getAlarmInterfaceByCollection(collectionSetId,
                        collection.getCollection_id());

                if (targetAlarmInterface == null) {
                    throw new EngineException("Alarminterface not found where COLLECTION ID = " + this.collection.getCollection_id()
                            + " and COLLECTION_SET_ID = " + this.collectionSetId, new String[] { new String("") }, new Exception(), this, this
                            .getClass().getName(), EngineConstants.ERR_TYPE_EXECUTION);
                } else {
                    this.rowstatus = "TBA_" + targetAlarmInterface.getQueueNumber().toString();
                    this.log.log(Level.FINEST, "Using rowstatus " + this.rowstatus);
                    this.log.log(Level.FINER, "Alarminterface with INTERFACE_ID = " + targetAlarmInterface.getInterfaceId()
                            + " found where COLLECTION ID = " + this.collection.getCollection_id() + " and COLLECTION_SET_ID = "
                            + this.collectionSetId);
                    this.alarmInterfaceId = targetAlarmInterface.getInterfaceId();
                    this.queueNumber = targetAlarmInterface.getQueueNumber();

                    // Check that the lockedBasetables.state file is writable.
                    if (!this.lockedBasetablesIsWritable(this.alarmInterfaceId)) {
                        this.createLockedBasetablesFile(this.alarmInterfaceId);
                    }
                }

                final List<CachedAlarmReport> alarmReports = targetAlarmInterface.getAlarmReports();

                if (alarmReports.size() == 0) {
                    this.log.info("No AlarmReports found for AlarmInterface " + targetAlarmInterface.getInterfaceId());
                    // Set the skipExecution flag as true so that the rest of the actions of this set are skipped.
                    this.setContext.put("skipExecution", "true");
                    // Clear the lockedbasetables, because this interface cannot lock
                    // anything because there are no reports anymore!
                    final String lockedBasetablesFilepath = System.getProperty("CONF_DIR") + File.separator + ".alarm" + File.separator
                            + this.alarmInterfaceId + "_lockedbasetables.state";
                    final File lockedBasetablesFile = new File(lockedBasetablesFilepath);

                    final boolean savingLockedBTablesFileSuccesfull = saveLockedBasetablesFile(lockedBasetablesFile, new Properties(), this.log);
                    if (savingLockedBTablesFileSuccesfull) {
                        this.log.log(Level.FINEST, "Cleared " + lockedBasetablesFile.getAbsolutePath()
                                + " file successfully. No alarm reports are attached to this AlarmInterface.");
                    } else {
                        throw new EngineMetaDataException("Failed to clear lockedBasetables.state file. Aborting execution of AlarmMarkupAction.",
                                new Exception(), "clearLockedBasetablesList");
                    }

                } else {
                    // Iterate through the AlarmReports and create boundReports
                    for (final CachedAlarmReport currentAlarmReport : alarmReports) {
                        this.boundReports.put(currentAlarmReport.getReportId(), currentAlarmReport.getBaseTableName());
                        if (currentAlarmReport.getBaseTableName().trim().isEmpty()) {
                            // This AlarmReport does not have a eniqBasetableName parameter.
                            this.log.log(Level.SEVERE, "Found unbound report " + currentAlarmReport.getReportId());
                        }
                    }

                    // Get all the basetablenames from tableName.
                    final Share share = Share.instance();
                    this.log.log(Level.FINE, "Trying to get first executionSlotProfile.");

                    if (share.get("executionSlotProfileObject") == null) {
                        throw new EngineException("share.get returned null.", new String[] { new String("") }, new Exception(), this, this.getClass()
                                .getName(), EngineConstants.ERR_TYPE_EXECUTION);
                    }

                    final ExecutionSlotProfileHandler executionSlotProfile = (ExecutionSlotProfileHandler) share.get("executionSlotProfileObject");

                    final Iterator<ExecutionSlot> runningExecutionSlotsIterator = executionSlotProfile.getActiveExecutionProfile()
                            .getAllRunningExecutionSlots();

                    final List currentRunningSetBasetables = new ArrayList();

                    while (runningExecutionSlotsIterator.hasNext()) {
                        final ExecutionSlot currentRunningSet = runningExecutionSlotsIterator.next();

                        if (currentRunningSet.getRunningSet() != null) {
                            if (currentRunningSet.getRunningSet().getSetTables() != null) {
                                currentRunningSetBasetables.addAll(currentRunningSet.getRunningSet().getSetTables());
                            } else {
                                if (currentRunningSet.getName() != null) {
                                    this.log.log(Level.FINE, "currentRunningSet.getRunningSet().getSetTables() returned null. Ignoring set "
                                            + currentRunningSet.getName());
                                } else {
                                    this.log.log(Level.FINE,
                                            "currentRunningSet.getRunningSet().getSetTables() and currentRunningSet.getName() returned null. Ignoring set.");
                                }
                            }
                        } else {
                            if (currentRunningSet.getName() != null) {
                                this.log.log(Level.FINE,
                                        "currentRunningSet.getRunningSet() returned null. Ignoring set " + currentRunningSet.getName());
                            } else {
                                this.log.log(Level.FINE,
                                        "currentRunningSet.getRunningSet() and currentRunningSet.getName() returned null. Ignoring set.");
                            }
                        }
                    }
                    this.log.log(Level.FINE, "Trying to get second executionSlotProfile.");
                    final ExecutionSlotProfileHandler executionSlotHandler = (ExecutionSlotProfileHandler) share.get("executionSlotProfileObject");
                    final EngineThread set = executionSlotHandler.getActiveExecutionProfile().getRunningSet(this.collection.getCollection_name(),
                            collection.getCollection_id().longValue());
                    if (set == null) {
                        this.log.log(Level.SEVERE, "Could not get runningSet.");
                    }
                    // Now removing current set's basetable from this list
                    final List settables = set.getSetTables();
                    if (settables != null) {
                        currentRunningSetBasetables.removeAll(settables);
                    }

                    // Start checking which basetables used by this AlarmMarkup are in use
                    // by some other processes.
                    String alarmReportTableNames = "";
                    final Set<String> boundReportReportIds = this.boundReports.keySet();
                    final Iterator<String> boundReportIdsIterator = boundReportReportIds.iterator();

                    while (boundReportIdsIterator.hasNext()) {
                        final String currentAlarmReportId = boundReportIdsIterator.next();
                        final String bTableNameParamValue = this.boundReports.get(currentAlarmReportId);

                        if (currentRunningSetBasetables.contains(bTableNameParamValue)) {
                            // Basetable used by alarm report is already in use by some other
                            // process.
                            this.log.log(Level.FINE, "Basetable " + bTableNameParamValue + " is used by some other process. Saving "
                                    + bTableNameParamValue + " to lockedBasetables file as locked.");
                            updateLockedBasetablesList(bTableNameParamValue, "LOCKED");

                        } else {
                            // Basetable used by alarm is free to use.
                            this.availableBoundReports.put(currentAlarmReportId, bTableNameParamValue);
                            // Add the available basetable also to a list to be added later to
                            // tableName.
                            this.log.log(Level.FINEST, "Found boundReport " + currentAlarmReportId + " which is bound to " + bTableNameParamValue);
                            alarmReportTableNames += bTableNameParamValue + ",";
                        }
                    }

                    if (alarmReportTableNames.length() > 0) {
                        alarmReportTableNames = alarmReportTableNames.substring(0, (alarmReportTableNames.length() - 1));
                        this.log.log(Level.FINE, "Adding tableNames: " + alarmReportTableNames);
                    }

                    // Iterate through the available boundReports and remove their locked
                    // status.
                    removeLockedBTableFromLockedBTablesList(this.availableBoundReports);

                    final Set<String> availableBoundReportIds = availableBoundReports.keySet();
                    final Iterator<String> availableBoundReportsIterator = availableBoundReportIds.iterator();

                    while (availableBoundReportsIterator.hasNext()) {
                        final String currentAlarmReportId = availableBoundReportsIterator.next();
                        final String currAlarmRepBTable = availableBoundReports.get(currentAlarmReportId);

                        // Find out which partitions should be used when updating rowstatus
                        // of
                        // tables used by alarm report.
                        final List<String> currAlarmRepPartitions = getPartitions(currAlarmRepBTable, this.nLatestDays);

                        if (this.queueNumber.longValue() == 1) {
                            // Case 1 in AlarmMarkup algorithm: Alarm Interface has
                            // queueNumber
                            // value 1.
                            final String previousBasetableStatus = getBTableStatusFromLockedBTablesFile(currAlarmRepBTable);

                            if (previousBasetableStatus.equalsIgnoreCase(AlarmMarkupAction.USED)) {
                                // Don't run the sql update from TBA_1 to PBA_1.
                            } else {
                                // Execute the SQL update from TBA_1 to PBA_1.
                                this.log.log(Level.FINE, "Updating rowstatus from " + this.rowstatus + " to PBA_1");

                                for (int i = 0; i < currAlarmRepPartitions.size(); i++) {
                                    final String currentPartition = currAlarmRepPartitions.get(i);
                                    final Long updateRowcount = updateRowstatus(currentPartition, "'" + this.rowstatus + "'", "PBA_1");

                                    this.log.log(Level.FINEST, "Updated " + updateRowcount.toString() + " rows.");

                                    if (updateRowcount.longValue() == -1) {
                                        this.log.log(Level.SEVERE, "Updating partition " + currentPartition + " failed ");
                                    }
                                }
                            }
                            // Update from BEGIN_STATUSES to TBA_1.
                            this.log.log(Level.FINE, "Updating rowstatus from " + this.beginStatuses + " to " + this.rowstatus);

                            for (int i = 0; i < currAlarmRepPartitions.size(); i++) {
                                final String currentPartition = currAlarmRepPartitions.get(i);
                                final Long updateRowcount = updateRowstatus(currentPartition, this.formattedBeginStatuses, this.rowstatus);

                                if (updateRowcount.longValue() > 0) {
                                    // basetablename to lockedBasetables list.
                                    final boolean updateWasSuccesfull = updateLockedBasetablesList(currAlarmRepBTable, AlarmMarkupAction.USED);

                                    if (!updateWasSuccesfull) {
                                        this.log.log(Level.SEVERE, "Updating lockedBasetables file failed.");
                                    }
                                }
                            }

                        } else {
                            // Case 2 in AlarmMarkup algorithm: Alarm Interface has
                            // queueNumber
                            // value larger than 1.

                            final String previousBasetableStatus = getBTableStatusFromLockedBTablesFile(currAlarmRepBTable);

                            if (previousBasetableStatus.equalsIgnoreCase(AlarmMarkupAction.USED)) {
                                // Do not update from TBA_n to PBA_n.
                            } else {
                                // Execute the SQL update from TBA_n to PBA_n.
                                this.log.log(Level.FINE, "Updating rowstatus from " + this.rowstatus + " to PBA_" + this.queueNumber);

                                for (int i = 0; i < currAlarmRepPartitions.size(); i++) {
                                    final String currentPartition = currAlarmRepPartitions.get(i);
                                    final Long updateRowcount = updateRowstatus(currentPartition, "'" + this.rowstatus + "'", "PBA_"
                                            + this.queueNumber);

                                    this.log.log(Level.FINEST, "Updated " + updateRowcount.toString() + " rows.");

                                    if (updateRowcount.longValue() == -1) {
                                        this.log.log(Level.SEVERE, "Updating partition " + currentPartition + " failed ");
                                    }
                                }
                            }

                            // Update from PBA_n-1 to TBA_n.
                            Long previousQueueNumber = getPreviousQueueNumber(this.queueNumber);
                            this.log.log(Level.FINE, "Updating rowstatus from PBA_" + previousQueueNumber.toString() + " to " + this.rowstatus);
                            for (int i = 0; i < currAlarmRepPartitions.size(); i++) {
                                final String currentPartition = currAlarmRepPartitions.get(i);
                                final Long updateRowcount = updateRowstatus(currentPartition, "'PBA_" + previousQueueNumber.toString() + "'",
                                        this.rowstatus);

                                if (updateRowcount.longValue() > 0) {
                                    // basetablename to lockedBasetables list.
                                    final boolean updateWasSuccesfull = updateLockedBasetablesList(currAlarmRepBTable, AlarmMarkupAction.USED);

                                    if (!updateWasSuccesfull) {
                                        this.log.log(Level.SEVERE, "Updating lockedBasetables file failed.");
                                    }
                                }
                            }

                            // Check if basetablename exists at least in one of previous
                            // [1...n-1] interfaces lockedBasetales lists?
                            boolean prevIntfHasLockedBTable = false;
                            final List<String> prevIntfLockedBTablePathsVec = getPrevIntfLockedBTablesPaths();
                            final Iterator<String> prevIntfLockedBTablePathsIter = prevIntfLockedBTablePathsVec.iterator();
                            while (prevIntfLockedBTablePathsIter.hasNext()) {
                                final String currPrevIntfLockedBTablePath = prevIntfLockedBTablePathsIter.next();
                                final File currPrevIntfLockedBTableFile = new File(currPrevIntfLockedBTablePath);
                                final Properties currPrevIntfLockedBTableProp = loadLockedBasetablesFile(currPrevIntfLockedBTableFile, this.log);

                                if (currPrevIntfLockedBTableProp.containsKey(currAlarmRepBTable)) {
                                    final String previousInterfaceTablenameStatus = currPrevIntfLockedBTableProp.getProperty(currAlarmRepBTable);
                                    this.log.log(Level.FINE, "lockedBasetables file " + currPrevIntfLockedBTablePath + " has locked basetable "
                                            + currAlarmRepBTable + " with status " + previousInterfaceTablenameStatus);
                                    prevIntfHasLockedBTable = true;
                                }
                            }

                            if (!prevIntfHasLockedBTable) {
                                // It's safe to execute update from TBA_n-1 to TBA_n.
                                previousQueueNumber = getPreviousQueueNumber(this.queueNumber);
                                this.log.log(Level.FINE, "Updating rowstatus from TBA_" + previousQueueNumber.toString() + " to " + this.rowstatus);

                                for (int i = 0; i < currAlarmRepPartitions.size(); i++) {
                                    final String currentPartition = currAlarmRepPartitions.get(i);
                                    final Long updateRowcount = updateRowstatus(currentPartition, "'TBA_" + previousQueueNumber.toString() + "'",
                                            this.rowstatus);

                                    if (updateRowcount.longValue() > 0) {
                                        // basetablename to lockedBasetables list.
                                        final boolean updateWasSuccesfull = updateLockedBasetablesList(currAlarmRepBTable, AlarmMarkupAction.USED);

                                        if (!updateWasSuccesfull) {
                                            this.log.log(Level.SEVERE, "Updating lockedBasetables file failed.");
                                        }
                                    }
                                }

                                final boolean prevIntfsUsesBTable = bTableUsedByPrevIntfs(currAlarmRepBTable);

                                if (!prevIntfsUsesBTable) {
                                    // It's also safe to execute update from BEGIN_STATUSES to
                                    // TBA_n.
                                    this.log.log(Level.FINE, "Updating rowstatus from " + this.beginStatuses + " to " + this.rowstatus);
                                    for (int i = 0; i < currAlarmRepPartitions.size(); i++) {
                                        final String currentPartition = currAlarmRepPartitions.get(i);
                                        final Long updateRowcount = updateRowstatus(currentPartition, this.formattedBeginStatuses, this.rowstatus);

                                        if (updateRowcount.longValue() > 0) {
                                            // basetablename to lockedBasetables list.
                                            final boolean updateWasSuccesfull = updateLockedBasetablesList(currAlarmRepBTable, AlarmMarkupAction.USED);

                                            if (!updateWasSuccesfull) {
                                                this.log.log(Level.SEVERE, "Updating lockedBasetables file failed.");
                                            }
                                        }
                                    }
                                }

                                // Also make sure that update is done from PBA_x to TBA_n. For
                                // example, a situation where alarm reports attached to the same
                                // basetable are
                                // used in AlarmInterface_5min and AlarmInterface_15min. This
                                // situation needs and update from PBA_1 to TBA_3.
                                final Long prevQueueNumWithBasetable = getPrevQueueNumWithBasetable(currAlarmRepBTable, this.queueNumber);

                                if (prevQueueNumWithBasetable >= 1) {
                                    this.log.log(Level.FINE, "Previous alarminterface that uses the same basetable has a queueNumber value "
                                            + prevQueueNumWithBasetable.toString() + ". It is safe to update rowstatus from PBA_"
                                            + prevQueueNumWithBasetable.toString() + " to " + this.rowstatus);
                                    this.log.log(Level.FINE, "Updating rowstatus from PBA_" + prevQueueNumWithBasetable.toString() + " to "
                                            + this.rowstatus);

                                    for (int i = 0; i < currAlarmRepPartitions.size(); i++) {
                                        final String currentPartition = currAlarmRepPartitions.get(i);
                                        final Long updateRowcount = updateRowstatus(currentPartition, "'PBA_" + prevQueueNumWithBasetable.toString()
                                                + "'", this.rowstatus);

                                        if (updateRowcount.longValue() > 0) {
                                            // basetablename to lockedBasetables list.
                                            final boolean updateWasSuccesfull = updateLockedBasetablesList(currAlarmRepBTable, AlarmMarkupAction.USED);

                                            if (!updateWasSuccesfull) {
                                                this.log.log(Level.SEVERE, "Updating lockedBasetables file failed.");
                                            }
                                        }
                                    }
                                } else {
                                    this.log.log(Level.FINE, "No previous alarminterface use the same basetable.");
                                }
                            }
                        }
                    }

                    this.setContext.put("rowstatus", this.rowstatus);

                    this.log.log(Level.FINE, "Finished " + this.alarmInterfaceId + ".");

                }

            } catch (final EngineException ee) {
                throw ee;
            } catch (final Exception e) {
                this.log.log(Level.WARNING, "Exception occurred: ", e);
            } finally {
                if (this.dwhConnection != null) {
                    try {
                        this.dwhConnection.close();
                    } catch (final Exception e) {
                    }
                }
                try {
                    if (this.etlrepRockFactory != null) {
                        this.etlrepRockFactory.getConnection().close();
                    }
                } catch (final Exception ex) {

                }
                try {
                    if (this.dwhrepRockFactory != null) {
                        this.dwhrepRockFactory.getConnection().close();
                    }
                } catch (final Exception ex) {

                }
            }
        }
    }

    /**
     * This function creates the RockFactory to dwhrep. The created RockFactory is inserted in class variable dwhrepRockFactory.
     * 
     * @return Returns the dwhrep RockFactory object.
     */
    private RockFactory createDwhrepRockFactory() throws EngineMetaDataException {

        RockFactory dwhrepRockFactory = null;

        try {
            final Meta_databases whereMetaDatabases = new Meta_databases(this.etlrepRockFactory);
            whereMetaDatabases.setConnection_name("dwhrep");
            whereMetaDatabases.setType_name("USER");
            final Meta_databasesFactory metaDatabasesFactory = new Meta_databasesFactory(this.etlrepRockFactory, whereMetaDatabases);
            final Vector<Meta_databases> metaDatabases = metaDatabasesFactory.get();

            if (metaDatabases == null) {
                throw new EngineMetaDataException("Unable to connect metadata database (No dwhrep or multiple dwhreps defined in Meta_databases..)",
                        new Exception(), "createDwhrepRockFactory");
            } else {
                final Meta_databases targetMetaDatabase = metaDatabases.get(0);

                dwhrepRockFactory = new RockFactory(targetMetaDatabase.getConnection_string(), targetMetaDatabase.getUsername(),
                        targetMetaDatabase.getPassword(), etlrepRockFactory.getDriverName(), "AlarmMarkupAction", true);

            }
        } catch (final Exception e) {
            throw new EngineMetaDataException("Creating database connection to dwhrep failed.", e, "createDwhrepRockFactory");
        }

        return dwhrepRockFactory;
    }

    /**
     * This function creates the RockFactory to dwh.
     */
    private Connection createDwhConnection() throws EngineMetaDataException {
        try {
            RockFactory tempRockFactory = null;
            final Meta_databases whereMetaDatabases = new Meta_databases(this.etlrepRockFactory);

            whereMetaDatabases.setConnection_name("dwh");
            whereMetaDatabases.setType_name("USER");
            final Meta_databasesFactory metaDatabasesFactory = new Meta_databasesFactory(this.etlrepRockFactory, whereMetaDatabases);
            final Vector<Meta_databases> metaDatabases = metaDatabasesFactory.get();

            if (metaDatabases == null) {
                throw new EngineMetaDataException("Unable to connect to dwh (No dwh or multiple dwhs defined in Meta_databases.", new Exception(),
                        "createDwhConnection");
            } else {
                final Meta_databases targetMetaDatabase = metaDatabases.get(0);
                tempRockFactory = new RockFactory(targetMetaDatabase.getConnection_string(), targetMetaDatabase.getUsername(),
                        targetMetaDatabase.getPassword(), etlrepRockFactory.getDriverName(), "ActivateInterface", true);
                return tempRockFactory.getConnection();
            }
        } catch (final Exception e) {
            throw new EngineMetaDataException("Creating database connection to dwhrep failed.", e, "createDwhConnection");
        }
    }

    /**
     * This function updates the persistent list lockedBasetables (located in path ${CONF_DIR}/.alarm/${ALARMINTERFACE_NAME}_lockedbasetables.state.
     * 
     * @param basetableName
     *            Name of the target basetable.
     * @param status
     *            Status of the basetable (For example LOCKED, USED etc.)
     * @return Returns true if the lockedBasetables list is updated correctly. Otherwise returns false.
     */
    private boolean updateLockedBasetablesList(final String basetableName, final String status) throws EngineMetaDataException {
        final String lockedBasetablesFilepath = System.getProperty("CONF_DIR") + File.separator + ".alarm" + File.separator + this.alarmInterfaceId
                + "_lockedbasetables.state";
        final File lockedBasetablesFile = new File(lockedBasetablesFilepath);

        // Read up the lockedBasetables.state file, which is in Java Properties
        // format.
        Properties lockedBasetablesProperties = new Properties();

        lockedBasetablesProperties = loadLockedBasetablesFile(lockedBasetablesFile, this.log);

        if (lockedBasetablesProperties == null) {
            throw new EngineMetaDataException("Failed to update lockedBasetables.state file. Aborting execution of AlarmMarkupAction.",
                    new Exception(), "updateLockedBasetablesList");
        }

        // Add the basetable with it's status to lockedBasetables.
        lockedBasetablesProperties.setProperty(basetableName, status);

        final boolean savingLockedBTablesFileSuccesfull = saveLockedBasetablesFile(lockedBasetablesFile, lockedBasetablesProperties, this.log);
        if (savingLockedBTablesFileSuccesfull) {
            this.log.log(Level.FINEST, "Updated lockedBasetables.state file successfully with values " + basetableName + "=" + status);
        } else {
            throw new EngineMetaDataException("Failed to save lockedBasetables.state file. Aborting execution of AlarmMarkupAction.",
                    new Exception(), "updateLockedBasetablesList");
        }

        return true;
    }

    /**
     * This function removes all the basetables given in availableBoundReports with status "LOCKED".
     * 
     * @param availableBoundReports
     *            contains reportId's as keys and basetable's as value.
     * @return Returns true if the removal was succesful. Otherwise returns false.
     */
    private boolean removeLockedBTableFromLockedBTablesList(final Map<String, String> availableBoundReports) throws EngineMetaDataException {
        final String lockedBasetablesFilepath = System.getProperty("CONF_DIR") + File.separator + ".alarm" + File.separator + this.alarmInterfaceId
                + "_lockedbasetables.state";
        final File lockedBasetablesFile = new File(lockedBasetablesFilepath);

        // Read up the lockedBasetables.state file, which is in Java Properties
        // format.
        Properties lockedBasetablesProperties = new Properties();

        lockedBasetablesProperties = loadLockedBasetablesFile(lockedBasetablesFile, this.log);

        if (lockedBasetablesProperties == null) {
            throw new EngineMetaDataException(
                    "Failed to remove unlocked tablenames from lockedBasetables.state file. Aborting execution of AlarmMarkupAction.",
                    new Exception(), "removeLockedBTableFromLockedBTablesList");
        }

        final Enumeration<Object> lockedBasetablesEntries = lockedBasetablesProperties.keys();
        while (lockedBasetablesEntries.hasMoreElements()) {
            final String currLockedBTableName = (String) lockedBasetablesEntries.nextElement();

            if (availableBoundReports.containsValue(currLockedBTableName)) {
                final String currLockedBTableStatus = lockedBasetablesProperties.getProperty(currLockedBTableName);

                if (currLockedBTableStatus.equalsIgnoreCase("LOCKED")) {
                    // Basetable was unlocked since last execution of AlarmMarkupAction.
                    lockedBasetablesProperties.remove(currLockedBTableName);
                }
            }
        }

        final boolean savingLockedBTablesFileSuccesfull = saveLockedBasetablesFile(lockedBasetablesFile, lockedBasetablesProperties, this.log);
        if (!savingLockedBTablesFileSuccesfull) {
            throw new EngineMetaDataException("Failed to save lockedBasetables.state file. Aborting execution of AlarmMarkupAction.",
                    new Exception(), "removeLockedBTableFromLockedBTablesList");
        }

        return true;
    }

    /**
     * This function loads the lockedBasetables.state file.
     * 
     * @param lockedBasetablesFile
     *            is a Java File object containing path to the lockedBasetables.state file.
     * @param log
     *            is the Logger instance where the log is written.
     * @return Returns the Java Properties object containing locked basetables and theis statuses. Returns null in case of error.
     */
    public static Properties loadLockedBasetablesFile(final File lockedBasetablesFile, final Logger log) throws EngineMetaDataException {
        final Properties lockedBasetablesProperties = new Properties();

        try {
            if (lockedBasetablesFile.exists() && lockedBasetablesFile.isFile() && lockedBasetablesFile.canRead()) {
                final FileInputStream fileInputStream = new FileInputStream(lockedBasetablesFile);
                lockedBasetablesProperties.load(fileInputStream);
                fileInputStream.close();
            } else {
                throw new EngineMetaDataException("lockedBasetablesFile " + lockedBasetablesFile.getAbsolutePath()
                        + " does not exist or the file is unreadable.", new Exception(), "loadLockedBasetablesFile");
            }
        } catch (final Exception e) {
            throw new EngineMetaDataException("Failed to load lockedBasetables list file " + lockedBasetablesFile.getAbsolutePath(), e,
                    "loadLockedBasetablesFile");
        }

        return lockedBasetablesProperties;

    }

    /**
     * This function saves the lockedBasetables.state file.
     * 
     * @param lockedBasetablesFile
     *            is a Java File object containing path to the lockedBasetables.state file.
     * @param lockedBasetablesProperties
     *            is a Java Properties object containing values to save to lockedBasetables.state file.
     * @param log
     *            is the Logger instance where the log is written.
     * 
     * @return Returns true if the save was succesfull.
     */
    public static boolean saveLockedBasetablesFile(final File lockedBasetablesFile, final Properties lockedBasetablesProperties, final Logger log)
            throws EngineMetaDataException {
        try {
            // Save the updated lockedBasetables.
            final FileOutputStream fileOutputStream = new FileOutputStream(lockedBasetablesFile);
            lockedBasetablesProperties.store(fileOutputStream, "");
            fileOutputStream.close();
            log.log(Level.FINEST, "Saved " + lockedBasetablesFile.getAbsolutePath() + " with new values.");
        } catch (final Exception e) {
            throw new EngineMetaDataException("Failed to update lockedBasetables list file " + lockedBasetablesFile.getAbsolutePath(), e,
                    "saveLockedBasetablesFile");
        }

        return true;
    }

    /**
     * This function returns the partition tablenames used by a alarm report.
     * 
     * @param currentAlarmReportBasetableName
     *            Basetablename used by alarm report's.
     * @param nLatestDays
     *            Number of days backwards from current time that are taken into account when creating alarms.
     * @return Returns a Vector containing names of partitions used by alarm report.
     */
    private List<String> getPartitions(final String currentAlarmReportBasetableName, final Long nLatestDays) throws EngineMetaDataException {
        final Vector<String> partitions = new Vector<String>();

        try {

            // Start by getting the storageId of this basetable.
            final Dwhtype whereDwhType = new Dwhtype(this.dwhrepRockFactory);
            whereDwhType.setBasetablename(currentAlarmReportBasetableName);
            final DwhtypeFactory dwhTypeFactory = new DwhtypeFactory(this.dwhrepRockFactory, whereDwhType);

            final Vector<Dwhtype> dwhTypes = dwhTypeFactory.get();
            final Iterator<Dwhtype> dwhTypesIterator = dwhTypes.iterator();

            String targetStorageId = "";

            while (dwhTypesIterator.hasNext()) {
                final Dwhtype currentDwhType = dwhTypesIterator.next();
                targetStorageId = currentDwhType.getStorageid();
            }

            // With the storageId get the partitions used by this alarm report's
            // basetable.
            final Dwhpartition whereDwhPartition = new Dwhpartition(this.dwhrepRockFactory);
            whereDwhPartition.setStorageid(targetStorageId);
            final DwhpartitionFactory dwhPartitionFactory = new DwhpartitionFactory(this.dwhrepRockFactory, whereDwhPartition);
            final Vector<Dwhpartition> dwhPartitions = dwhPartitionFactory.get();
            final Iterator<Dwhpartition> dwhPartitionIterator = dwhPartitions.iterator();

            String partitionsString = "";

            while (dwhPartitionIterator.hasNext()) {
                final Dwhpartition currentDwhPartition = dwhPartitionIterator.next();

                if (currentDwhPartition.getStatus().equalsIgnoreCase("ACTIVE")) {
                    final Timestamp currentDwhPartitionEndTime = currentDwhPartition.getEndtime();
                    final Date currentDate = new Date();
                    final Long currentTime = new Long(currentDate.getTime());
                    // One day is 24 hours * 60 minutes * 60 seconds * 1000 milliseconds =
                    // 86400000 ms.
                    final Long nLatestDaysInMilliseconds = new Long(nLatestDays.longValue() * 86400000);

                    // The timestamp to compare with against the partition end time's is
                    // currenttime - nLatestDays.
                    final Timestamp thresholdTimestamp = new Timestamp(currentTime.longValue() - nLatestDaysInMilliseconds.longValue());

                    if (currentDwhPartitionEndTime.after(thresholdTimestamp)) {
                        // Current DwhPartition end later than current time - nLatestDays.
                        // Add
                        // it to partitions Vector.
                        partitions.add(currentDwhPartition.getTablename());
                        partitionsString += currentDwhPartition.getTablename() + ", ";
                    }
                } else {
                    log.log(Level.FINE,
                            "Partition " + currentDwhPartition.getTablename() + " ignored because status is " + currentDwhPartition.getStatus()
                                    + ". Only ACTIVE partitions are used for alarm checking.");
                }

            }

            if (partitionsString.length() > 0) {
                partitionsString = partitionsString.substring(0, (partitionsString.length() - 2));
                this.log.log(Level.FINEST, "Partitions to use for updating are " + partitionsString);
            } else {
                this.log.log(Level.FINEST, "No partitions to use found with nLatestDays value " + nLatestDays.toString());
            }

        } catch (final Exception e) {
            throw new EngineMetaDataException("Failed to get partitions for basetable " + currentAlarmReportBasetableName, e,
                    "saveLockedBasetablesFile");
        }

        return partitions;
    }

    /**
     * This function returns the status of the given basetablename. Returns an empty String if no status is not found from lockedBasetables file.
     * 
     * @param alarmReportBasetableName
     *            Name of the basetable to search for.
     * @return Returns the state of the basetable ("LOCKED", "USED", etc.). Returns an empty String if no status is found from lockedBasetables file.
     */
    private String getBTableStatusFromLockedBTablesFile(final String alarmReportBasetableName) throws EngineMetaDataException {
        String status = "";
        final String lockedBasetablesFilepath = System.getProperty("CONF_DIR") + File.separator + ".alarm" + File.separator + this.alarmInterfaceId
                + "_lockedbasetables.state";
        final File lockedBasetablesFile = new File(lockedBasetablesFilepath);

        Properties lockedBtablesProp = new Properties();

        lockedBtablesProp = loadLockedBasetablesFile(lockedBasetablesFile, this.log);

        if (lockedBtablesProp == null) {
            throw new EngineMetaDataException("Failed to get status of basetable " + alarmReportBasetableName
                    + " from lockedBasetables.state file. Aborting execution of AlarmMarkupAction.", new Exception(),
                    "getBTableStatusFromLockedBTablesFile");

        }

        final Enumeration<Object> lockedBasetablesEntries = lockedBtablesProp.keys();
        while (lockedBasetablesEntries.hasMoreElements()) {
            final String currentLockedBasetableName = (String) lockedBasetablesEntries.nextElement();

            if (alarmReportBasetableName.equals(currentLockedBasetableName)) {
                // Status for this basetable has been found.
                status = lockedBtablesProp.getProperty(alarmReportBasetableName);
                return status;
            }
        }

        // No status found for basetable. Return the empty status string.
        return status;
    }

    /**
     * This function executes update SQL query that updates the rowstatuses to partition tables specified in parameter partitions. Rowstatus is
     * changed from parameter fromRowstatusValue to parameter toRowstatusValue.
     * 
     * @param partitions
     *            Vector containing names of partition tables where the update SQL statement must be executed.
     * @param fromRowstatusValues
     *            Value of rowstatuses from where we are updating from. String must be in the format that sql condition WHERE IN accepts. Example:<br/>
     *            'ROWSTATUS_1', 'ROWSTATUS_2', 'ROWSTATUS_3'<br />
     *            Default value is 'LOADED'
     * @param toRowstatusValue
     *            Value of rowstatus to where we are updating. Do not include '-characters for this parameter. Example:<br/>
     *            PBA_1
     * @return Returns the rowcount affected by the update query. If the update failed, Long with value -1 is returned.
     */
    private Long updateRowstatus(final String partition, final String fromRowstatusValues, final String toRowstatusValue)
            throws EngineMetaDataException {
        Long rowcount = new Long(0);
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = this.dwhConnection.createStatement();
            final String rowcountQuery = "SELECT COUNT (rowstatus) as rowcount FROM " + partition + " WHERE rowstatus IN (" + fromRowstatusValues
                    + ");";

            this.log.log(Level.FINEST, "Executing SQL statement " + rowcountQuery);

            resultSet = statement.executeQuery(rowcountQuery);
            if (resultSet.next()) {
                rowcount = new Long(resultSet.getLong("rowcount"));
            }

            if (rowcount.longValue() > 0) {
                this.log.log(Level.FINEST, "Executing rowstatus update to partition " + partition + " where rowcount is " + rowcount.toString()
                        + " for rowstatuses " + fromRowstatusValues);
                final String updateQuery = "UPDATE " + partition + " SET rowstatus='" + toRowstatusValue + "' WHERE rowstatus IN ("
                        + fromRowstatusValues + ");";
                this.log.log(Level.FINEST, "Executing SQL statement " + updateQuery);
                statement.executeUpdate(updateQuery);

            } else {
                this.log.log(Level.FINEST, "Rowcount was 0 for partition " + partition + ". No rowstatus update was executed.");
            }

        } catch (final Exception e) {
            throw new EngineMetaDataException("Failed to update rowstatus for partition " + partition + " from rowstatuses " + fromRowstatusValues
                    + " to " + toRowstatusValue, e, "updateRowstatus");
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (final Exception e) {

                }
            }

            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (final Exception e) {

                }
            }
        }

        return rowcount;
    }

    /**
     * This function returns absolute file paths to lockedBasetables file paths. File paths are returned in a Java Vector.
     * 
     * @return Returns a Java Vector containing absolute file paths to lockedBasetables files.
     */
    private List<String> getPrevIntfLockedBTablesPaths() throws EngineMetaDataException {
        final List<String> prevIntfLockedBTablesPaths = new ArrayList<String>();

        try {

            final Meta_collections whereMeta_collections = new Meta_collections(this.etlrepRockFactory);
            whereMeta_collections.setSettype("Alarm");
            whereMeta_collections.setEnabled_flag("Y");
            final Meta_collectionsFactory meta_collectionsFactory = new Meta_collectionsFactory(this.etlrepRockFactory, whereMeta_collections);
            final List<Meta_collections> meta_collections = meta_collectionsFactory.get();

            for (final Meta_collections meta_collection : meta_collections) {
                if (meta_collection.getCollection_name().startsWith("Adapter_")) {
                    final CachedAlarmInterface cachedAlarmInterface = alarmConfig.getAlarmInterfaceByCollection(
                            meta_collection.getCollection_set_id(), meta_collection.getCollection_id());
                    if (cachedAlarmInterface.isScheduled()) {
                        final String currentAlarmInterfaceId = cachedAlarmInterface.getInterfaceId();
                        final Long currAlarmIntfQueueNumber = cachedAlarmInterface.getQueueNumber();
                        if (currAlarmIntfQueueNumber.longValue() < this.queueNumber.longValue()) {
                            // This AlarmMarkupAction has smaller queueNumber than this
                            // AlarmMarkupAction. Add the lockedBasetablesFilepath of this Alarm
                            // Interface to the Vector.
                            this.log.log(Level.FINEST, "Added previous Alarm Interface lockedBasetables file path " + System.getProperty("CONF_DIR")
                                    + File.separator + ".alarm" + File.separator + currentAlarmInterfaceId
                                    + "_lockedbasetables.state in function getPreviousInterfacesLockedBasetablesFilepaths.");
                            prevIntfLockedBTablesPaths.add(System.getProperty("CONF_DIR") + File.separator + ".alarm" + File.separator
                                    + currentAlarmInterfaceId + "_lockedbasetables.state");
                        }
                    }
                }
            }

        } catch (final Exception e) {
            throw new EngineMetaDataException("Failed to get previous interfaces locked basetables paths.", e, "getPrevIntfLockedBTablesPaths");
        }

        return prevIntfLockedBTablesPaths;
    }

    /**
     * This function returns the previous Alarm Interface's queue number.
     * 
     * @param queueNumber
     *            is the queue number to compare other queue number's against.
     * @return Returns the previous queue number. Returns -1 if no previous queue number cannot be found or in case of error.
     */
    private Long getPreviousQueueNumber(final Long queueNumber) throws EngineMetaDataException {
        Long previousQueueNumber = new Long(-1);
        try {
            final List<CachedAlarmInterface> alarmInterfaces = alarmConfig.getAlarmInterfaces();
            for (final CachedAlarmInterface alarmInterface : alarmInterfaces) {
                if (!alarmInterface.isScheduled()) {
                    continue;
                }
                if (alarmInterface.getQueueNumber().compareTo(queueNumber) < 0) {
                    if (alarmInterface.getQueueNumber().compareTo(previousQueueNumber) > 0) {
                        previousQueueNumber = alarmInterface.getQueueNumber();
                    }
                }
            }
        } catch (final Exception e) {
            throw new EngineMetaDataException("Getting previous queueNumber failed", e, "getPreviousQueueNumber");
        }
        return previousQueueNumber;
    }

    /**
     * This function returns true if the file lockedBasetables.state file is writable. If the file does not exist or is not writable, this function
     * returns false.
     * 
     * @param interfaceId
     *            InterfaceId of this interface. Used in finding the correct state file.
     * @return Returns true if the file lockedBasetables.state file is writable. Otherwise returns false.
     */
    private boolean lockedBasetablesIsWritable(final String interfaceId) {
        final String lockedBasetablesFilepath = System.getProperty("CONF_DIR") + File.separator + ".alarm" + File.separator + interfaceId
                + "_lockedbasetables.state";
        final File lockedBasetablesFile = new File(lockedBasetablesFilepath);

        if (lockedBasetablesFile.isFile() && lockedBasetablesFile.canRead() && lockedBasetablesFile.canWrite()) {
            return true;
        }
        return false;
    }

    /**
     * This function creates the lockedBasetables.state file. Return true if the creation was succesfull, otherwise returns false.
     * 
     * @param interfaceId
     *            InterfaceId of this interface. Used in finding the correct state file.
     * @return Returns true if the file lockedBasetables.state is created successfully. Otherwise returns false.
     */
    private boolean createLockedBasetablesFile(final String interfaceId) throws EngineMetaDataException {
        final String lockedBasetablesFilepath = System.getProperty("CONF_DIR") + File.separator + ".alarm" + File.separator + interfaceId
                + "_lockedbasetables.state";
        final File lockedBasetablesFile = new File(lockedBasetablesFilepath);
        try {
            lockedBasetablesFile.createNewFile();
            if (lockedBasetablesFile.isFile() && lockedBasetablesFile.canRead() && lockedBasetablesFile.canWrite()) {
                return true;
            }

            return false;

        } catch (final Exception e) {
            throw new EngineMetaDataException("Failed to create file " + lockedBasetablesFile.getAbsolutePath(), e, "getPreviousQueueNumber");
        }
    }

    /**
     * This function checks if alarm reports of previous alarm interfaces (queue number is smaller than this interface) are using the same basetable
     * given as parameter.
     * 
     * @param alarmReportBasetableName
     *            Basetable name to compare previous basetables.
     * @return Returns true if basetable is used by at least one previous alarm interface.
     */
    private boolean bTableUsedByPrevIntfs(final String alarmReportBasetableName) throws EngineMetaDataException {
        try {
            final List<CachedAlarmInterface> cachedAlarmInterfaces = alarmConfig.getAlarmInterfaces();
            for (final CachedAlarmInterface cachedAlarmInterface : cachedAlarmInterfaces) {
                if (cachedAlarmInterface.isScheduled()) {
                    if (cachedAlarmInterface.getQueueNumber().compareTo(this.queueNumber) < 0) {
                        final List<CachedAlarmReport> cachedAlarmReports = cachedAlarmInterface.getAlarmReports();
                        for (final CachedAlarmReport cachedAlarmReport : cachedAlarmReports) {
                            final String currentAlarmReportBasetableName = cachedAlarmReport.getBaseTableName();
                            if (currentAlarmReportBasetableName.equalsIgnoreCase(alarmReportBasetableName)) {
                                this.log.log(Level.FINE, "Previous alarm interface " + cachedAlarmInterface.getInterfaceId() + " report "
                                        + cachedAlarmReport.getReportName() + " uses also basetable " + currentAlarmReportBasetableName);
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (final Exception e) {
            throw new EngineMetaDataException("Failed to check basetables used by previous alarm interfaces.", e, "bTableUsedByPrevIntfs");
        }
        return false;
    }

    /**
     * This function checks that connection to alarmcfg can be established before any rows are updated by AlarmMarkupAction.
     * 
     * @param collectionSetId
     *            Id of the collection set that the actions AlarmMarkup and AlarmHandler are related to.
     * @param collection
     *            Id of the collection that the actions AlarmMarkup and AlarmHandler are related to.
     * @throws EngineMetaDataException
     *             Throws exception if connection to alarmcfg cannot be established.
     */
    private void checkAlarmcfgConnection(final Long collectionSetId, final Meta_collections collection) throws EngineMetaDataException {

        IAlarmcfgSession session = null;

        try {
            final Meta_transfer_actions whereTransferAction = new Meta_transfer_actions(this.etlrepRockFactory);
            whereTransferAction.setCollection_set_id(collectionSetId);
            whereTransferAction.setCollection_id(collection.getCollection_id());
            whereTransferAction.setAction_type("AlarmHandler");
            final Meta_transfer_actionsFactory actionsFactory = new Meta_transfer_actionsFactory(this.etlrepRockFactory, whereTransferAction);
            final Vector<Meta_transfer_actions> actions = actionsFactory.get();

            if (actions.size() > 0) {
                final Meta_transfer_actions targetAlarmHandler = actions.get(0);
                final String actionContents = targetAlarmHandler.getAction_contents();

                final Properties targetAlarmHandlerProp = new Properties();

                if ((actionContents != null) && (actionContents.length() > 0)) {

                    try {
                        final ByteArrayInputStream bais = new ByteArrayInputStream(actionContents.getBytes());
                        targetAlarmHandlerProp.load(bais);
                        bais.close();
                    } catch (final Exception e) {
                        throw new Exception("Error reading AlarmHandler configuration.", e);
                    }
                } else {
                    throw new Exception("No action_contents found for AlarmHandler " + targetAlarmHandler.getTransfer_action_name());
                }

                final String protocol = targetAlarmHandlerProp.getProperty("protocol", "http");
                final String hostname = targetAlarmHandlerProp.getProperty("hostname", "localhost:8080");
                final String cms = targetAlarmHandlerProp.getProperty("cms", "webportal:6400");
                final String username = targetAlarmHandlerProp.getProperty("username", "eniq_alarm");
                final String password = targetAlarmHandlerProp.getProperty("password", "eniq_alarm");
                final String auth = targetAlarmHandlerProp.getProperty("authmethod", "secEnterprise");

                // Test connection to alarmcfg.

                session = AlarmcfgSessionFactory.createSession(protocol, hostname, cms, username, password, auth);
                session.close();
                this.log.log(Level.FINE, "Connection to alarmcfg was checked successfully.");

            } else {
                throw new Exception("Failed to get AlarmHandler action from collection " + collection.getCollection_name());
            }

        } catch (final Exception e) {
            throw new EngineMetaDataException("Failed to create connection to alarmcfg.", e, "checkAlarmcfgConnection");
        }

    }

    /**
     * This function returns the previous queuenumber of an alarminterface that uses the same basetable as alarminterface specified by parameter
     * queueNumber.
     * 
     * @param basetable
     *            Name of the basetable to be used.
     * @param queueNumber
     *            Queuenumber of the alarminterface to search backwards from.
     * @return Returns the previous queuenumber that uses the same basetable.
     */
    Long getPrevQueueNumWithBasetable(final String basetable, final Long queueNumber) throws EngineMetaDataException {

        Long largestQueueNumber = new Long(0);

        try {

            final List<CachedAlarmReport> cachedAlarmReports = alarmConfig.getAlarmReportsByBasetable(basetable);

            // Get all alarm reports and alarm interfaces.
            final HashMap<String, String> alarmReports = new HashMap<String, String>();
            for (final CachedAlarmReport cachedAlarmReport : cachedAlarmReports) {
                if (cachedAlarmReport.isScheduled()) {
                    alarmReports.put(cachedAlarmReport.getReportId(), cachedAlarmReport.getInterfaceId());
                }
            }

            // Get all queue numbers of the scheduled AlarmInterfaces.
            final List<CachedAlarmInterface> cachedAlarmInterfaces = alarmConfig.getAlarmInterfaces();
            final HashMap<String, Long> alarmInterfaces = new HashMap<String, Long>();
            for (final CachedAlarmInterface cachedAlarmInterface : cachedAlarmInterfaces) {
                if (cachedAlarmInterface.isScheduled()) {
                    alarmInterfaces.put(cachedAlarmInterface.getInterfaceId(), cachedAlarmInterface.getQueueNumber());
                }
            }

            for (final Entry<String, String> entry : alarmReports.entrySet()) {
                final String currInterfaceId = entry.getValue();

                // Check if this AlarmInterface has queue number
                if (alarmInterfaces.containsKey(currInterfaceId)) {
                    final Long currQueueNumber = alarmInterfaces.get(currInterfaceId);

                    // Get the largest queuenumber while still smaller than the
                    // queuenumber given as parameter.
                    if ((currQueueNumber < queueNumber) && (largestQueueNumber < currQueueNumber)) {
                        largestQueueNumber = currQueueNumber;
                    }
                }
            }

        } catch (final Exception e) {
            throw new EngineMetaDataException("Getting previous queueNumber with same basetable failed", e, "getPrevQueueNumWithBasetable");
        }

        return largestQueueNumber;
    }
}
