package com.distocraft.dc5000.etl.alarm;

import com.distocraft.dc5000.common.SessionHandler;
import com.distocraft.dc5000.common.StaticProperties;
import com.distocraft.dc5000.etl.engine.common.EngineCom;
import com.distocraft.dc5000.etl.engine.common.EngineMetaDataException;
import com.distocraft.dc5000.etl.engine.common.SetContext;
import com.distocraft.dc5000.etl.parser.Main;
import com.distocraft.dc5000.etl.parser.ParseSession;
import com.distocraft.dc5000.etl.parser.ParserDebugger;
import com.distocraft.dc5000.etl.parser.SourceFile;
import com.distocraft.dc5000.etl.parser.TransformerCache;
import com.distocraft.dc5000.etl.rock.Meta_collections;
import com.distocraft.dc5000.etl.rock.Meta_transfer_actions;
import com.distocraft.dc5000.etl.rock.Meta_transfer_actionsFactory;
import com.distocraft.dc5000.repository.cache.DataFormatCache;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Ignore;
import org.junit.Test;

import ssc.rockfactory.RockException;
import ssc.rockfactory.RockFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@Ignore("Needs to be refactored for Continuos Integration test")
public class AlarmExecuteTest extends TestCase {
//	private static final String junitDbUrl = "jdbc:sqlite:c:\\alarm_testdb.sqlite";
//	private static final String dbDriver = "org.sqlite.JDBC";
//	private final String junitDbUrl = "jdbc:hsqldb:mem:testdb";
//	private static final String dbDriver = "org.hsqldb.jdbcDriver";
//	
//	private final static String _createStatementMetaFile = "TableCreateStatements.sql";
//	private RockFactory testDB = null;
//	private HttpServer httpServer = null;
//	private final String testReportName = "testReport";
//	private String finalRestingPlace = getTestTmpBaseDir() + "/dc_z_alarm_info/raw/";
//
//	public AlarmExecuteTest() {
//		super("AlarmExecuteTest");
//	}
//
//	@Test
//	public void testAlarmExecute() throws Exception {
//		final AtomicInteger ai = new AtomicInteger(0);
//	//final ConsoleAppender ca = new ConsoleAppender(new SimpleLayout()) {
//			@Override
//			public void doAppend(LoggingEvent loggingEvent) {
//				ai.incrementAndGet();
//				super.doAppend(loggingEvent);
//			}
//		};
//		final Logger log = Logger.getAnonymousLogger();
//		log.setLevel(Level.WARNING);
//		org.apache.log4j.Logger.getRootLogger().addAppender(ca);
//		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.WARN);
//		final String _alarmInterface = "AlarmInterface_15min";
//		final String reportUrl = "TBA_blaablaa";
//		final String alarmDir = getTestTmpBaseDir() + "/AlarmInterface_15min/in/";
//		final String outputFilePrefix = "test_deleteme_";
//
//		Meta_transfer_actions metaActions = null;
//		final SetContext dSet = getSetContext();
//		try {
//			testDB = new RockFactory(junitDbUrl, "SA", "", dbDriver, "con", true, -1);
//			loadUnitDb(testDB, "testSql");
//			metaActions = getAlarmReportExecuteTestAction(_alarmInterface, alarmDir, outputFilePrefix);
//			setupLockFile(_alarmInterface);
//			startTestHttpServer(reportUrl, getSampleReport("eniq_alarm_2.x_template.xml"));
//		} catch (Throwable t) {
//			t.printStackTrace();
//			fail("Failed to setup test data", t.getCause());
//			shutdown(testDB);
//		}
//		final Meta_collections metaCollection = new Meta_collections(testDB);
//		final AlarmHandlerAction alarmExecutorAction = new AlarmHandlerAction(
//			 null, null, metaCollection, testDB, metaActions, dSet, log);
//		alarmExecutorAction.execute();
//		final File[] results = new File(alarmDir).listFiles();
//		assertEquals("There should only be one alarm file", 1, results.length);
//		assertTrue("Report output name doesnt look right", results[0].getName().startsWith(outputFilePrefix + testReportName + "_"));
//
//		final Properties actionProps = getMainParserProperties(testDB, "Parser_AlarmInterface_15min");
//		final List<Class> psParams = new ArrayList<Class>();
//		psParams.add(long.class);
//		psParams.add(Properties.class);
//		final Class psClass = Class.forName("com.distocraft.dc5000.etl.parser.ParseSession");
//		final Constructor psCon = psClass.getDeclaredConstructor(psParams.toArray(new Class[psParams.size()]));
//		psCon.setAccessible(true);
//		final ParseSession pSession = (ParseSession) psCon.newInstance(1, actionProps);
//
//		final List<Class> params = new ArrayList<Class>();
//		params.add(File.class);
//		params.add(Properties.class);
//		params.add(RockFactory.class);
//		params.add(RockFactory.class);
//		params.add(ParseSession.class);
//		params.add(ParserDebugger.class);
//		params.add(String.class);
//		params.add(Logger.class);
//		final Class c = Class.forName("com.distocraft.dc5000.etl.parser.SourceFile");
//		final Constructor con = c.getDeclaredConstructor(params.toArray(new Class[params.size()]));
//		con.setAccessible(true);
//
//		final Logger l = Logger.getLogger("main");
//		final SourceFile sf = (SourceFile) con.newInstance(results[0], actionProps, testDB, testDB, pSession, (ParserDebugger) null, "none", l);
//
//		final String techPackName = "AlarmInterfaces";
//		final String setType = "Alarm";
//		final String setName = "Adapter_" + _alarmInterface;
//		final EngineCom eCom = new EngineCom();
//		final Main mainParser = new Main(actionProps, techPackName, setType, setName, testDB, testDB, eCom) {
//			private boolean done = false;
//
//			@Override
//			public SourceFile nextSourceFile() throws Exception {
//				if (done) {
//					return null;
//				}
//				done = true;
//				return sf;
//			}
//		};
//		System.setProperty("dwhrep.", "");
//		DataFormatCache.initialize(testDB);
//		SessionHandler.init();
//		final TransformerCache tc = new TransformerCache();
//		tc.revalidate(testDB, testDB);
//		mainParser.parse();
//		final File[] parsedAlarms = new File(finalRestingPlace).listFiles();
//		assertEquals("Only one loader file should have been created ", 1, parsedAlarms.length);
//		final BufferedReader br = new BufferedReader(new FileReader(parsedAlarms[0]));
//		String line;
//		final List<String> alarmLine = new ArrayList<String>();
//		while ((line = br.readLine()) != null) {
//			alarmLine.add(line);
//		}
//		assertEquals("Wrong number of alarms were parsed ", 9, alarmLine.size());
//	}
//
//	private String getTestTmpBaseDir() {
//		final File check = new File(getTmpDir() + "/alarmTests/");
//		if (!check.exists()) {
//			if (!check.mkdirs()) {
//				fail("Failed to create test temp directory " + check.getAbsolutePath());
//			}
//		}
//		return check.getAbsolutePath();
//	}
//
//	private Properties getMainParserProperties(final RockFactory etl, final String parserName) throws RockException, SQLException {
//		final Meta_transfer_actions whereClause = new Meta_transfer_actions(etl);
//		whereClause.setTransfer_action_name(parserName);
//		final Meta_transfer_actionsFactory mtFactroy = new Meta_transfer_actionsFactory(etl, whereClause);
//		final Vector v = mtFactroy.get();
//		if (v.isEmpty()) {
//			throw new NullPointerException("No Transfer Action Info found for " + parserName);
//		}
//		final Meta_transfer_actions mta = (Meta_transfer_actions) v.get(0);
//		final Properties properties = new Properties();
//
//		final String act_cont = mta.getAction_contents();
//		if (act_cont != null && act_cont.length() > 0) {
//			try {
//				ByteArrayInputStream bais = new ByteArrayInputStream(act_cont.getBytes());
//				properties.load(bais);
//				bais.close();
//			} catch (Exception e) {
//				System.out.println("Error loading action contents");
//				e.printStackTrace();
//			}
//		}
//		return properties;
//	}
//
//	@Override
//	protected void tearDown() throws Exception {
//		shutdown(testDB);
//		if (httpServer != null) {
//			httpServer.stop(1);
//		}
//		deleteTempDirs();
//	}
//
//	private void deleteTempDirs() {
//		deleteDir(new File(getTestTmpBaseDir()));
//	}
//
//	private void deleteDir(final File dir) {
//		if (!dir.exists()) {
//			return;
//		}
//		final File[] subDirs = dir.listFiles(new FileFilter() {
//			public boolean accept(File pathname) {
//				return pathname.isDirectory();
//			}
//		});
//		for (File sd : subDirs) {
//			deleteDir(sd);
//		}
//		final File[] contents = dir.listFiles();
//		for (File f : contents) {
//			deleteFile(f);
//		}
//		deleteFile(dir);
//	}
//
//	private void deleteFile(final File file) {
//		if (!file.delete()) {
//			System.err.println("Couldn't delete " + file.getAbsoluteFile());
//		}
//	}
//
//	private void mkfile(final String file) throws IOException {
//		final File f = new File(file);
//		if (!f.getParentFile().exists()) {
//			mkdirs(f.getParentFile());
//		}
//		if (!f.createNewFile()) {
//			fail("File already exists " + f.getAbsolutePath());
//		}
//	}
//
//	private void mkdirs(final String dir) {
//		mkdirs(new File(dir));
//	}
//
//	private void mkdirs(final File dir) {
//		if (!dir.exists()) {
//			if (!dir.mkdirs()) {
//				fail("Failed to create " + dir.getAbsolutePath());
//			}
//		}
//	}
//
//	@Override
//	protected void setUp() throws Exception {
//		deleteTempDirs();
//		setRequiredProperties();
//		mkdirs(getTestTmpBaseDir() + "/AlarmInterface_15min/in/");
//		mkdirs(finalRestingPlace);
//		mkdirs(getTestTmpBaseDir() + "/eniq_oss_1/alarmData/");
//		final String alarm_templates = getTestTmpBaseDir() + "/alarm_templates/";
//		mkdirs(alarm_templates);
//
//		//need to copy this to ${CONF_DIR}/alarm_templates...
//		final String et_vm = "ericsson_template.vm";
//		final URL is = ClassLoader.getSystemClassLoader().getResource(et_vm);
//		if (is == null) {
//			fail("Failed to copy alarm template");
//		}
//		final File source = new File(is.toURI().getRawPath());
//		mkfile(alarm_templates + "/" + et_vm);
//		copyFile(source, new File(alarm_templates + "/" + et_vm));
//	}
//
//	private void copyFile(final File source, final File target) throws Exception {
//		final FileChannel sourceFile = new FileInputStream(source).getChannel();
//		final FileChannel targetFile = new FileOutputStream(target, false).getChannel();
//		try {
//			sourceFile.transferTo(0, sourceFile.size(), targetFile);
//		} finally {
//			sourceFile.close();
//			targetFile.close();
//		}
//	}
//
//	private void setRequiredProperties() throws Exception {
//		System.setProperty("PMDATA_DIR", getTestTmpBaseDir());
//		System.setProperty("ETLDATA_DIR", getTestTmpBaseDir());
//		System.setProperty("CONF_DIR", getTestTmpBaseDir());
//		System.setProperty(".alarm", "alarm");
//		final Properties sp = new Properties();
//		final String sp_props = getTestTmpBaseDir() + "/sp.properties";
//		mkfile(sp_props);
//		sp.put("SessionHandling.storageFile", sp_props);
//		sp.put("SessionHandling.log.types", "");
//		StaticProperties.giveProperties(sp);
//	}
//
//	private void startTestHttpServer(final String inUrl, final String outResponse) throws IOException {
//		final InetSocketAddress address = new InetSocketAddress("localhost", 8080);
//		httpServer = HttpServer.create(address, 0);
//
//		final HttpHandler alarmCfgHandler = new HttpHandler() {
//			public void handle(HttpExchange httpExchange) throws IOException {
//				byte[] response;
//				if (httpExchange.getRequestURI().toASCIIString().endsWith(inUrl)) {
//					response = outResponse.getBytes();
//				} else {
//					response = "ok".getBytes();
//				}
//				httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length);
//				httpExchange.getResponseBody().write(response);
//				httpExchange.close();
//			}
//		};
//		httpServer.createContext("/alarmcfg/", alarmCfgHandler);
//		httpServer.start();
//	}
//
//	private String getSampleReport(final String sampleName) throws URISyntaxException, IOException {
//		final URL url = ClassLoader.getSystemClassLoader().getResource("sampleReports");
//		final File f = new File(url.toURI().getRawPath() + "/" + sampleName);
//		final BufferedReader reader = new BufferedReader(new FileReader(f));
//		String line;
//		final StringBuilder report = new StringBuilder();
//		while ((line = reader.readLine()) != null) {
//			report.append(line).append("\n");
//		}
//		return report.toString();
//	}
//
//	private String getTmpDir() {
//		return System.getProperty("java.io.tmpdir");
//	}
//
//	private void setupLockFile(final String _interface) throws IOException, EngineMetaDataException {
//		final String lockedBasetablesFilepath = System.getProperty("CONF_DIR") + File.separator + System.getProperty(".alarm")
//			 + File.separator + _interface + "_lockedbasetables.state";
//		final Properties lockedTables = new Properties();
//		lockedTables.put("TEST_BASE_TABLE", "USED");
//		mkfile(lockedBasetablesFilepath);
//		AlarmMarkupAction.saveLockedBasetablesFile(new File(lockedBasetablesFilepath), lockedTables, Logger.getAnonymousLogger());
//	}
//
//	private SetContext getSetContext() {
//		final SetContext dSet = new SetContext();
//		//noinspection unchecked
//		final String rowStatusToTry = "TBA_blaablaa";
//		//noinspection unchecked
//		dSet.put("rowstatus", rowStatusToTry);
//		return dSet;
//	}
//
//	private Meta_transfer_actions getAlarmReportExecuteTestAction(final String _interface, final String alarmDir,
//																																final String outputFilePrefix) throws Exception {
//		final Properties aprops = new Properties();
//		final File f = new File(alarmDir);
//		if (!f.exists()) {
//			mkdirs(f);
//		} else {
//			//clean out the dir, makes it easier later on whne trying to test the parse...
//			final File[] delete = f.listFiles();
//			for (File toDelete : delete) {
//				deleteFile(toDelete);
//			}
//		}
//		aprops.put("outputPath", alarmDir);
//		aprops.put("protocol", "http");
//		aprops.put("hostname", "localhost:8080");
//		aprops.put("cms", "localhost:8080");
//		aprops.put("username", "u");
//		aprops.put("password", "p");
//		aprops.put("authmethod", "auth");
//		aprops.put("interfaceId", _interface);
//		aprops.put("maxSimulDownloads", "1");
//		aprops.put("outputFilePrefix", outputFilePrefix);
//
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		aprops.store(baos, "");
//		final String getAction_contents = baos.toString();
//		final Meta_transfer_actions action = new Meta_transfer_actions(testDB);
//		action.setAction_contents(getAction_contents);
//		return action;
//	}
//
//	private void fail(final String msg, final Throwable error) {
//		if (error instanceof AssertionFailedError) {
//			throw (AssertionFailedError) error;
//		}
//		final StringBuilder sb = new StringBuilder(msg);
//		sb.append("\n\t");
//		sb.append(error.getMessage());
//		sb.append("\n\t");
//		for (StackTraceElement ste : error.getStackTrace()) {
//			sb.append(ste).append("\n\t");
//		}
//		fail(sb.toString().trim());
//	}
//
//	private void loadUnitDb(final RockFactory unitdb, final String dir) throws Exception {
//		final URL url = ClassLoader.getSystemClassLoader().getResource(dir);
//		if(url == null){
//			throw new Exception("Couldn't load "+dir+", make sure its on the classpath"); 
//		}
//		final File f = new File(url.toURI());
//		loadSetup(unitdb, f.getAbsolutePath());
//	}
//
//	private void loadSetup(final RockFactory testDB, final String baseDir) throws ClassNotFoundException, IOException, SQLException {
//		final File loadFrom = new File(baseDir);
//		final File[] toLoad = loadFrom.listFiles(new FilenameFilter() {
//			public boolean accept(File dir, String name) {
//				return name.endsWith(".sql") && !name.equals(_createStatementMetaFile);
//			}
//		});
//		final File createFile = new File(baseDir + "/" + _createStatementMetaFile);
//		loadSqlFile(createFile, testDB);
//		for (File loadFile : toLoad) {
//			loadSqlFile(loadFile, testDB);
//		}
//	}
//
//	private void loadSqlFile(final File sqlFile, final RockFactory testDB) throws IOException, SQLException, ClassNotFoundException {
//		if (!sqlFile.exists()) {
//			System.out.println(sqlFile + " doesnt exist, skipping..");
//			return;
//		}
//		BufferedReader br = new BufferedReader(new FileReader(sqlFile));
//		String line;
//		int lineCount = 0;
//		try {
//			while ((line = br.readLine()) != null) {
//				lineCount++;
//				line = line.trim();
//				if (line.length() == 0 || line.startsWith("#")) {
//					continue;
//				}
//				while (!line.endsWith(";")) {
//					final String tmp = br.readLine();
//					if (tmp != null) {
//						line += "\r\n";
//						line += tmp;
//					} else {
//						break;
//					}
//				}
//				update(line, testDB);
//			}
//			testDB.commit();
//		} catch (SQLException e) {
//			throw new SQLException("Error executing on line [" + lineCount + "] of " + sqlFile, e);
//		} finally {
//			br.close();
//		}
//	}
//
//	private void update(final String insertSQL, final RockFactory testDB) throws SQLException, ClassNotFoundException, IOException {
//		final Statement s = testDB.getConnection().createStatement();
//		try {
//			s.executeUpdate(insertSQL);
//		} catch (SQLException e) {
//			if (e.getSQLState().equals("S0004")) {
//				System.out.println("Views not supported yet: " + e.getMessage());
//			} else if (e.getSQLState().equals("S0001") || e.getSQLState().equals("42504")) {
//				//ignore, table already exists.......
//			} else {
//				throw e;
//			}
//		}
//	}
//
//	private void shutdown(final RockFactory db) {
//		try {
//			if (db != null && !db.getConnection().isClosed()) {
//				final Statement stmt = db.getConnection().createStatement();
//				stmt.executeUpdate("SHUTDOWN");
//				stmt.close();
//				db.getConnection().close();
//			}
//		} catch (Throwable t) {/*ignored*/}
//	}
}
