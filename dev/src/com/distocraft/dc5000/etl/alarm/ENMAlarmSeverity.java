/**
 * 
 */
package com.distocraft.dc5000.etl.alarm;

/**
 * @author xthobob
 * This will be storing the mapping between the Alarm Severity integer to its 
 * Corresponding String.
 *
 */
public enum ENMAlarmSeverity {
	
	CRITICAL(1),
	MAJOR(2), 
	MINOR(3),
	WARNING(4),
	INDETERMINATE(0);

	private int severityValue;
	
	ENMAlarmSeverity(final int newValue) {
        severityValue = newValue;
    }

    public int getValue() { return severityValue; }
    
    public static ENMAlarmSeverity getStatusFor(int desired) {
        for (ENMAlarmSeverity status : values()) {
          if (desired == status.severityValue) {
            return status;
          }
        }
		return null;
    }
	

}
