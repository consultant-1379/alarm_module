package com.distocraft.dc5000.etl.alarm;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ssc.rockfactory.RockException;
import ssc.rockfactory.RockFactory;
import utils.MemoryDatabaseUtility;


public class BOXIparserTest {

  private final Mockery context = new JUnit4Mockery() {

    {
      setImposteriser(ClassImposteriser.INSTANCE);
    }
  };
  
  private RockFactory mockFactory;
  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
    mockFactory = context.mock(RockFactory.class);
    
    context.checking(new Expectations() {
      private PreparedStatement mockStatement = context.mock(PreparedStatement.class);

      {
          allowing(mockFactory).createPreparedSqlQuery(with(any(String.class)));
          will(returnValue(mockStatement));
      }
    });
    
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testParse() throws ParserConfigurationException, SAXException, IOException, SQLException, RockException, URISyntaxException {
    
    context.checking(new Expectations() {
      private Vector<Object> queryResults = new Vector<Object>();
      private Integer[] result = new Integer[] { 0 };

      {
          queryResults.add(new Vector<Object>(Arrays.asList(result)));
          allowing(mockFactory).executePreparedSqlQuery(with(any(PreparedStatement.class)), with(any(Vector.class)));
          will(returnValue(queryResults));
      }
    });
    
    BOXIparser boxiParser = new BOXIparser(mockFactory);
    boxiParser.log = Logger.getLogger("TEST");
    boxiParser.dateFormat = "yyyy-MM-dd HH:mm:ss";
    
    final URL url = ClassLoader.getSystemResource("xml/alarm_junit_test.xml");

    boxiParser.parse(new InputSource(url.openStream()));

    assertThat(boxiParser.alarms.size(), not(0));
      
  }

  @Test
  public void testParseDuplicate() throws ParserConfigurationException, SAXException, IOException, SQLException, RockException {
    
    context.checking(new Expectations() {
      private Vector<Object> queryResults = new Vector<Object>();
      private Integer[] result = new Integer[] { 1 };

      {
          queryResults.add(new Vector<Object>(Arrays.asList(result)));
          allowing(mockFactory).executePreparedSqlQuery(with(any(PreparedStatement.class)), with(any(Vector.class)));
          will(returnValue(queryResults));
      }
    });
    
    BOXIparser boxiParser = new BOXIparser(mockFactory);
    boxiParser.log = Logger.getLogger("TEST");
    boxiParser.dateFormat = "yyyy-MM-dd HH:mm:ss";
    
    final URL url = ClassLoader.getSystemResource("xml/alarm_junit_test.xml");

    boxiParser.parse(new InputSource(url.openStream()));

    assertThat(boxiParser.alarms.size(), is(0));
      
  }

}
