package com.distocraft.dc5000.etl.alarm;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;

import com.distocraft.dc5000.common.StaticProperties;

/**
 * AlarmThreadHandling class creates a ThreadPoolExecutor and adding the alarm
 * row information to the LinkedBlockingQueue and creates a Timer for session
 * logout action.
 * 
 * @author xsarave
 * 
 */
public class AlarmThreadHandling {

	private static ThreadPoolExecutor threadpoolExecutor;
	 private static LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
	private static AlarmThreadHandling instance = null;

	private Map<String, String> alarmData=null;
	private static Logger log;

	private AlarmThreadHandling() {

	}

	/**
	 * This function returns the AlarmThreadHandling class instance and creates
	 * ThreadPoolExecutor.
	 * 
	 * @return Returns the instance of AlarmThreadHandling class.
	 */
	public static AlarmThreadHandling getinstance() {
		if (instance == null) {
			instance = new AlarmThreadHandling();

			final int coreSize = Integer.parseInt(StaticProperties.getProperty("jmsConsumerThreadPoolCoreSize", "15"));
			final int maxSize = Integer.parseInt(StaticProperties.getProperty("jmsConsumerThreadPoolMaxSize", "30"));
			threadpoolExecutor = new ThreadPoolExecutor(coreSize, maxSize, 100001, TimeUnit.MILLISECONDS, queue);
			threadpoolExecutor.prestartAllCoreThreads();
			// ShutdownHelper.register(instance);
			
		}

		return instance;
	}

	/**
	 * This function gets the Client instance and adds the alarm information to
	 * the BlockingQueue.
	 * 
	 * @param alarmMessage
	 *            alarmMessage object containing the alarm information.
	 * @param log
	 *            contains log instance
	 * @param cache
	 *            contains the object details of ENMServerDetails.
	 * 
	 */

	public void processMessage(final Map<String, String> alarmMessage, final Logger log, final ENMServerDetails cache) {

		try {
			alarmData=alarmMessage;
			this.log = log;
			log.finest("Adding task to blocking queue");
			queue.offer(new AlarmProcessingTask(alarmData, log, cache));
			log.finest("Currently running threads in threadpoolexecutor ::" + threadpoolExecutor.getActiveCount());
			log.finest("Blocking queue size ::" + queue.size());
			

		} catch (final Exception e) {

			log.info("Exception in alarmThread class::"+e);

		}

	}

	
	

}
