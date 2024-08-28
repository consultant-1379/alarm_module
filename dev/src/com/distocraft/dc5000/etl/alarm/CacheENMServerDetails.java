package com.distocraft.dc5000.etl.alarm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.distocraft.dc5000.common.StaticProperties;

import java.util.ArrayList;

/**
 * This class compares two files and creates a cache of the ENM server details
 * in the cache memory.
 * 
 * @author xsarave
 * 
 */
class CacheENMServerDetails {

	static CacheENMServerDetails cacheinstance;
	static HashMap<String, ENMServerDetails> det;

	/**
	 * This function returns the ENM server details in the form of HashMap.
	 * 
	 * @return Returns the HashMap<String,ENMServerDetails> which contains the
	 *         details of the ENM server.
	 */
	static HashMap<String, ENMServerDetails> getInstance(Logger log) throws IOException {
		cacheinstance = new CacheENMServerDetails();
		det = new HashMap<String, ENMServerDetails>();

		List<String> ip = new ArrayList<String>();
		List<String> oss_id = new ArrayList<String>();

		File ref = new File(StaticProperties.getProperty("oss_ref_name_file_path", "/eniq/sw/conf/.oss_ref_name_file"));
		File enm = new File(StaticProperties.getProperty("enm_file_path", "/eniq/sw/conf/enmserverdetail"));
		FileReader fileReader;
		FileReader fileReader1;
		BufferedReader br = null;
		BufferedReader br1 = null;
		try {
			fileReader = new FileReader(ref);
			br = new BufferedReader(fileReader);
			String line;

			while ((line = br.readLine()) != null) {
				String[] odd = line.split("\\s+");
				oss_id.add(odd[0]);
				ip.add(odd[1]);
			}

			fileReader1 = new FileReader(enm);
			br1 = new BufferedReader(fileReader1);

			String line1;
			while ((line1 = br1.readLine()) != null) {
				String[] odd = line1.split("\\s+");

				for (int j = 0; j < ip.size(); j++) {
					if (odd[0].equals(ip.get(j))) {
						ENMServerDetails element = new ENMServerDetails();

						element.setIp(odd[0]);
						element.setHost(odd[1]);
						element.setType(odd[2]);
						element.setUsername(odd[3]);
						element.setPassword(odd[4]);
						element.setHostname(odd[5]);
						det.put(oss_id.get(j), element);

					}
				}

			}
			br.close();
			br1.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			log.finest("Files .oss_ref_name_file or enmserverdetail not found Exception and exception is:"+e);
		}
		return det;
	}

}
