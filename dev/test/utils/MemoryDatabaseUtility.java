package utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import ssc.rockfactory.RockFactory;

/**
 * @author eheijun 
 * @Deprecated replaced by com.ericsson.eniq.common.testutilities.MemoryDatabaseUtility
 * @see com.ericsson.eniq.common.testutilities.MemoryDatabaseUtility
 */
@Deprecated 
public class MemoryDatabaseUtility {

  public static final String TEST_ETLREP_BASIC = "setupSQL/ETLREP/basic";

  public static final String TEST_DWHREP_BASIC = "setupSQL/DWHREP/basic";

  public static final String TEST_DWH_BASIC = "setupSQL/DWH/basic";

  public static final String TESTDB_DRIVER = "org.hsqldb.jdbcDriver";

  public static final String TESTDB_USERNAME = "SA";

  public static final String TESTDB_PASSWORD = "";

  public static final String TEST_ETLREP_URL = "jdbc:hsqldb:mem:etlrep";
  
  public static final String TEST_DWHREP_URL = "jdbc:hsqldb:mem:dwhrep";
  
  public static final String TEST_DWH_URL = "jdbc:hsqldb:mem:dwh";
  
  private final static String _schemaStatementMetaFile = "SchemaCreateStatements.sql";

  private final static String _createStatementMetaFile = "TableCreateStatements.sql";
  
  private MemoryDatabaseUtility() {}  

  /**
   * Loads predefined SQL update files and executes them
   * @param rockFactory Database to be updated
   * @param base SQL file location extension
   * @throws Exception
   */
  public static void loadSetup(final RockFactory rockFactory, final URL url) throws Exception {
    final String baseDir = url.toURI().getRawPath();
    final File loadFrom = new File(baseDir);
    final File[] toLoad = loadFrom.listFiles(new FilenameFilter() {

      public boolean accept(final File dir, final String name) {
        return name.endsWith(".sql") && !name.equals(_createStatementMetaFile) && !name.equals(_schemaStatementMetaFile) ;
      }
    });
    final File schemaFile = new File(baseDir + "/" + _schemaStatementMetaFile);
    loadSqlFile(schemaFile, rockFactory);
    final File createFile = new File(baseDir + "/" + _createStatementMetaFile);
    loadSqlFile(createFile, rockFactory);
    for (File loadFile : toLoad) {
      loadSqlFile(loadFile, rockFactory);
    }
  }

  /**
   * Loads file containing SQL update commands and executes them 
   * @param sqlFile
   * @param rockFactory
   * @throws Exception 
   */
  private static void loadSqlFile(final File sqlFile, final RockFactory rockFactory) throws Exception {
    if (!sqlFile.exists()) {
      return;
    }
    final BufferedReader br = new BufferedReader(new FileReader(sqlFile));
    String line;
    int lineCount = 0;
    try {
      while ((line = br.readLine()) != null) {
        lineCount++;
        line = line.trim();
        if (line.length() == 0 || line.startsWith("#")) {
          continue;
        }
        while (!line.endsWith(";")) {
          final String tmp = br.readLine();
          if (tmp == null) {
            break;
          } else {
            line += "\r\n";
            line += tmp;
          }
        }
        update(line, rockFactory);
      }
      rockFactory.commit();
    } catch (SQLException e) {
      throw new SQLException("Error executing on line [" + lineCount + "] of " + sqlFile, e);
    } catch (Exception e) {
      throw e;
    } finally {
      br.close();
    }
  }

  /**
   * Executes given SQL update command for given RockFactory
   * @param updateSQL
   * @param rockFactory
   * @throws Exception 
   */
  public static int update(final String updateSQL, final RockFactory rockFactory) throws Exception {
    final Statement stmt = rockFactory.getConnection().createStatement();
    try {
      int rows = stmt.executeUpdate(updateSQL);
      return rows;
    } catch (SQLException e) {
      if (e.getSQLState().equals("S0004")) {
        System.out.println("Views not supported yet: " + e.getMessage());
      } else if (e.getSQLState().equals("S0001") || e.getSQLState().equals("42504")) {
        //ignore, table already exists.......
      } else {
        throw e;
      }
      return 0;
    } catch (Exception e) {
      throw e;
    } finally {
      stmt.close();
    }
  }
  
  /**
   * Executes given SQL command for given RockFactory
   * @param executeSQL
   * @param rockFactory
   * @throws Exception 
   */
  public static boolean execute(final String executeSQL, final RockFactory rockFactory) throws Exception {
    final Statement stmt = rockFactory.getConnection().createStatement();
    try {
      boolean result = stmt.execute(executeSQL);
      return result;
    } catch (Exception e) {
      throw e;
    } finally {
      stmt.close();
    }
  }
  
  
  /**
   * Executes given SQL query for given RockFactory
   * @param executeSQL
   * @param rockFactory
   * @throws Exception 
   */
  public static ResultSet executeQuery(final String executeSQL, final RockFactory rockFactory) throws Exception {
    final Statement stmt = rockFactory.getConnection().createStatement();
    try {
      ResultSet rs = stmt.executeQuery(executeSQL);
      return rs;
    } catch (Exception e) {
      throw e;
    } finally {
      stmt.close();
    }
  }
  
  
  /**
   * Shuts down memory database
   * @param rockFactory
   * @throws Exception
   */
  public static void shutdown(final RockFactory rockFactory) throws Exception {
    final Connection memoryConn = rockFactory.getConnection();
    try {
      final Statement stmt = memoryConn.createStatement();
      try {
        stmt.executeUpdate("SHUTDOWN");
      } finally {
        stmt.close();
      }
    } finally {
      memoryConn.close();
    }
  }

  
}
