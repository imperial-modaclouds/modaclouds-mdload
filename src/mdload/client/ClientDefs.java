package mdload.client;

import java.io.InputStream;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.openqa.selenium.firefox.FirefoxProfile;

public class ClientDefs
{
	public static int CACHING;
	public static int TOTAL_USERS = 1;
	public static String CLIENT_MASTER_IP;
	public static long RANDOM_SEED = 0L;
	public static String BASE_IP;
	public static int BASE_PORT = 8080;
	public static String BASE_URL;

	public static int MONITORING_PORT = 9876;
	public static int OVERSEER_PORT = 9877;
	public static int DISPATCHER_PORT = 9878;

	public static int THINK_TIME_MEAN_MS = 7000;
	public static int THINK_TIME_STDEV_MS = 7000;
	public static double THINK_TIME_ACF_GEOMDECAY_RATE = 0.0;
	public static int SESSION_IAT_MEAN_MS = 7000;
	public static int SESSION_IAT_STDEV_MS = 7000;
	public static double SESSION_IAT_ACF_GEOMDECAY_RATE = 0.0;
	public static int EXECUTION_TIME_MS = 60000;
	public static int PAGELOAD_TIMEOUT_MS = 30000;
	public static int IMPLICIT_WAIT_MS = 30000;

	//default strategy to be used
	public static String WORKLOAD_CLASS;
	public static String WORKLOAD_JAR;
	public static String STRATEGY;
	public final static Properties properties = new Properties();
	static Logger logger = Logger.getLogger( ClientDefs.class );

	// properties overridden from default values on loading of these definitions
	static
	{
		String file = "/config.client";

		try
		{
			logger.info( "loading from file: " + file );
			String workingDir =System.getProperty("user.dir");
			InputStream stream = ClientDefs.class.getResourceAsStream( file );
			//FileInputStream stream = new FileInputStream(workingDir+file); 
			if( stream == null ) throw new Exception( "config file not found" );

			properties.load( stream );

			Field[] fields = ClientDefs.class.getFields();

			for( int i = 0; i < fields.length; i++ )
			{
				String type = fields[ i ].getType().getName();
				String field = fields[ i ].getName();
				String value = properties.getProperty( field );

				logger.info( "encountered field " + field + " with property type '" + type + "' and value " + value );

				if( field.equals( "properties" ) || field.equals( "logger" ) ) continue;
				else if( value == null ) throw new Exception( "field " + field + " does" +
						" not have a matching property in the config file" );
				else if( type.equals( "double" ) )
				{
					fields[ i ].setDouble( null, new Double( value ) );
				}
				else if( type.equals( "int" ) )
				{
					fields[ i ].setInt( null, new Integer( value ) );
				}
				else if( type.equals( "boolean" ) )
				{
					fields[ i ].setBoolean( null, new Boolean( value ) );
				}
				else if( type.equals( "java.lang.String" ) )
				{
					fields[ i ].set( null, value );
				}

				logger.debug( fields[ i ].getName() + " set to " + value );
			}
		}
		catch( Exception e )
		{
			logger.warn( "failed to fetch properties: " + e.getMessage() + ", using default values" );
		}
	}

	public final static FirefoxProfile getBrowserProfile() {
		FirefoxProfile profile = new FirefoxProfile();
		profile.setAcceptUntrustedCertificates(true);
		if (ClientDefs.CACHING == 0) {
			profile.setPreference("browser.cache.disk.enable", false);
			profile.setPreference("browser.cache.memory.enable", false);
			profile.setPreference("browser.cache.offline.enable", false);
			profile.setPreference("network.http.use-cache", false);
		} else {
			profile.setPreference("browser.cache.disk.enable", true);
			profile.setPreference("browser.cache.memory.enable", true);
			profile.setPreference("browser.cache.offline.enable", true);
			profile.setPreference("network.http.use-cache", true);
		}
		profile.setPreference("general.autoScroll", false);
		profile.setPreference("general.warnOnAboutConfig", false);
		profile.setPreference("security.default_personal_cert", "Select Automatically");
		profile.setPreference("security.dialog_enable_delay", false);
		profile.setPreference("security.warn_entering_weak", false);
		profile.setPreference("security.warn_entering_weak.show_once", false);
		profile.setPreference("security.warn_entering_mixed", false);
		profile.setPreference("security.warn_entering_mixed.show_once", false);
		profile.setPreference("security.warn_viewing_mixed", false);
		profile.setPreference("security.warn_viewing_mixed.show_once", false);
		profile.setPreference("security.warn_submit_insecure", false);
		profile.setPreference("security.warn_submit_insecure.show_once", false);
		//	profile.setPreference("browser.sessionstore.enabled*", false);
		
		// temporary preferences for manual proxy setting in firefox
		//profile.setPreference("network.proxy.socks", "127.0.0.1");
		//profile.setPreference("network.proxy.type", 1);
		//profile.setPreference("network.proxy.socks_port", 1080);
		
		return profile;
	}
}
