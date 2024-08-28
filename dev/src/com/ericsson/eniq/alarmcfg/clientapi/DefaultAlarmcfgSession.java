package com.ericsson.eniq.alarmcfg.clientapi;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.distocraft.dc5000.common.StaticProperties;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACAuthenticateException;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACReportNotFoundException;
import com.ericsson.eniq.alarmcfg.clientapi.exceptions.ACSessionException;



/**
 * This class defines the session to the ENIQ Alarmcfg. Use it to authenticate to the server and retrieve alarm reports
 * from BO.
 * 
 * @author eheijun
 * @copyright Ericsson (c) 2009
 */
public class DefaultAlarmcfgSession implements IAlarmcfgSession {

	/**
	 * Logger
	 */
	// private static Logger logger = Logger.getLogger(DefaultAlarmcfgSession.class.getName());
	private static Logger logger = Logger.getLogger("etlengine.engine.EngineAdmin");
	private final static String DEFAULTPROTOCOL = "HTTP";

	/**
	 * Protocol HTTP/HTTPS
	 */
	private String protocol;

	/**
	 * Alarmcfg host name or IP, includes port number
	 */
	private String host;

	/**
	 * BO CMS name or IP, includes port number
	 */
	private String cms;

	/**
	 * BO username
	 */
	private String username;

	/**
	 * BO password
	 */
	private String password;

	/**
	 * BO login parameter
	 */
	private String authtype;

	/**
	 * Status of the session
	 */
	private boolean active = false;

	/**
	 * Our "browser"
	 */
	protected HttpClient httpclient;

	/**
	 * 
	 */
	boolean foundMatch = false;
	HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

