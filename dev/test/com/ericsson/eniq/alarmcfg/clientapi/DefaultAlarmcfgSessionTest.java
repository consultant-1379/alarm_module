package com.ericsson.eniq.alarmcfg.clientapi;

import junit.framework.TestCase;

import org.junit.Ignore;

public class DefaultAlarmcfgSessionTest extends TestCase {
  
    public DefaultAlarmcfgSessionTest() {
        super("DefaultAlarmcfgSessionTest");
    }

    @Ignore("Needs to be rewritten for Continuos Integration test")
    public void testGetLargePageNoWarnings(){
//        final AtomicInteger ai = new AtomicInteger(0);
//        final ConsoleAppender ca = new ConsoleAppender(new SimpleLayout()){
//            @Override
//            public void doAppend(LoggingEvent loggingEvent) {
//                ai.incrementAndGet();
//                super.doAppend(loggingEvent);
//            }
//        };
//        Logger.getRootLogger().addAppender(ca);
//        Logger.getRootLogger().setLevel(Level.WARN);
//
//        // This string needs to be big enough to generate the warning if using
//        // the getMethod.getResponseBodyAsString() call in DefaultAlarmcfgSession.getRealURL(...)
//        StringBuilder someLargeText = new StringBuilder();
//        for(int i=0;i<500;i++){
//            for(int x=0;x<800;x++){
//                someLargeText.append(x);
//            }
//            someLargeText.append("\n");
//        }
//        final String contents = someLargeText.toString();
//        someLargeText = null;
//        final String testFile = "test.txt";
//        try {
//            final InetSocketAddress address = new InetSocketAddress("localhost", 8080);
//            final HttpServer httpServer = HttpServer.create(address, 0);
//            final HttpHandler handler = new HttpHandler() {
//                public void handle(HttpExchange exchange) throws IOException {
//                    byte[] response = contents.getBytes();
//                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK,response.length);
//                    exchange.getResponseBody().write(response);
//                    exchange.close();
//                }
//            };
//            httpServer.createContext("/"+testFile, handler);
//            httpServer.start();
//            final Properties p = new Properties();
//            p.put("alarmTimeout", "60x");
//            StaticProperties.giveProperties(p);
//            final Test session = new Test();
//            final Class pcClass = session.getClass();
//	        final Method getRealURL = pcClass.getDeclaredMethod("getRealURL", new Class[] { String.class });
//	        getRealURL.setAccessible(true);
//	        getRealURL.invoke(session, "http://localhost:8080/test.txt");
//            httpServer.stop(0);
//            assertEquals("Zero warnings should have been logged", 0, ai.get());
//        } catch (InvocationTargetException e){
//            e.getCause().printStackTrace();
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
    }
//    class Test extends DefaultAlarmcfgSession{
//        public Test() {
//            httpclient = new HttpClient();
//        }
//
//        @Override
//        protected String getRealURL(String path) throws IOException, ACSessionException {
//            return super.getRealURL(path);
//        }
//    }
}
