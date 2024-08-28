package com.distocraft.dc5000.etl.alarm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.distocraft.dc5000.common.StaticProperties;
import com.distocraft.dc5000.etl.parser.xmltransformer.ConfigException;
import com.distocraft.dc5000.etl.parser.xmltransformer.Transformation;
import com.ericsson.eniq.common.VelocityPool;

/**
 * This class does the transformation of an alarm.
 * 
 * @author berggren
 * 
 */
public class AlarmTransformation implements Transformation {

	private String filenamePattern;
	private String name;
	private String format;
	

	/**
	 * Does the transformation of this alarm.
	 * 
	 * @param data
	 *            is a Map containing transformed values from the
	 *            Velocity-template.
	 * @param logger
	 *            is the logger to be used by the transformation,
	 */
	public void transform(final Map data, final Logger log) {
		
		if(data.get("OSSTYPE") == "OSSRC")
		{
		try {
			String templateFilename = null;

			if (data.containsKey("AlarmTemplate")) {
				templateFilename = (String) data.get("AlarmTemplate");
			} else {
				log.warning("AlarmTemplate was not received from AlarmParser.");
				return;
			}

			String baseDirectoryPath = null;

			if (data.containsKey("baseDir")) {
				baseDirectoryPath = (String) data.get("baseDir");
				baseDirectoryPath = AlarmHandlerAction.replacePathVariables(baseDirectoryPath, log);
			} else {
				log.warning("baseDir was not received from AlarmParser.");
				return;
			}

			final File baseDirectoryFile = new File(baseDirectoryPath);

			// outputPath error checking.
			if (!baseDirectoryFile.isDirectory()) {
				log.warning("baseDirectory  " + baseDirectoryFile.getAbsolutePath() + " is not a directory.");
				return;
			}

			if (data.containsKey("AlarmCriteria")) {
				if (!data.get("AlarmCriteria").equals("1")) {
					log.finest("Transformation did not proceed because AlarmCriteria is not 1.");
					return;
				}
			}

			String OssName = "";

			// Create the Velocity context and set it's references.
			final VelocityContext context = new VelocityContext();

			final Iterator dataIterator = data.entrySet().iterator();
			while (dataIterator.hasNext()) {
				final Map.Entry pairs = (Map.Entry) dataIterator.next();

				if ((templateFilename.indexOf("ericsson_template") >= 0)
						&& (pairs.getKey().toString().equalsIgnoreCase("EventTime"))) {
					// Create a new velocitycontext variable from eventtime for
					// ericsson_template from format yyyy-MM-dd HH:mm:ss to
					// yyyyMMddHHmmss.
					final String eventTimeString = pairs.getValue().toString();
					log.log(Level.FINEST,
							"this.templateFilename = " + templateFilename + " Key = " + pairs.getKey().toString());

					try {
						final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
						final Date tempDate = simpleDateFormat.parse(eventTimeString);
						final SimpleDateFormat ericssonTemplateDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
						final String parsedDate = ericssonTemplateDateFormat.format(tempDate);
						context.put("ericssonTemplateEventTime", parsedDate);

						log.log(Level.FINEST, "Created ericssonTemplateEventTime with value " + parsedDate);

					} catch (ParseException e) {
						log.log(Level.WARNING, "Error parsing dateString for ericsson_template.vm: " + eventTimeString,
								e);
					}
				}

				context.put(pairs.getKey().toString(), pairs.getValue().toString());

				if (pairs.getKey().toString().equalsIgnoreCase("OssName")) {
					OssName = pairs.getValue().toString();
				}
			}

			log.log(Level.FINEST, "Transforming for OSS with name " + OssName);

			// Create the working directory for this transformation if it
			// doesn't
			// already exist.
			String workingDirectoryPath = baseDirectoryPath;
			if (workingDirectoryPath.charAt(workingDirectoryPath.length() - 1) != '/') {
				workingDirectoryPath = workingDirectoryPath + '/';
			}
			// AlarmTransformation uses "alarm"-folder as working directory.
			workingDirectoryPath = workingDirectoryPath + OssName + '/' + "alarmData";
			workingDirectoryPath = AlarmHandlerAction.replacePathVariables(workingDirectoryPath, log);

			final File workingDirectory = new File(workingDirectoryPath);

			    Files.createDirectories(workingDirectory.toPath());

				final StringWriter writer = new StringWriter();

				VelocityEngine vengine = null;

				final String alarmTemplatesPath = new String(
						System.getProperty("CONF_DIR") + File.separator + "alarm_templates" + File.separator);

				final FileReader fr = new FileReader(alarmTemplatesPath + templateFilename);

				try {
					vengine = VelocityPool.reserveEngine();
					vengine.evaluate(context, writer, "", fr);
				} finally {
					if (vengine != null) {
						VelocityPool.releaseEngine(vengine);
					}
				}

				// Save the file to outputfolder.
				BufferedWriter out = null;
				try {
					final SimpleDateFormat dateFormat = new SimpleDateFormat(this.format);

					String filename = this.filenamePattern;
					// Replace the "$"-character with parsed datestring.
					if (filename.indexOf("$") >= 0) {
						final String dateString = dateFormat.format(new Date());
						filename = filename.replaceFirst("\\$", dateString);
					}

					final String filepath = workingDirectory.getAbsolutePath() + '/' + filename;
					out = new BufferedWriter(new FileWriter(filepath));
					// Write the file to outputpath.
					out.write(writer.toString());
				
					log.fine("AlarmTransfomation.transform: File " + filename + " written successfully.  " + filepath);
					
					out.close();
										
					
				} catch (Exception e) {
					log.log(Level.WARNING, "AlarmTransformation.transform: File write error.", e);
				} finally {
					if (out != null) {
						try {
							out.close();
						} catch (IOException e) {
							log.log(Level.SEVERE, "AlarmTransformation.transform: IOException", e);
						}
					}
				}
			
		} 
		catch (FileAlreadyExistsException e){
			log.log(Level.FINEST, "alarmData directory could not be created as a file with same name exists");
		}
		
		catch (Exception e) {
			log.log(Level.WARNING, "AlarmTransformation.transform failed.", e);
		}
	}
	}

	/**
	 * This method configures this instance of AlarmTransformation.
	 * 
	 * @param src
	 *            The source key to the data map. Not used by
	 *            AlarmTransformation.
	 * @param tgt
	 *            The target key to the data map. Target key may be null if the
	 *            target key(s) is hardcoded in transformation. Not used by
	 *            AlarmTransformation.
	 * @param props
	 *            The configuration of transformation.
	 * @param logger
	 *            is the logger to be used.
	 * @throws ConfigException
	 *             is thrown if transformed fails to initialize.
	 */
	public void configure(final String name, final String src, final String tgt, final Properties props,
			final Logger logger) throws ConfigException {

		this.filenamePattern = props.getProperty("filenamePattern");
		this.format = props.getProperty("format");

		logger.finest("properties = " + props.toString());

		// Set the optional parameters with default values.
		if (this.format == null) {
			this.format = "yyyy-MM-dd_HH:mm:ss:SSS";
			logger.finest("AlarmTransformation format is null. Using default value.");
		}
		logger.finest("AlarmTransformation uses format " + this.format);

		if (this.filenamePattern == null) {
			logger.finest("AlarmTransformation filenamePattern is null. Using default value.");
			this.filenamePattern = "filename_$.xx";
		}
		logger.finest("AlarmTransformation uses filenamePattern " + this.filenamePattern);

	}

	public String getName() throws Exception {
		// TODO Auto-generated method stub
		return name;
	}

	public String getSource() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public String getTarget() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