	/**
	 * This connection is done via Factory class, located in this package.
	 * 
	 * @param nProtocal
	 *            HTTP or HTTPS
	 * @param nHost
	 * @param nUsername
	 * @param nPassword
	 * @throws ACAuthenticateException
	 * @throws ACSessionException
	 */
	protected DefaultAlarmcfgSession(final String nProtocol, final String nHost, final String nCms,
			final String nUsername, final String nPassword, final String nAuthMethod) throws ACAuthenticateException,
			ACSessionException {

		protocol = nProtocol;
		host = nHost;
		cms = nCms;
		username = nUsername;
		password = nPassword;
		authtype = nAuthMethod;

		// CR-118 -- Starts--
		final File inputFile = new File("/eniq/sw/runtime/tomcat/webapps/alarmcfg/WEB-INF/web.xml");
		try {
			final FileInputStream fstream = new FileInputStream(inputFile);
			final DataInputStream in = new DataInputStream(fstream);
			final BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while (((strLine = br.readLine()) != null) && !foundMatch) {
				if (strLine.contains("CONFIDENTIAL")) {
					foundMatch = true;
					break;
				}
			}
			in.close();
			fstream.close();

		} catch (final Exception e) {
			logger.info("Exception Occured while checking web.xml file : " + e.getMessage());
		}

		if (foundMatch) {
			protocol = "https";
			host = "webserver:8443";
		}

		logger.finest(" DefaultAlarmcfgSession - Protocol :" + protocol + " and Hostname :" + host);
		// CR-118 --End--

		// new HTTP client
		httpclient = new DefaultHttpClient();

		try {

			// default retry ...
			// httpclient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new
			// DefaultHttpMethodRetryHandler());
			// 3 secs
			// httpclient.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			// RFC 2109
			// httpclient.getParams().setCookiePolicy(CookiePolicy.RFC_2109); // see
			// latest HTTP Protocol
			httpclient.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_1);
			// UTF-8 charset
			httpclient.getParams().setParameter("http.protocol.content-charset", "UTF-8");

			// test connection
			getFirstPage();

			active = true;
		} catch (final ACSessionException e) {
			throw e;
		} catch (final ACAuthenticateException e) {
			throw e;
		} catch (final Exception e) {
			throw new ACSessionException("Connection failed: " + e.getMessage());
		}
	}

	/**
	 * This connection is done via Factory class, located in this package.
	 * 
	 * @param nHost
	 * @param nUsername
	 * @param nPassword
	 * @throws ACAuthenticateException
	 * @throws ACSessionException
	 */
	protected DefaultAlarmcfgSession(final String nHost, final String nCms, final String nUsername,
			final String nPassword, final String nAuthMethod) throws ACAuthenticateException, ACSessionException {
		this(DEFAULTPROTOCOL, nHost, nCms, nUsername, nPassword, nAuthMethod);
	}

	protected DefaultAlarmcfgSession() {
	}

	/**
	 * To test Alarmcfg connection. Called when initialization is done.
	 * 
	 * @throws ACSessionException
	 * 
	 * @see com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgSession#connect(java.lang.String, java.lang.String)
	 */
	private void getFirstPage() throws ACAuthenticateException, ACSessionException {

		try {
			final String firstPageURL = protocol + "://" + host + "/alarmcfg/";
			final String HTML = getRealURL(firstPageURL);
			logger.finest("URL is : " + HTML);
		} catch (final IOException e) {
			throw new ACAuthenticateException("Authentication failed: " + e.getMessage());
		} catch (final ACSessionException e) {
			throw e;
		} catch (final Exception e) {
			throw new ACAuthenticateException("Authentication failed: " + e.getMessage());
		}
	}

	/**
	 * To retrieve a report. Pass report request to retrieve some specific report. This method returns plain HTML (or
	 * PDF) report.
	 * 
	 * @return Report as plain HTML
	 * @throws ACAuthenticateException
	 * @throws ACSessionException
	 * @throws ACReportNotFoundException
	 * @see com.ericsson.eniq.alarmcfg.clientapi.IAlarmcfgSession#getReport(IAlarmcfgReportRequest)
	 */
	@Override
	public IAlarmcfgReport getReport(final IAlarmcfgReportRequest request) throws ACSessionException,
			ACReportNotFoundException, ACAuthenticateException {

		if (!active) {
			throw new ACSessionException("Alarmcfg session is not active.");
		}

		String reportURL = null;

		final IAlarmcfgReport report = new DefaultAlarmcfgReport();

		final String reportNameWithParameters = request.getReportName();
		logger.finest("ReportNameWithParameters: '" + reportNameWithParameters + "'");

		int endIndex = reportNameWithParameters.indexOf("&");
		if (endIndex < 0) {
			endIndex = reportNameWithParameters.length();
		}

		final String reportNameWithoutParameters = reportNameWithParameters.substring(0, endIndex);

		String loginInfo = "cms=" + cms;
		loginInfo += "&" + "username=" + username;
		loginInfo += "&" + "password=" + password;
		loginInfo += "&" + "authtype=" + authtype;

		try {
			reportURL = protocol + "://" + host + "/alarmcfg/AlarmReportRunner?" + loginInfo + "&"
					+ reportNameWithParameters;
			final String response = getRealURL(reportURL);

			logger.finest("Report URL is : " + response);
			if (response.equals("")) {
				throw new ACReportNotFoundException(reportURL);
			} else {
				report.setName(reportNameWithoutParameters);
				report.setXML(response);
			}

		} catch (final IOException e) {
			// This happens when we are trying to fetch some URL
			// we categories this as connection problem
			
			throw new ACSessionException("Couldn't get URL: '" + reportURL.replaceAll("password=[^&]+", "password=***") + " (" + e.getMessage().replaceAll("password=[^&]+", "password=***") + ")");
		} catch (final ACReportNotFoundException e) {
			
			throw new ACReportNotFoundException("Report not found: " + reportURL.replaceAll("password=[^&]+", "password=***") + " (" + e.getMessage().replaceAll("password=[^&]+", "password=***") + ")");
		} catch (final Exception e) {
		
			throw new ACSessionException("Fatal error when getting URL: '" + reportURL.replaceAll("password=[^&]+", "password=***") + " (" + e.getMessage().replaceAll("password=[^&]+", "password=***") + ")");
		}

		return report;

	}

	/**
	 * This method fetches certain page. It's by no means universal, but tackles some redirecting. For page fetch we
	 * mimic IE browser and do some MANUAL encoding. Horrible. TR: HK66224: start Put timeout value which will be 180000
	 * by default. timeout value is used here by alarm
	 * 
	 * @param path
	 *            the page we want to fetch
	 * @return HTML of that page
	 * @throws HttpException
	 *             thrown if page fetch fails
	 * @throws IOException
	 *             thrown if no connection (I assume)
	 * @throws ACSessionException .
	 */
	protected String getRealURL(String path) throws HttpException, IOException, ACSessionException {

		String URL= path; 
		logger.fine("URL: '" + URL.replaceAll("password=[^&]+", "password=***") + "'");

		// replace all spaces with plus sign...
		path = path.replaceAll(" ", "\\+");

		// final StringBuilder streamBuffer = new StringBuilder();

		// CR118 --Start--
		String strBuf = null;
		ClientConnectionManager ccm;
		final HttpResponse httpResponse;
		HttpGet method = null;

		if (foundMatch) {

			logger.fine("HTTPs is Enabled");
			try {
				final SSLContext sslcontext = SSLContext.getInstance("TLS");

				// sslcontext.init(null, null, null);
				sslcontext.init(null, new TrustManager[] { new X509TrustManager() {
					@Override
					public X509Certificate[] getAcceptedIssuers() {
						logger.fine("============= getAcceptedIssuers =============");
						return null;
					}

					@Override
					public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
						logger.fine("============= checkClientTrusted =============");

					}

					@Override
					public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
						logger.fine("============= checkServerTrusted =============");
					}
				} }, new SecureRandom());

				final SSLSocketFactory sf = new SSLSocketFactory(sslcontext,
						SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
				final Scheme https = new Scheme("https", 8443, sf);

				ccm = httpclient.getConnectionManager();
				final SchemeRegistry sr = ccm.getSchemeRegistry();
				sr.register(https);
				method = new HttpGet(path);

				// we try to look like IE
				method.addHeader("Accept-Language", "en-us");
				method.addHeader("Accept-Encoding", "gzip, deflate");
				method.addHeader("content-length", "0");
				method.addHeader("accept", "image/gif, " + "image/x-xbitmap, " + "image/jpeg, " + "image/pjpeg, "
						+ "application/x-shockwave-flash, " + "application/vnd.ms-excel, "
						+ "application/vnd.ms-powerpoint, " + "application/msword, */*");
				method.addHeader("User-Agent",
						"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)");
				method.getParams().setParameter("http.socket.timeout", 180 * 1000);

				// Fetch page with HTTPs
				httpResponse = httpclient.execute(method);

				final HttpEntity entity = httpResponse.getEntity();
				if (entity != null) {
					logger.fine("Entity is : " + entity);
					strBuf = EntityUtils.toString(entity);
				} else {
					logger.info("Entity is NULL : " + entity);
				}

				logger.fine("HTTPS COnnection Done");

			} catch (final Exception ex) {
				logger.info("Exception occured with HTTPS : " + ex.getMessage());
			} finally {
				method.releaseConnection();
			}
		} else {

			logger.fine("HTTPs is Disabled.");

			// create get method for page fetch
			final HttpGet getMethod = new HttpGet(path);

			int timeout = 180 * 1000;
			try {
				timeout = Integer.parseInt(StaticProperties.getProperty("alarmTimeout", "180")) * 1000;
			} catch (final Exception e) {
				logger.warning("StaticProperties timeout is not number format."
						+ " Using default timeout 180*1000 seconds");
			}

			try {

				// we try to look like Internet Explorer
				getMethod.addHeader("Accept-Language", "en-us");
				getMethod.addHeader("Accept-Encoding", "gzip, deflate");
				getMethod.addHeader("content-length", "0");
				getMethod.addHeader("accept", "image/gif, " + "image/x-xbitmap, " + "image/jpeg, " + "image/pjpeg, "
						+ "application/x-shockwave-flash, " + "application/vnd.ms-excel, "
						+ "application/vnd.ms-powerpoint, " + "application/msword, */*");
				getMethod.addHeader("User-Agent",
						"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322)");

				// getMethod.setFollowRedirects(false);
				getMethod.getParams().setParameter("http.socket.timeout", timeout);

				try {
					// and fetch the page
					final HttpResponse httpRes = httpclient.execute(getMethod);
					logger.fine("HttpResponse is " + httpRes.toString() + " in HTTP.");

					// check if redirect was requested - if so, fetch that page
					Header locationHeader = getMethod.getFirstHeader("location");

					while (locationHeader != null) {
						String redirectLocation = locationHeader.getValue();

						// I would love to use URLEncoder here, if it would work
						// desired way. Unforutunately all &-chars as well as
						// /-chars
						// get encoded too, and that is not what we want. Doing it
						// old
						// way.
						// It's horrible and not taking account all specials, but...
						redirectLocation = redirectLocation.replaceAll(" ", "\\+");
						redirectLocation = redirectLocation.replaceAll("\\(", "\\%28");
						redirectLocation = redirectLocation.replaceAll("\\)", "\\%29");

						// we are looking this address
						final HttpContext localContext = new BasicHttpContext();
						final HttpUriRequest req = (HttpUriRequest) localContext
								.getAttribute(ExecutionContext.HTTP_REQUEST);
						getMethod.setURI(req.getURI());

						// and fetch the page
						httpclient.execute(getMethod);

						// still looking ?
						locationHeader = getMethod.getFirstHeader("location");
					}

					// HK99289
					final StringBuilder streamBuffer = new StringBuilder();
					final InputStream is = httpRes.getEntity().getContent();
					final BufferedReader br = new BufferedReader(new InputStreamReader(is));
					String line;
					while ((line = br.readLine()) != null) {
						streamBuffer.append(line).append("\n");
					}
					if ((streamBuffer.toString() != null) && (streamBuffer.toString().length() > 0)) {
						strBuf = streamBuffer.toString();
					} else {
						throw new HttpException("HTTP is not responding " + streamBuffer.toString());
					}

				} finally {
					// release the connection
					getMethod.releaseConnection();
				}

			} catch (final Exception e) {
				throw new ACSessionException("Fatal error when getting path: '" + path + " (" + e.getMessage() + ")");
			}
		}

		// and return with page HTML
		// return streamBuffer.toString();
		return strBuf;

		// CR118 --End--
	}

	/**
	 * Closes current session for alarmcfg. Session can not be used after this has been called.
	 */
	@Override
	public void close() throws ACSessionException {
		try {
			active = false;
			final String logoffURL = protocol + "://" + host + "/alarmcfg/Logout";
			getRealURL(logoffURL);
		} catch (final HttpException e) {
			throw new ACSessionException("HttpException");
		} catch (final IOException e) {
			throw new ACSessionException("IOException");
		} catch (final Exception e) {
			throw new ACSessionException("FatalException");
		}
	}
}
