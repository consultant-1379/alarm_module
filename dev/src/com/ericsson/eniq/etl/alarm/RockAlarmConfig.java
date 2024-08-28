/**
 * 
 */
package com.ericsson.eniq.etl.alarm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.distocraft.dc5000.repository.dwhrep.Alarminterface;
import com.distocraft.dc5000.repository.dwhrep.AlarminterfaceFactory;
import com.distocraft.dc5000.repository.dwhrep.Alarmreport;
import com.distocraft.dc5000.repository.dwhrep.AlarmreportFactory;
import com.distocraft.dc5000.repository.dwhrep.Alarmreportparameter;
import com.distocraft.dc5000.repository.dwhrep.AlarmreportparameterFactory;
import com.ericsson.eniq.common.Utils;

import ssc.rockfactory.RockFactory;

/**
 * @author eheijun
 *
 */
public class RockAlarmConfig implements AlarmConfig {
  
  private static final String ENIQ_BASETABLE_NAME = "eniqBasetableName";
  
  private static final String ACTIVE_TAG = "active";

  private List<CachedAlarmInterface> alarmInterfaces;

  private Map<String, CachedAlarmReport> alarmReportsById;
  
  private Map<String, List<CachedAlarmReport>> alarmReportsByBasetable;
  
  private final RockFactory dwhrepRock;

  private final Logger logger;
  
  public RockAlarmConfig(final RockFactory dwhrepRock, Logger logger) {
    this.dwhrepRock = dwhrepRock;
    this.logger = logger;
    logger.info("AlarmConfig initialized.");
  }

  /**
   * Sorter for CachedAlarmInterfaces
   */
  private final static Comparator<CachedAlarmInterface> ALARMINTERFACECOMPARATOR = new Comparator<CachedAlarmInterface>() {

    public int compare(final CachedAlarmInterface d1, final CachedAlarmInterface d2) {
      final Long l1 = d1.getQueueNumber();
      final Long l2 = d2.getQueueNumber();
      return l1.compareTo(l2);
    }
  };
  
  /*
   * (non-Javadoc)
   * @see com.ericsson.eniq.etl.alarm.AlarmConfig#reload()
   */
  @Override
	public void reload() {
	  alarmInterfaces = new ArrayList<CachedAlarmInterface>();
	  alarmReportsById = new HashMap<String, CachedAlarmReport>();
    alarmReportsByBasetable = new HashMap<String, List<CachedAlarmReport>>();
    try {
      final Alarminterface whereAlarminterface = new Alarminterface(dwhrepRock);
      whereAlarminterface.setStatus(ACTIVE_TAG);
      final AlarminterfaceFactory alarmInterfaceFactory = new AlarminterfaceFactory(dwhrepRock, whereAlarminterface);
      final List<Alarminterface> alarminterfaceList = alarmInterfaceFactory.get();
      for (Alarminterface alarminterfaceItem : alarminterfaceList) {
        final String interfaceid = alarminterfaceItem.getInterfaceid();
        final CachedAlarmInterface cachedAlarmInterface = new CachedAlarmInterface(interfaceid);
        cachedAlarmInterface.setCollectionSetId(alarminterfaceItem.getCollection_set_id());
        cachedAlarmInterface.setCollectionId(alarminterfaceItem.getCollection_id());
        cachedAlarmInterface.setQueueNumber(alarminterfaceItem.getQueue_number());
        final Alarmreport whereAlarmreport = new Alarmreport(dwhrepRock);
        whereAlarmreport.setInterfaceid(interfaceid);
        final AlarmreportFactory alarmreportFactory = new  AlarmreportFactory(dwhrepRock, whereAlarmreport);
        final List<Alarmreport> alarmreportList = alarmreportFactory.get();
        for (Alarmreport alarmreportItem : alarmreportList) {
          boolean isActive = alarmreportItem.getStatus().equalsIgnoreCase(ACTIVE_TAG);
          if (isActive) {
            final String reportId = alarmreportItem.getReportid();
            final CachedAlarmReport cachedAlarmReport = new CachedAlarmReport(reportId, interfaceid);
            cachedAlarmReport.setReportName(alarmreportItem.getReportname());
            cachedAlarmReport.setURL(alarmreportItem.getUrl());
            cachedAlarmReport.setSimultaneous(Utils.replaceNull(alarmreportItem.getSimultaneous()).intValue() == 1);
            final Alarmreportparameter whereAlarmreportparameter = new Alarmreportparameter(dwhrepRock);
            whereAlarmreportparameter.setReportid(reportId);
            final AlarmreportparameterFactory alarmreportparameterFactory = new  AlarmreportparameterFactory(dwhrepRock, whereAlarmreportparameter);
            final List<Alarmreportparameter> alarmreportparameterList = alarmreportparameterFactory.get();
            for (Alarmreportparameter alarmreportparameterItem : alarmreportparameterList) {
              final String paramName = alarmreportparameterItem.getName();
              final String paramValue = alarmreportparameterItem.getValue();
              if (ENIQ_BASETABLE_NAME.equals(paramName)) {
                cachedAlarmReport.setBaseTableName(paramValue);
                if (alarmReportsByBasetable.containsKey(paramValue)) {
                  alarmReportsByBasetable.get(paramValue).add(cachedAlarmReport);
                } else {
                  final List<CachedAlarmReport> tmp = new ArrayList<CachedAlarmReport>();
                  tmp.add(cachedAlarmReport);
                  alarmReportsByBasetable.put(paramValue, tmp);
                }
              } else {
                final CachedAlarmReportPrompt cachedAlarmReportPrompt = new CachedAlarmReportPrompt();
                cachedAlarmReportPrompt.setName(paramName);
                cachedAlarmReportPrompt.setValue(paramValue);
                cachedAlarmReport.getAlarmReportPrompts().add(cachedAlarmReportPrompt);
              }
            }
            cachedAlarmInterface.addAlarmReport(cachedAlarmReport);
            alarmReportsById.put(reportId, cachedAlarmReport);
          }
        }
        alarmInterfaces.add(cachedAlarmInterface);
      }
      Collections.sort(alarmInterfaces, ALARMINTERFACECOMPARATOR);
      logger.info("AlarmConfig " + this + " reloaded.");
    } catch (Exception e) {
      logger.severe("AlarmConfig loading failed: " + e.getMessage());
      e.printStackTrace();
    }
 }

