package mdload.client;

import mdload.client.ClientRandomStream;
import mdload.client.util.DMPH;
import mdload.client.util.Distribution;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.net.InetAddress;

public class MDLoad {
	static Logger logMain = Logger.getLogger("mdload.client.Overseer");

	public static Distribution fitThinkTime(double mean, double cv, double gamma) {
		DMPH thinkDistrib = new DMPH(2);

		if (gamma == 0) {
			double scv = cv * cv;
			thinkDistrib.setRate(0, 1, 1, -2 / mean);
			thinkDistrib.setRate(0, 1, 2, -mean / (mean * mean - mean * mean * (scv + 1)));
			thinkDistrib.setRate(0, 2, 1, 0.0);
			thinkDistrib.setRate(0, 2, 2, mean / (mean * mean - mean * mean * (scv + 1)));
			thinkDistrib.setRate(1, 1, 1, 2 / mean - (mean * (gamma - 1)) / (mean * mean - mean * mean * (scv + 1)));
			thinkDistrib.setRate(1, 1, 2, (gamma * mean) / (mean * mean - mean * mean * (scv + 1)));
			thinkDistrib.setRate(1, 2, 1, (mean * (gamma - 1)) / (mean * mean - mean * mean * (scv + 1)));
			thinkDistrib.setRate(1, 2, 2, -(gamma * mean) / (mean * mean - mean * mean * (scv + 1)));
		} else {
			thinkDistrib.setRate(0, 1, 1, -3 - 0.0024399);
			thinkDistrib.setRate(0, 1, 2, 0.0024399);
			thinkDistrib.setRate(0, 2, 1, 0.00020246);
			thinkDistrib.setRate(0, 2, 2, -0.021799 - 0.00020246);
			thinkDistrib.setRate(1, 1, 1, 3.0);
			thinkDistrib.setRate(1, 1, 2, 0.0);
			thinkDistrib.setRate(1, 2, 1, 0.0);
			thinkDistrib.setRate(1, 2, 2, 0.021799);
		}
		return thinkDistrib;
	}

	public static void main(String[] args) throws Exception {
		// to stop apache output from flooding console
		Logger.getLogger("org").setLevel(Level.WARN);

		logMain.info("Loading configuration...");
		int runTime = ClientDefs.EXECUTION_TIME_MS;
		String strategy = ClientDefs.STRATEGY;
		logMain.info("Configuration loaded.");
		logMain.info("Steady-state will last " + runTime + " ms");
		logMain.info("Strategy is " + strategy);

		boolean isMaster = InetAddress.getLocalHost().getHostAddress().equals(ClientDefs.CLIENT_MASTER_IP);
		if (isMaster) {
			logMain.info("This client is master");
		} else {
			logMain.info("This client is not master, this is " + InetAddress.getLocalHost().getHostAddress() + ", master is "
					+ ClientDefs.CLIENT_MASTER_IP);
		}

		double mean = ClientDefs.THINK_TIME_MEAN_MS / 1000; // mean is in
		// seconds
		double cv = ClientDefs.THINK_TIME_STDEV_MS / ClientDefs.THINK_TIME_MEAN_MS;
		double gamma = ClientDefs.THINK_TIME_ACF_GEOMDECAY_RATE;
		Distribution thinkTime = fitThinkTime(mean, cv, gamma);

		mean = ClientDefs.SESSION_IAT_MEAN_MS / 1000; // mean is in seconds
		cv = ClientDefs.SESSION_IAT_STDEV_MS / ClientDefs.SESSION_IAT_MEAN_MS;
		gamma = ClientDefs.SESSION_IAT_ACF_GEOMDECAY_RATE;
		Distribution sessionIAT = fitThinkTime(mean, cv, gamma);

		Overseer overseer = new Overseer(thinkTime, sessionIAT, runTime, isMaster);
		overseer.execute();

		System.exit(0);
	}
}
