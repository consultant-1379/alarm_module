package com.distocraft.dc5000.etl.alarm;

/**
 * @author xsarave
 * 
 */
public class AlarmRequestDetails {
	private String eventTime;
	private String perceivedSeverity;
	private String thresholdInformation;
	private String monitoredAttributes;
	private String monitoredAttributeValues;
	private String additionalText;
	private String timeZone;
	private String notificationSource;
	private String managedObjectClass;
	private String perceivedSeverityText;
	private String additionalInformation;
	private String managedObjectInstance;
	

	public String getManagedObjectClass() {
		return managedObjectClass;
	}

	public void setManagedObjectClass(String managedObjectClass) {
		this.managedObjectClass = managedObjectClass;
	}

	public String getPerceivedSeverityText() {
		return perceivedSeverityText;
	}

	public void setPerceivedSeverityText(String perceivedSeverityText) {
		this.perceivedSeverityText = perceivedSeverityText;
	}

	public String getAdditionalInformation() {
		return additionalInformation;
	}

	public void setAdditionalInformation(String additionalInformation) {
		this.additionalInformation = additionalInformation;
	}

	public String getManagedObjectInstance() {
		return managedObjectInstance;
	}

	public void setManagedObjectInstance(String managedObjectInstance) {
		this.managedObjectInstance = managedObjectInstance;
	}

	

	public String getNotificationSource() {
		return notificationSource;
	}

	public void setNotificationSource(String notificationSource) {
		this.notificationSource = notificationSource;
	}

	public String getEventTime() {
		return eventTime;
	}

	public void setEventTime(String eventTime) {
		this.eventTime = eventTime;
	}

	public String getPerceivedSeverity() {
		return perceivedSeverity;
	}

	public void setPerceivedSeverity(String perceivedSeverity) {
		this.perceivedSeverity = perceivedSeverity;
	}

	public String getThresholdInformation() {
		return thresholdInformation;
	}

	public void setThresholdInformation(String thresholdInformation) {
		this.thresholdInformation = thresholdInformation;
	}

	public String getMonitoredAttributes() {
		return monitoredAttributes;
	}

	public void setMonitoredAttributes(String monitoredAttributes) {
		this.monitoredAttributes = monitoredAttributes;
	}

	public String getMonitoredAttributeValues() {
		return monitoredAttributeValues;
	}

	public void setMonitoredAttributeValues(String monitoredAttributeValues) {
		this.monitoredAttributeValues = monitoredAttributeValues;
	}

	public String getAdditionalText() {
		return additionalText;
	}

	public void setAdditionalText(String additionalText) {
		this.additionalText = additionalText;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

}
