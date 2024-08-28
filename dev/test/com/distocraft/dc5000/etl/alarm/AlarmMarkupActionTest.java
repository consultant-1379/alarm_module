/**
 * 
 */
package com.distocraft.dc5000.etl.alarm;

import static org.junit.Assert.*;

import com.ericsson.eniq.common.testutilities.MemoryDatabaseUtility;
import java.net.URL;
import java.util.logging.Logger;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ssc.rockfactory.RockFactory;

import com.distocraft.dc5000.etl.engine.common.EngineMetaDataException;
import com.distocraft.dc5000.etl.engine.common.SetContext;
import com.distocraft.dc5000.etl.rock.Meta_collections;
import com.distocraft.dc5000.etl.rock.Meta_collectionsFactory;
import com.distocraft.dc5000.etl.rock.Meta_transfer_actions;
import com.distocraft.dc5000.etl.rock.Meta_transfer_actionsFactory;
import com.ericsson.eniq.alarmcfg.clientapi.AlarmcfgSessionFactory;
import com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgSession;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACSessionException;
import com.ericsson.eniq.etl.alarm.AlarmConfig;
import com.ericsson.eniq.etl.alarm.CachedAlarmInterface;


/**
 * @author eheijun
 *
 */
public class AlarmMarkupActionTest {

  private final Mockery context = new JUnit4Mockery() {

    {
      setImposteriser(ClassImposteriser.INSTANCE);
    }
  };

  public static final String TEST_APPLICATION = AlarmMarkupActionTest.class.getName();
  
  private static RockFactory testEtlRep;

  private static RockFactory testDwhRep;

  private static RockFactory testDwh;

  private static Logger testLogger;

  private Meta_collections reducedDelayAlarmAdapterSet;

  private Meta_transfer_actions alarmMarkupAction;
  
  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    testEtlRep = new RockFactory(MemoryDatabaseUtility.TEST_ETLREP_URL, MemoryDatabaseUtility.TESTDB_USERNAME,
        MemoryDatabaseUtility.TESTDB_PASSWORD, MemoryDatabaseUtility.TESTDB_DRIVER, TEST_APPLICATION, true);
    final URL etlrepsqlurl = ClassLoader.getSystemResource(MemoryDatabaseUtility.TEST_ETLREP_BASIC);
    MemoryDatabaseUtility.loadSetup(testEtlRep, etlrepsqlurl);
    testDwhRep = new RockFactory(MemoryDatabaseUtility.TEST_DWHREP_URL, MemoryDatabaseUtility.TESTDB_USERNAME,
        MemoryDatabaseUtility.TESTDB_PASSWORD, MemoryDatabaseUtility.TESTDB_DRIVER, TEST_APPLICATION, true);
    final URL dwhrepsqlurl = ClassLoader.getSystemResource(MemoryDatabaseUtility.TEST_DWHREP_BASIC);
    MemoryDatabaseUtility.loadSetup(testDwhRep, dwhrepsqlurl);
    testDwh = new RockFactory(MemoryDatabaseUtility.TEST_DWH_URL, MemoryDatabaseUtility.TESTDB_USERNAME,
        MemoryDatabaseUtility.TESTDB_PASSWORD, MemoryDatabaseUtility.TESTDB_DRIVER, TEST_APPLICATION, true);
    final URL dwhsqlurl = ClassLoader.getSystemResource(MemoryDatabaseUtility.TEST_DWH_BASIC);
    MemoryDatabaseUtility.loadSetup(testDwh, dwhsqlurl);
    
    testLogger = Logger.getLogger("TEST");
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    testLogger = null;
    MemoryDatabaseUtility.shutdown(testDwh);
    MemoryDatabaseUtility.shutdown(testDwhRep);
    MemoryDatabaseUtility.shutdown(testEtlRep);
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    final Meta_collections whereCollection = new Meta_collections(testEtlRep);
    whereCollection.setCollection_name("Adapter_AlarmInterface_RD");
    reducedDelayAlarmAdapterSet = (Meta_collections) new Meta_collectionsFactory(testEtlRep, whereCollection).get().firstElement();
    
    final Meta_transfer_actions whereTransferActions = new Meta_transfer_actions(testEtlRep);
    whereTransferActions.setCollection_set_id(reducedDelayAlarmAdapterSet.getCollection_set_id());
    whereTransferActions.setAction_type("AlarmMarkup");
    alarmMarkupAction = (Meta_transfer_actions) new Meta_transfer_actionsFactory(testEtlRep, whereTransferActions).get().firstElement();
    
    final IAlarmcfgSession mockAlarmcfgSession = context.mock(IAlarmcfgSession.class);
    AlarmcfgSessionFactory.setNonDefaultSession(mockAlarmcfgSession);
    
    context.checking(new Expectations() {{
      allowing(mockAlarmcfgSession).close();
    }});
    
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    AlarmcfgSessionFactory.setNonDefaultSession(null);
    reducedDelayAlarmAdapterSet = null;
    alarmMarkupAction = null;
  }

  /**
   * Test method for {@link com.distocraft.dc5000.etl.alarm.AlarmMarkupAction#AlarmMarkupAction(java.lang.Long, com.distocraft.dc5000.etl.rock.Meta_collections, ssc.rockfactory.RockFactory, com.distocraft.dc5000.etl.rock.Meta_transfer_actions, com.distocraft.dc5000.etl.engine.common.SetContext, java.util.logging.Logger)}.
   * @throws EngineMetaDataException 
   * @throws ACSessionException 
   */
  @Test
  public void testAlarmMarkupAction() throws EngineMetaDataException, ACSessionException {
    final SetContext mockSetcontext = context.mock(SetContext.class);
    final AlarmConfig mockAlarmConfig = context.mock(AlarmConfig.class); 
    final CachedAlarmInterface mockedAlarmInteface = context.mock(CachedAlarmInterface.class);
    
    context.checking(new Expectations() {{
      
      allowing(mockSetcontext).containsKey("skipExecution");
      will(returnValue(true));
      allowing(mockSetcontext).get("skipExecution");
      will(returnValue("true"));
      
      allowing(mockSetcontext).containsKey("alarmConfig");
      will(returnValue(true));
      allowing(mockSetcontext).get("alarmConfig");
      will(returnValue(mockAlarmConfig));
      
      allowing(mockAlarmConfig).getAlarmInterfaceByCollection(with(any(Long.class)), with(any(Long.class)));
      will(returnValue(mockedAlarmInteface));
      
      allowing(mockedAlarmInteface).isScheduled();
      will(returnValue(false));
      
    }});
    
    AlarmMarkupAction testObject = new AlarmMarkupAction(reducedDelayAlarmAdapterSet.getCollection_set_id(), reducedDelayAlarmAdapterSet, testEtlRep, alarmMarkupAction, mockSetcontext, testLogger);
    assertNotNull(testObject);
  }

}
