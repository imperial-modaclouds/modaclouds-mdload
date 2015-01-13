package mdload.client;

import mdload.client.comm.InChannel;
import mdload.client.comm.OutChannel;
import mdload.client.comm.Signal;
import mdload.client.comm.TCPInChannel;
import mdload.client.comm.TCPOutChannel;
import mdload.client.util.*;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Overseer {
static Logger logMain = Logger.getLogger("mdload.client.Overseer");
static Logger logState = Logger.getLogger("mdload.client.Overseer.State");
private OutChannel outboxDispatcher;
private InChannel inbox;
private Thread dispatcherThread;
private Dispatcher dispatcher;
private int runTime;
private boolean isMaster;

public Overseer(Distribution thinkTime, Distribution sessIAT, int runtime, boolean ismaster) {

	inbox = new TCPInChannel(ClientDefs.OVERSEER_PORT);
	dispatcher = new Dispatcher(thinkTime, sessIAT);

	runTime = runtime;
	isMaster = ismaster;

}

public void executeWarmup() throws Exception {
	// warm-up phase

	long t0 = System.currentTimeMillis();
	logMain.info("Warm up initiated at " + t0);
	logState.info("warmup," + t0);

	logMain.info(" Starting warmup...");
	outboxDispatcher.send(new Signal(Signal.DISPATCHER_WARMUP_START));

	logMain.info("Waiting for warmup completion signal...");
	while (inbox.receive().getSignal() != Signal.DISPATCHER_WARMUP_END) {
		Thread.sleep(1000);
	}

	long t1 = System.currentTimeMillis();
	logMain.info("Warm up completed in " + (t1 - t0) + "ms");

}

public void executeSteadyState() {
	// steady-state phase
	long t0 = System.currentTimeMillis();
	logMain.info("Steady state initiated at " + t0);
	logState.info("steady," + t0);
	try {
		Thread.sleep(runTime);
	} catch (InterruptedException e) {
		e.printStackTrace();
	}
	long t1 = System.currentTimeMillis();
	logMain.info("Stable run completed in " + (t1 - t0) + "ms");

}

public void executeCooldown() {
	// cool-down phase
	long t0 = System.currentTimeMillis();
	logMain.info("Cooldown initiated at " + t0);
	logState.info("cooldown," + t0);

	// dispatcherChannel.send(new
	// Signal(Signal.OFBENCH_DISPATCHER_COOLDOWN_START));
	synchronized (dispatcher) {
		dispatcher.setCooldownPhase(true);
	}

	try {
		dispatcherThread.join();
	} catch (InterruptedException e) {
		e.printStackTrace();
	}

	long t1 = System.currentTimeMillis();
	logMain.info("Cooldown completed in " + (t1 - t0) + "ms");
	logState.info("end," + t1);

}

public void execute() throws Exception {
	dispatcherThread = new Thread(dispatcher);
	dispatcherThread.start();

	try {
		outboxDispatcher = new TCPOutChannel(InetAddress.getLocalHost(), ClientDefs.DISPATCHER_PORT);
	} catch (UnknownHostException e) {
		e.printStackTrace();
		System.exit(-1);
	}

	logMain.info("Waiting for dispatcher...");
	long s = inbox.receive().getSignal();

	logMain.info("Dispatcher is ready (signal: " + s + ").");

	if (isMaster) {
		logMain.info("Sending start signals...");
		outboxDispatcher.send(new Signal(Signal.DISPATCHER_WARMUP_START));
		logMain.info("Start signals sent.");
	}

	executeWarmup();
	executeSteadyState();
	executeCooldown();
}

}
