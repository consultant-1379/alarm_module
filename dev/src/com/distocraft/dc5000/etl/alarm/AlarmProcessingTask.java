package com.distocraft.dc5000.etl.alarm;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

import java.util.Map;

import java.util.TimeZone;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.apache.http.cookie.Cookie;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;

import javax.ws.rs.client.Invocation.Builder;

/**
 * This class is a thread class that contains functionality to handle alarm
 * request send from ENIQ to ENM in the form of JSON Object in REST request.
 * 
 * @author xsarave
 * 
 */
public class AlarmProcessingTask implements Runnable {

	Logger log;
	Client client;
	Boolean session = false;
	String eniqHostName;
	ENMServerDetails cache;
	String HOST;
	RestClientInstance restClientInstance;
	String objectOfReference;
	String SpecificProblem;
	String probableCause;
	String perceivedSeverity;
	String thresholdInformation;
	String monitoredAttributes;
	String monitoredAttributeValues;
	String additionalText;
	String timeZone;
	String eventTime;
	String ossname;
	String eventType;
	String alarmName;
	String managedObjectInstance;
	String reportTitle;
	String perceivedSeverityText;
	String additionalInformation;
	String managedObjectClass;
	public static final String SECONDS = "00";
	public static final String COMMA = ",";
	public static final String DEFAULT_URL_ENC = "UTF-8";
	public static final String ENM_FM_URL_PATH = "errevents-service/v1/errorevent/";

	/**
	 * This function is the constructor of this AlarmMarkupAction class.
	 * 
	 * @param message
	 * @param log
	 * @param client
	 * @param cache
	 * 
	 */
	public AlarmProcessingTask(final Map<String, String> message, final Logger log, ENMServerDetails cache) {

		this.objectOfReference = message.get("ObjectOfReference");
		this.SpecificProblem = message.get("SPText");
		this.probableCause = message.get("PCText");
		this.perceivedSeverity = message.get("PerceivedSeverity");
		this.thresholdInformation = message.get("ThresholdInformation");
		this.monitoredAttributes = message.get("MonitoredAttributes");
		this.monitoredAttributeValues = message.get("MonitoredAttributeValues");
		this.additionalText = message.get("AdditionalText");
		this.timeZone = message.get("TimeZone");
		this.eventTime = message.get("EventTime");
		this.ossname = message.get("OssName");
		this.eventType = message.get("ETText");
		this.alarmName = message.get("AlarmName");
		this.managedObjectInstance = message.get("ManagedObjectInstance");
		this.reportTitle = message.get("ReportTitle");
		this.additionalInformation = message.get("AdditionalInformation");
		this.perceivedSeverityText = message.get("PerceivedSeverityText");
		this.managedObjectClass = message.get("ManagedObjectClass");

		this.log = log;
		//log.log(Level.FINEST, "oor in message {0}", objectOfReference);
		log.finest("oor in message " + objectOfReference);
		this.cache = cache;
		this.eniqHostName = cache.getHostname();

	}

	AlarmProcessingTask() {
		// constructor
	}

	/**
	 * Send alarm information to ENM in REST PUT request in the form of JSON object.
	 */
	@Override
	public void run() {
		try {
			log.finest("Thread : "+Thread.currentThread().getName()+" starts executing for ENM Host : "+cache.getHost());
			HOST = "https://" + cache.getHost();
			restClientInstance = RestClientInstance.getInstance();
			client = restClientInstance.getClient(cache, log);
			Boolean bool = restClientInstance.sessionMap.get(cache.getHost());
			if (Boolean.TRUE.equals(bool)) { // proceed further only if session got created
				Builder builder;
				List<Cookie> cookieStoreCookies = ApacheConnectorProvider.getCookieStore(client).getCookies();
				log.fine("Alarm. Cookiestore after clearing  : " + cookieStoreCookies);

				manipulateDataForENM();
				String fullpath = ENM_FM_URL_PATH + urlDataEncoder(
						objectOfReference + "," + SpecificProblem + "," + probableCause + "," + eventType);
				log.finest("Data being sent to ENM: eventTime="+eventTime+" ,PerceivedSeverity="+perceivedSeverity+" ,ThresholdInformation="+thresholdInformation+" ,MonitoredAttributes="+monitoredAttributes+" ,monitoredAttributeValues="+monitoredAttributeValues+" ,AdditionalText="+additionalText+" ,TimeZoneToENM="+timeZone+" ,manageObjectInstance="+managedObjectInstance+" ,perceivedSeverityText="+perceivedSeverityText+" ,managedObjectClass="+managedObjectClass+" ,additionalinformation="+additionalInformation+" ,notificationSource="+eniqHostName);
				AlarmRequestDetails event = new AlarmRequestDetails();
				event.setEventTime(eventTime);
				event.setPerceivedSeverity(perceivedSeverity);
				event.setThresholdInformation(thresholdInformation);
				event.setMonitoredAttributes(monitoredAttributes);
				event.setMonitoredAttributeValues(monitoredAttributeValues);
				event.setAdditionalText(additionalText);
				event.setTimeZone(timeZone);
				event.setManagedObjectInstance(managedObjectInstance);
				event.setPerceivedSeverityText(perceivedSeverityText);
				event.setManagedObjectClass(managedObjectClass);
				event.setAdditionalInformation(additionalInformation);
				event.setNotificationSource(eniqHostName);
				restClientInstance.sessionTime.put(cache.getHost(), new Date());

				builder = client.target(HOST).path(fullpath).request("application/json");
				
			
				restClientInstance.clearCookieStore();
				
				final Response response1 = restClientInstance.setCookies(builder, log).put(Entity.json(event));

				log.finest("Url for sending alarm data  ::::  "+client.target(HOST).path(fullpath)+" for : "+Thread.currentThread().getName());
				restClientInstance.sessionTime.put(cache.getHost(), new Date());
				final StringBuilder buffer = new StringBuilder();
				buffer.append("\nRequest: " + fullpath);

				String errorMessage = null;
				log.finest("Response after sending alarm data : response status : " + response1.getStatus());
				log.finest("Response after sending alarm data : response status information : "
						+ response1.getStatusInfo());
				if (response1.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
					log.info("Alarm data sent successfully for : " + cache.getHost()
							+ response1.readEntity(String.class));

					response1.close();
				} else {
					alarmData(builder, fullpath, event, errorMessage);

				}

			} else {
				restClientInstance.sessionTime.put(cache.getHost(), new Date());
				log.info("Will not send data as Client failed to create session with the server :" + cache.getHost());
			}
		} catch (Exception e) {
			if (e.getCause() instanceof TimeoutException) {
				log.info("TIMEOUT Exception while sending alarm ::  " + e);
				restClientInstance.errorTableUpdate(HOST, "TIMEOUT EXCEPTION IN ALARM REQUEST:" + e.getMessage(),
						alarmName, managedObjectInstance, objectOfReference, ossname, reportTitle, eventTime);
			} else {
				log.info("Exception while sending alarm ::  " + e);
				restClientInstance.errorTableUpdate(HOST, "Exception in Alarm data sending:" + e.getMessage(),
						alarmName, managedObjectInstance, objectOfReference, ossname, reportTitle, eventTime);
			}
		}
	}

