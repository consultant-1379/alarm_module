package com.distocraft.dc5000.etl.alarm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;

import com.distocraft.dc5000.common.StaticProperties;
import com.distocraft.dc5000.etl.rock.Meta_databases;
import com.ericsson.eniq.common.DatabaseConnections;
import com.ericsson.eniq.common.RemoteExecutor;
import com.ericsson.eniq.repository.DBUsersGet;

import ssc.rockfactory.RockFactory;

/**
 * RestClientInstance creates a Client instance and open the ENM session for
 * sending alarms.
 * 
 * @author xsarave
 * 
 */
public class RestClientInstance {

	private Client client;
	private static RestClientInstance restClientInstance;
	private String HOST = null;
	private String USERNAME = null;
	private String PASSWORD = null;
	private static boolean session_process = false;
	Logger log = null;
	private PoolingHttpClientConnectionManager clientConnectionManager;
	private static int TIMEOUT = 15000; // Value in milliseconds for Timeout
	public HashMap<String, Boolean> sessionMap;
	public HashMap<String, Date> sessionTime;
	private Map<String, NewCookie> sessionCookies;
	private static final String LOGIN = "login";
	RestClientInstance() {
	}

	/**
	 * This function returns the RestClientInstance class instance and starts the
	 * timer thread.
	 * 
	 * @return Returns the instance of AlarmThreadHandling class.
	 */

	synchronized static RestClientInstance getInstance() {
		if (restClientInstance == null) {
			restClientInstance = new RestClientInstance();
			restClientInstance.startTimer();
		}
		return restClientInstance;
	}

	// For EQEV-66104
	public String getSecurityProtocol() { // --- done
		String securityProtocol = null;
		Pattern p = Pattern.compile(".*sslEnabledProtocols=(.*)");
		String path = "/eniq/sw/runtime/tomcat/conf/server.xml";
		File serverFile = new File(path);
		FileReader fileReader = null;
		try {
			fileReader = new FileReader(serverFile);
		} catch (FileNotFoundException e1) {
			e1.getMessage();
		}
		try (BufferedReader bufReader = new BufferedReader(fileReader);) {
			String line = bufReader.readLine();
			while (line != null) {
				if (line.contains("sslEnabledProtocols")) {
					Matcher m = p.matcher(line);
					if (m.find()) {

						securityProtocol = m.group(1).split("\\s")[0];
						securityProtocol = securityProtocol.substring(1, securityProtocol.length() - 1);

						break;
					}
				}
				line = bufReader.readLine();
			}
		}

		catch (IOException e) {
			log.info("Exception occured in getting the security protocol version:" + e);
		}
		return securityProtocol;
	}

	/**
	 * This function creates the client instance and returns the Client instance.
	 * 
	 * @param cache contains the details of ENMServerDetails class object.
	 * @param log   contains the Logger class instance.
	 * @return Returns the instance of Client registered with session cookies.
	 */

	synchronized Client getClient(final ENMServerDetails cache, final Logger log) throws IOException {
 
		log.finest("getClient method called for : " + cache.getHost());
		if (client == null) {
			try {
				sessionChecks();
				this.log = log;
				log.finest("Client object found null so would be created now");

				// To validate server CA certificate
				// In order to import server CA certificate
				// keytool -import -file cacert.pem -alias ENM -keystore
				// truststore.ts -storepass secret
				// And give the location of the keystore

				String keyStorePassValue = getValue("keyStorePassValue").trim();
				String securityProtocol = getSecurityProtocol();

				final SslConfigurator sslConfig = SslConfigurator.newInstance()
						.trustStoreFile(StaticProperties.getProperty("TRUSTSTORE_DIR",
								"/eniq/sw/runtime/jdk/jre/lib/security/truststore.ts"))
						.trustStorePassword(keyStorePassValue).securityProtocol(securityProtocol);

				//log.log(Level.FINE, "we are using {0} securityProtocol.", securityProtocol);
				log.fine("we are using "+ securityProtocol +" securityProtocol.");

				final SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
						sslConfig.createSSLContext());

				final Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
						.register("http", PlainConnectionSocketFactory.getSocketFactory())
						.register("https", sslSocketFactory).build();

				// Pooling HTTP Client Connection manager.
				// connections will be re-used from the pool
				// also can be used to enable
				// concurrent connections to the server and also
				// to keep a check on the number of connections

				clientConnectionManager = new PoolingHttpClientConnectionManager(registry);
				clientConnectionManager.setMaxTotal(50);
				clientConnectionManager.setDefaultMaxPerRoute(20);
				final ClientConfig clientConfig = new ClientConfig();
				clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER, clientConnectionManager);
				clientConfig.connectorProvider(new ApacheConnectorProvider());
				client = ClientBuilder.newBuilder().withConfig(clientConfig).build();
				log.finest("Client object successfully created");
			}