  /*
   * (non-Javadoc)
   * @see com.ericsson.eniq.etl.alarm.AlarmConfig#getAlarmInterfaces()
   */
  @Override
  public List<CachedAlarmInterface> getAlarmInterfaces() {
    return alarmInterfaces;
  }

  /*
   * (non-Javadoc)
   * @see com.ericsson.eniq.etl.alarm.AlarmConfig#getAlarmInterface(java.lang.String)
   */
  @Override
  public CachedAlarmInterface getAlarmInterfaceById(final String interfaceId) {
    for (CachedAlarmInterface alarmInterface : alarmInterfaces) {
      if (alarmInterface.getInterfaceId().equals(interfaceId)) {
        return alarmInterface;
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * @see com.ericsson.eniq.etl.alarm.AlarmConfig#getAlarmInterfaceByCollection(java.lang.Long, java.lang.Long)
   */
  @Override
  public CachedAlarmInterface getAlarmInterfaceByCollection(Long collectionSetId, Long collectionId) {
    for (CachedAlarmInterface alarmInterface : alarmInterfaces) {
      if (alarmInterface.getCollectionSetId().equals(collectionSetId) && alarmInterface.getCollectionId().equals(collectionId)) {
        return alarmInterface;
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * @see com.ericsson.eniq.etl.alarm.AlarmConfig#getAlarmReport(java.lang.String, java.lang.String)
   */
  @Override
  public CachedAlarmReport getAlarmReport(final String interfaceId, final String reportId) {
    for (CachedAlarmInterface alarmInterface : alarmInterfaces) {
      if (alarmInterface.getInterfaceId().equals(interfaceId)) {
        for (CachedAlarmReport alarmReport : alarmInterface.getAlarmReports()) {
          if (alarmReport.getReportId().equals(reportId)) {
              return alarmReport;
          }
        }
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * @see com.ericsson.eniq.etl.alarm.AlarmConfig#getAlarmReport(java.lang.String)
   */
  @Override
  public CachedAlarmReport getAlarmReportById(final String reportId) {
    return alarmReportsById.get(reportId);
  }

  /*
   * (non-Javadoc)
   * @see com.ericsson.eniq.etl.alarm.AlarmConfig#getAlarmReportsByBasetable(java.lang.String)
   */
  @Override
  public List<CachedAlarmReport> getAlarmReportsByBasetable(final String baseTableName) {
    if (alarmReportsByBasetable == null) {
      logger.severe("AlarmConfiguration  " + this + " not initialized properly");
      return null;
    }
    return alarmReportsByBasetable.get(baseTableName);
  }

}