	private void alarmData(Builder builder, String fullpath, AlarmRequestDetails event, String errorMessage) {
		Boolean check = false;
		try {
			client = RestClientInstance.getInstance().getClient(cache, log);
			check = true;
			for (int i = 0; i < 2; i++) {
				
				restClientInstance.clearCookieStore();
				
				log.finest("Trying again to send data to the ENM server");
				final Response response2 = restClientInstance.setCookies(builder, log).put(Entity.json(event));

				restClientInstance.sessionTime.put(cache.getHost(), new Date());

				log.finest("URL while re-trying :" + client.target(HOST).path(fullpath));
				if (response2.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
					log.info("Data sent successfully while re-trying." + response2.readEntity(String.class));
					check = false;
					break;
				}
				errorMessage = "Error Status:" + response2.getStatus() + " ,Response Headers:" + response2.getHeaders();

				response2.close();
			}
			if (Boolean.TRUE.equals(check)) {
				log.warning("Tried sending data to ENM Server 3 times but failed for : " + cache.getHost());
				String error = "ALARM DATA SENDING FAILED:" + errorMessage;
				log.severe(error);
				restClientInstance.errorTableUpdate(HOST, error, alarmName, managedObjectInstance, objectOfReference,
						ossname, reportTitle, eventTime);
			}
		} catch (Exception e ) {
			log.warning("Exception while re-trying to send Alarms" + e.getMessage());
		}

		log.finest("Thread " + Thread.currentThread().getName() + " finished its execution");
	}

	/**
	 * This is to manipulate the existing Alarm data compatible with ENM FM Alarm.
	 */
	private void manipulateDataForENM() throws NullPointerException{

		// If objectOfReference has Subnetwork, then removing the same from it.
		if (objectOfReference.contains("SubNetwork=SubNetwork,")) {
			objectOfReference = objectOfReference.replace("SubNetwork=SubNetwork,", "");
		}

		// If objectOfReference doesn't have MOID data, then append MOID with
		// objectOfReference.
		if (!objectOfReference.contains(managedObjectInstance)) {
			objectOfReference += COMMA + managedObjectInstance;
		}

		// Restructuring the Event time according to ENM specified. eg. 20170828094500
		StringBuilder remodeledEventTimeForENM = new StringBuilder() ;
		for (int i = 0; i < eventTime.length(); i++) {
			if ((eventTime.charAt(i) != '-') && (eventTime.charAt(i) != ' ') && (eventTime.charAt(i) != ':')) {
				remodeledEventTimeForENM = remodeledEventTimeForENM.append(eventTime.charAt(i)) ;
			}
		}
		eventTime = remodeledEventTimeForENM + SECONDS;

		// perceivedSeverity should be transformed to below list for the ENM by using
		// ENUM.
		// CRITICAL, MAJOR, MINOR, WARNING, INDETERMINATE
		try {
			perceivedSeverity = perceivedSeverity == null ? "-1" : perceivedSeverity;
			ENMAlarmSeverity severity = ENMAlarmSeverity.getStatusFor(Integer.parseInt(perceivedSeverity));
			perceivedSeverity = severity == null ? "INDETERMINATE" : severity.toString();
		} catch (Exception e) {
			log.severe("Error while parsing the perceivedSeverity data =" + perceivedSeverity + " :" + e);
		}

		// Appending GMT with the timezone offset.
		timeZone = "GMT" + timeZone;

	}

	public String urlDataEncoder(String urlData) throws UnsupportedEncodingException {
		return URLEncoder.encode(urlData, DEFAULT_URL_ENC).replaceAll("\\+", "%20");
	}

}