			catch (Exception e) {
				log.warning(" Exception while creating client instance :::   " + e);
			}
		}
		Boolean bool = sessionMap.get(cache.getHost());
		if (Boolean.FALSE.equals(bool)) { // checking whether we have
											// created the session for
											// respective ENM server or not
			session_process = true;
			log.finest("Session will be created for ENM Host : " + cache.getHost());
			try {
				HOST = "https://" + cache.getHost();
				USERNAME = cache.getUsername();
				final String Password_decrypt = cache.getPassword();
				
				PASSWORD = Encryption.decrypt(Password_decrypt);
				log.fine("ENM host and its username: " + HOST +" "+ USERNAME);
				getSession(cache);
				session_process = false;
			} catch (Exception e) {
				log.info("Exception while creating session : " + e);
			}
		}
		return client;
	}

	/**
	 * This function opens the ENM session for sending the REST request.
	 * 
	 * @param cache contains the details of ENMServerDetails class object.
	 * @param log   contains the Logger class instance.
	 * @return Returns the instance of Client registered with session cookies.
	 */
	void getSession(final ENMServerDetails cache) {
		log.finest("Inside getSession method of RestClientInstance");
		try {
			String errorMessage = null;
			final WebTarget target = client.target(HOST).path(LOGIN).queryParam("IDToken1", USERNAME)
					.queryParam("IDToken2", PASSWORD);
			log.finest("login URL :::: "+ client.target(HOST).path(LOGIN));
			final Response response = target.request(MediaType.WILDCARD_TYPE).post(Entity.json(""));

			sessionCookies = new HashMap<>();
			sessionCookies.putAll(response.getCookies());
			List<Cookie> cookieStoreCookies = ApacheConnectorProvider.getCookieStore(client).getCookies();
			String fileLogStr = "Before clearing current cookie information : " + cookieStoreCookies;
			log.fine(fileLogStr);
			ApacheConnectorProvider.getCookieStore(client).clear();
			log.fine("sessioncookies copied from login = "+sessionCookies);

			try {
				subGetSession(cache, errorMessage, response);
			} finally {
				if (Boolean.FALSE.equals(response == null))
					response.close();
			}

		} catch (Exception e) {
			if (e.getCause() instanceof TimeoutException) {
				log.warning("TIMEOUT Exception while logging in ::  " + e);
				String error = "TIMEOUT EXCEPTION:" + e.getMessage();
				errorTableUpdate(HOST, error, "-", "-", "-", "-", "-", "-");
			} else {
				log.warning("Exception while logging in ::  " + e);
				String error = "Exception:" + e.getMessage();
				errorTableUpdate(HOST, error, "-", "-", "-", "-", "-", "-");
			}
		}
	}

	private void subGetSession(final ENMServerDetails cache, String errorMessage, final Response response) {
		sessionTime.put(cache.getHost(), new Date());
		//unencrypted password displaying plain in logs
		String logstr1="Login Response Status :::" + response.getStatus();
		log.finest(logstr1);
		String logstr2=" Login Response Status Information :::"+ response.getStatusInfo();
        log.finest(logstr2);
		Boolean check = false;
		// if the response is client error 401 or any exception that can
		// be
		// successful by retrying,
		// send request for login again
		// and then send the request again for 2 more times
		if (response.getStatus() == 302) {
			sessionMap.put(cache.getHost(), true);
			log.info("Session established...Response code: "+response.getStatus()+" Response Headers : "+response.getHeaders());
		} else {
			log.finest("Response headers : " + response.getHeaders());
			check = true;
			for (int i = 0; i < 2; i++) {
				final WebTarget target1 = client.target(HOST).path(LOGIN).queryParam("IDToken1", USERNAME)
						.queryParam("IDToken2", PASSWORD);
				log.finest("Logging in again  :: URL   :::  " + client.target(HOST).path(LOGIN));
				final Response response1 = target1.request(MediaType.WILDCARD_TYPE).post(Entity.json(""));
				sessionTime.put(cache.getHost(), new Date());
				try {
					log.finest("Response received ::: Status = "+response1.getStatus());
					log.finest("Response received ::: Status Information = "+response1.getStatusInfo());
					if (response1.getStatus() == 302) {
						sessionMap.put(cache.getHost(), true);
						log.info("Session established while re-trying : " + response1.readEntity(String.class));
						check = false;
						break;
					} else {
						errorMessage = "Error Status:" + response1.getStatus() + " ,Response Headers:"
								+ response1.getHeaders();
					}
				} finally { // closing the response to release the
							// resources consumed
					if (Boolean.TRUE.equals(response1 != null))
						response1.close();
				}
			}
		}
		if (Boolean.TRUE.equals(check)) {
			log.severe(" Session creation request sent to server three times but failed to create session for : " + cache.getHost());
			String error = "SESSION CREATION FAILED:" + errorMessage;
			errorTableUpdate(HOST, error, "-", "-", "-", "-", "-", "-");
		}
	}

	/**
	 * This function checks the request is not send for more than 1 minute it will
	 * close the session.
	 * 
	 * @param client instance of the Client object
	 * @param client instance of Logger object
	 */
	void sessionCloseCheck() {
		try {
			Date date = new Date();
			boolean sessionCheckAll = false;
			for (Map.Entry<String, ENMServerDetails> entry : CacheENMServerDetails.det.entrySet()) {
				if ((date.getTime() - sessionTime.get(entry.getValue().getHost()).getTime()) >= 60000) {
					log.finest(
							"Found that session has now been opened for more than 1 minute, hence  will close session now for : "
									+ entry.getValue().getHost() + " if it is open");
					closeSession(entry.getValue());
				}
				if (Boolean.TRUE.equals(sessionMap.get(entry.getValue().getHost()))) {
					sessionCheckAll = true;
				}
			}
			if (!sessionCheckAll) {
				sessionShutDown();
			}
		}

		catch (Exception e) {
			log.warning("Exception while checking whether session has to be closed for a host or not : " + e);
		}
	}

	private void sessionShutDown() {
		if (client != null && !session_process) {
			if (checkIfsessionMapHasHost()) {
				log.finest("1--->Atleast one ACTIVE entry found in sessionMap. sessionMap : " + sessionMap);
			} else {
				log.finest("1--->No ACTIVE entry found in sessionMap. sessionMap : " + sessionMap);
				log.info("Since all sessions were closed shutting down the connection pool");
				clientConnectionManager.shutdown();
				client = null;
			}
		}
	}

	public void closeSession(ENMServerDetails cache) {
		try {
			log.finest("Will close session for : "+cache.getHost()+" if it is open.");			
			if (Boolean.TRUE.equals(sessionMap.get(cache.getHost()))) {
				try {
					closeSessionProcess(cache);
				} catch (Exception e) {
					log.warning("Exception while closing session for host : " + cache.getHost() + e);
				} finally {
					if (checkIfsessionMapHasHost()) {
						log.finest("2--->Atleast one ACTIVE entry found in sessionMap. sessionMap : " + sessionMap);
					} else {
						log.finest("2--->No ACTIVE entry found in sessionMap. sessionMap : " + sessionMap);
						log.finest("Clearing sessionCookies-1. sessionCookies = "+sessionCookies);
						sessionCookies.clear();
					}
				}
			} else {
				if (client != null) {
					if (checkIfsessionMapHasHost()) {
						log.finest("3--->Atleast one ACTIVE entry found in sessionMap. sessionMap : " + sessionMap);
					} else {
						log.finest("3--->No ACTIVE entry found in sessionMap. sessionMap : " + sessionMap);
						clientConnectionManager.shutdown();
						log.finest("2#Connection manager shut down");
						client = null;
					}
				}
			}
		} finally {
			if (checkIfsessionMapHasHost()) {
				log.finest("4--->Atleast one ACTIVE entry found in sessionMap. sessionMap : " + sessionMap);
			} else {
				log.finest("4--->No ACTIVE entry found in sessionMap. sessionMap : " + sessionMap);
				log.finest("Clearing sessionCookies-2. sessionCookies = "+sessionCookies);
				sessionCookies.clear();
			}
		}

	}

	private void closeSessionProcess(ENMServerDetails cache) {
		int status_code;
		log.info("Idle session found hence closing session for : " + cache.getHost());
		Builder builder;
		builder = client.target(HOST).path("logout").request("application/json");
		Response response3 = setCookies(builder, log).get();
		sessionTime.put(cache.getHost(), new Date());
		try {
			status_code = response3.getStatus();
			log.finest("Logging out. Response receieved : Status = "+ response3.getStatus());
			log.finest("Logging out. Response receieved : Status Information = "+ response3.getStatusInfo());
			if (response3.getStatus() == 200 || response3.getStatus() == 422) { //response status 422 check included to resolve Alarm sending 302 response issue
				sessionMap.put(cache.getHost(), false);
				log.info("Successfully logged out for :" + cache.getHost());
			}
		} finally {
			response3.close();
		}
		if (status_code == 200 || status_code == 422) {
			if (checkIfsessionMapHasHost()) {
				log.finest("5--->Atleast one ACTIVE entry found in sessionMap. sessionMap : " + sessionMap);
			} else {
				log.finest("5--->No ACTIVE entry found in sessionMap. sessionMap : " + sessionMap);
				clientConnectionManager.shutdown();
				log.finest("1#Connection manager shut down");
				client.close();
				if (Boolean.TRUE.equals(client != null))
					client = null;
				log.finest("Successfully logged out");
			}
		} else {
			log.warning("Error in closing the session..session exists and logout response from enm server: "
					+ response3.getStatus() + " " + response3.getStatusInfo());
		}
	}

	/**
	 * This function insert a row in ALARM_ERROR table if any errors occur during
	 * sending of alarms.
	 * 
	 * @param hostName       Hostname of the ENM server
	 * @param detailsOfError Details of the error occured during sending of alarms.
	 * @param nodeName       name of the node
	 * @param ossName        alias of the ossname
	 */

	synchronized void errorTableUpdate(String ENMHostname, String errorDetail, String alarmName,
			String managedObjectInstance, String objectOfReference, String ossName, String reportTitle,
			String eventTime) {
		RockFactory dwhdb = null;
		String alarmError = "Insert into DC_Z_ALARM_ERROR (ENMHostname, ErrorDetail, AlarmName ,ManagedObjectInstance ,ObjectOfReference ,OssName ,ReportTitle ,EventTime) Values ('"
				+ ENMHostname + "','" + errorDetail.replaceAll("'", "''") + "','" + alarmName + "','"
				+ managedObjectInstance + "','" + objectOfReference + "','" + ossName + "','" + reportTitle + "','"
				+ eventTime + "')";

		try {
			dwhdb = DatabaseConnections.getDwhDBConnection();
			dwhdb.getConnection().createStatement().executeUpdate(alarmError);
			log.finest("Errors occured during REST connection is stored successfully in DC_Z_ALARM_ERROR table");
		} catch (Exception e) {
			log.severe("Exception while updating the details for the failed data " + e.toString());

		} finally {
			try {
				if (dwhdb.getConnection() != null)
					dwhdb.getConnection().close();
			} catch (NullPointerException|SQLException e) {
				log.severe("Error in closing the DWH database connection: " + e.toString());
			}
		}
	}

	/**
	 * This function creates A Timer object and schedule a TimerTask to check the
	 * condition for session closing action.
	 * 
	 */
	public void startTimer() {

		Timer timer = new Timer();

		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				log.finest("Timer started as scheduled");
				if (client != null) {
					log.finest("Timer started and found a non null client will check sessions now");
					sessionCloseCheck();
				}
			}
		}, 15000, 15000);
	}

	/**
	 * This function read the value from niq.ini file and return the value
	 * 
	 * @param command
	 * @return String
	 */
	public String getValue(String command) {
		String output = "";
		try {

			String systemCommandString = "";
			final String user = "dcuser";
			final String service_name = "engine";
			List<Meta_databases> mdList = DBUsersGet.getMetaDatabases(user, service_name);
			if (mdList.isEmpty()) {
				mdList = DBUsersGet.getMetaDatabases(user, service_name);
				if (mdList.isEmpty()) {
					throw new Exception(
							"Could not find an entry for " + user + ":" + service_name + " in engine! (was is added?)");
				}
			}
			final String password = mdList.get(0).getPassword();
			systemCommandString = ". /eniq/home/dcuser; . ~/.profile; " + "cat /eniq/sw/conf/niq.ini |grep -i "
					+ command;
			output = RemoteExecutor.executeComand(user, password, service_name, systemCommandString);
			if (!output.contains("\n")) {
				output = output.substring(output.indexOf("=") + 1);
			} else {
				String[] outputArray = output.split("\n");
				boolean encryptionflag = false;
				for (String str : outputArray) {
					String key = str.substring(0, str.indexOf("=")).trim();
					String value = str.substring(str.indexOf("=") + 1).trim();
					if (key.contains("_Encrypted") && (value.equalsIgnoreCase("Y") ||value.equalsIgnoreCase("YY"))) {
						encryptionflag = true;
					} else if (key.equalsIgnoreCase(command)) {
						output = value;
					}
				}

				if (encryptionflag) {
					
					output = Encryption.decrypt(output.trim());
				}
			}
			return output.trim();
		} catch (final Exception e) {
			e.getMessage();
			log.finest("Exception:" + e);
		}
		return output;
	}

	void sessionChecks() {

		sessionMap = new HashMap<String, Boolean>();
		sessionTime = new HashMap<String, Date>();
		for (Map.Entry<String, ENMServerDetails> entry : CacheENMServerDetails.det.entrySet()) {
			sessionMap.put(entry.getValue().getHost(), false);
			sessionTime.put(entry.getValue().getHost(), new Date());
		}

	}

	public Builder setCookies(Builder builder, Logger log) {

		sessionCookies.forEach((name, newCookie) -> {
			Date currentDate = new Date();
			if (newCookie.getMaxAge() != 0
					&& (newCookie.getExpiry() == null || !newCookie.getExpiry().before(currentDate))) {
				builder.cookie(newCookie.getName(), newCookie.getValue());
			}
		});
		if (log != null) {
			log.finest("sesioncookies set = " + sessionCookies);
		}

		return builder;
	}

	void clearCookieStore() {
		ApacheConnectorProvider.getCookieStore(client).clear();
	}
	
	Boolean checkIfsessionMapHasHost() {
		
		Boolean sessionMapHasHost = false;
		
		for(HashMap.Entry<String, Boolean> entry : sessionMap.entrySet()) {
		    
		    if(entry.getValue()) {
				sessionMapHasHost = true;
				log.finest("ACTIVE entry found in sessionMap");
				break;
			}
		}
		return sessionMapHasHost;
	}

}
