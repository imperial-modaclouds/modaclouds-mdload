package mdload.client;

import mdload.client.comm.InChannel;
import mdload.client.comm.OutChannel;
import mdload.client.comm.Signal;
import mdload.client.comm.TCPInChannel;
import mdload.client.comm.TCPOutChannel;
import mdload.client.workload.*;
import mdload.client.exploration.*;
import mdload.client.util.*;

import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.File;
import java.lang.reflect.Method;

public class Dispatcher implements Runnable {
	static Logger logMain = Logger.getLogger("client.Dispatcher");
	static Logger logActive = Logger.getLogger("client.Dispatcher.Active");
	static Logger logBoot = Logger.getLogger("client.Dispatcher.Boot");
	static Logger logSpace = Logger.getLogger("client.Dispatcher.Space");
	static Logger logGraph = Logger.getLogger("client.Dispatcher.Graph");
	//this is the queue of UserAgents waiting for a new session.
	public LinkedBlockingQueue<UserAgent> dispatchQueue;
	public LinkedBlockingQueue<UserAgent> injectionQueue;
	//list of the agents
	private ArrayList<UserAgent> agents;
	private int warmUsers;
	private int activeUsers;
	private Distribution thinkTime;
	private Distribution sessIAT;
	private boolean cooldown;
	private boolean warmup;
	private boolean boot;
	private InChannel inbox;
	private OutChannel outboxOverseer;
	//State space management variables
	private StateSpace statespace;
	//store a dictionary between the request and the cluster they belong to, either deterministic or dynamically assigned.

	/**
	 * @param gen  Workload generator
	 * @param tt   Thinking time distribution parameter
	 * @param siat Session inter arrival time distribution parameter
	 */
	public Dispatcher(Distribution tt, Distribution siat) {
		dispatchQueue = new LinkedBlockingQueue<UserAgent>();
		injectionQueue = new LinkedBlockingQueue<UserAgent>();
		agents = new ArrayList<>();
		thinkTime = tt;
		sessIAT = siat;
		setBootPhase(true);
		setWarmupPhase(false);
		setCooldownPhase(false);
		setWarmUsers(0);
		setActiveUsers(0);

		/* set up signaling */
		inbox = new TCPInChannel(ClientDefs.DISPATCHER_PORT);
		try {
			outboxOverseer = new TCPOutChannel(InetAddress.getLocalHost(), ClientDefs.OVERSEER_PORT);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Simply runs the dispatcher's thread through boot, warm up, cool down phases.
	 */
	public void run() {
		sleep(5000);

		logMain.info("Sending ready signal...");
		outboxOverseer.send(new Signal(Signal.DISPATCHER_READY));

		logMain.info("Waiting for warm up start signal...");
		while (inbox.receive().getSignal() != Signal.DISPATCHER_WARMUP_START) {
			sleep(5000);
		}
		logMain.info("Warm up start signal received.");
		logMain.info("Starting warm up, please wait...");

		doBoot();
		/* this is an empty loop waiting for the boot to finish */
		while (isBootPhase()) {
			sleep(1000);
			logMain.info("Still in boot phase.");
		}

		setWarmupPhase(true);
		/* this is an empty loop waiting for the warmup to finish */
		while (isWarmupPhase()) {
			doWarmUp();
			logMain.info("Still in warm up phase.");
		}
		outboxOverseer.send(new Signal(Signal.DISPATCHER_WARMUP_END));

		while (!isCooldownPhase()) {
			doSteadyState();
		}
		doCooldown();
		writeGraphStats();
	}

	private void writeGraphStats() {
		// statespace.toCsv();
	}

	/**
	 * Creates a new agent calling the UserAgent constructor passing a new driver and a thikning time.
	 *
	 * @param id ID to set for the new agent
	 *
	 * @return The newly instantiated user agent.
	 */
	private UserAgent newAgent(int id) {
		WebDriver driver = new FirefoxDriver(ClientDefs.getBrowserProfile());
		driver.manage().timeouts().setScriptTimeout(ClientDefs.PAGELOAD_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		driver.manage().timeouts().implicitlyWait(ClientDefs.IMPLICIT_WAIT_MS, TimeUnit.MILLISECONDS);
		return new UserAgent(driver, thinkTime, this, id);
	}

	/**
	 * Inizialization of the agent.
	 *
	 * @param agent instance of the agent to inizialize.
	 */
	private void setAgent(UserAgent agent) {
		//	agent.setRequests(wkldGen.warmupWorkloadDependencies(agent.isRegistered()), true);

		try {
			double sessInterArrival = Math.min(1, sessIAT.next());

			Thread t = new Thread(agent, "agent" + agent.getId());
			t.start();
			agent.think(new Double(sessInterArrival * 1000).longValue());
			logMain.info("next agent will start in " + new Double(sessInterArrival).doubleValue() + " s.");
		} catch (Exception e) {
			e.printStackTrace();
		}
		logMain.debug("agent" + agent.getId() + " arrived and dispatched for registration");

	}

	/**
	 * Boot phase
	 */
	public void doBoot() {

		for (int i = 0; i < ClientDefs.TOTAL_USERS; i++) {
			UserAgent agent = newAgent(i);
			agents.add(agent);
			setAgent(agent);
		}
		logMain.info("**************************** Boot Complete **************************** ");
		setBootPhase(false);
	}

	/**
	 * Warmup Phase
	 */
	public void doWarmUp() {
		UserAgent agent = null;

		try {
			agent = dispatchQueue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long in = System.currentTimeMillis();
		double sample = sessIAT.next();
		long thinkTime = new Double(sample * 1000).longValue();
		long t0 = System.currentTimeMillis();

		try {
			String pathToJar = ClientDefs.WORKLOAD_JAR;     
			
			JarFile jarFile = new JarFile(pathToJar);
			@SuppressWarnings("rawtypes")
			Enumeration e = jarFile.entries();

			URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
			URLClassLoader cl = URLClassLoader.newInstance(urls);

			while (e.hasMoreElements()) {
				JarEntry je = (JarEntry) e.nextElement();
				if(je.isDirectory() || !je.getName().endsWith(".class")){
					continue;
				}
				// -6 because of .class
				String className = je.getName().substring(0,je.getName().length()-6);
				className = className.replace('/', '.');
				@SuppressWarnings("rawtypes")
				Class c = cl.loadClass(className);
			}
			@SuppressWarnings("unchecked")
			Class<? extends Session> workloadClass = (Class<? extends Session>) cl.loadClass("mdload.userdefined.session.Test");
			Object obj = workloadClass.getDeclaredConstructor(long.class).newInstance(agent.getId());
			Method method = workloadClass.getDeclaredMethod("getWarmup", new Class[] {});

			@SuppressWarnings("unchecked")
			Collection<Request> session = (Collection<Request>) method.invoke(obj, new Object[]{});

			agent.setRequests(session, true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Collection< Request > session = wkldGen.testWorkload();
		long t1 = System.currentTimeMillis();

		// time, time to boot, bot id, time to generate workload
		logBoot.info(t1 + "," + (t1 - t0) + "," + agent.getId() + "," + (t0 - in));
		logMain.info("next arrival for agent " + agent.getId() + " in " + thinkTime + " ms");

		int warmedUp = 0;
		for (int i = 0; i < agents.size(); i++) {
			if (agents.get(i).isStationary()) {
				warmedUp++;
			}
		}

		if (warmedUp == agents.size()) {
			logMain.info("**************************** WarmUp Complete **************************** ");
			setWarmupPhase(false);
		}
	}

	/**
	 * Steady State
	 */
	public void doSteadyState() {
		UserAgent agent = null;

		try {
			agent = dispatchQueue.take();
			long in = System.currentTimeMillis();
			double sample = sessIAT.next();
			long thinkTime = new Double(sample * 1000).longValue();
			long t0 = System.currentTimeMillis();

			try {
				String pathToJar = ClientDefs.WORKLOAD_JAR; //"C:/mdload-ofbiz.jar";     
				
				JarFile jarFile = new JarFile(pathToJar);
				@SuppressWarnings("rawtypes")
				Enumeration e = jarFile.entries();

				URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
				URLClassLoader cl = URLClassLoader.newInstance(urls);

				while (e.hasMoreElements()) {
					JarEntry je = (JarEntry) e.nextElement();
					if(je.isDirectory() || !je.getName().endsWith(".class")){
						continue;
					}
					// -6 because of .class
					String className = je.getName().substring(0,je.getName().length()-6);
					className = className.replace('/', '.');
					@SuppressWarnings("rawtypes")
					Class c = cl.loadClass(className);
				}
				@SuppressWarnings("unchecked")
				Class<? extends Session> workloadClass = (Class<? extends Session>) cl.loadClass(ClientDefs.WORKLOAD_CLASS);
				Object obj = workloadClass.getDeclaredConstructor(long.class).newInstance(agent.getId());
				Method method = workloadClass.getDeclaredMethod("getNext", new Class[] {});

				@SuppressWarnings("unchecked")
				Collection<Request> session = (Collection<Request>) method.invoke(obj, new Object[]{});

				agent.setRequests(session, true);
			} catch (Exception e) {
				e.printStackTrace();
			}

			long t1 = System.currentTimeMillis();

			// time, time to boot, bot id, time to generate workload
			logBoot.info(t1 + "," + (t1 - t0) + "," + agent.getId() + "," + (t0 - in));
			logMain.info("next arrival for agent " + agent.getId() + " in " + thinkTime + " ms");

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Cooldown phase
	 */
	public void doCooldown() {
		logMain.info("**************************** Cooldown Started **************************** ");
		for (int i = 0; i < ClientDefs.TOTAL_USERS; i++) {
			agents.get(i).doCooldown();
		}
	}

	/**
	 * Puts the dispatcher's thread on sleep for a given amount of time.
	 *
	 * @param time Duration of the sleep
	 */
	public void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public synchronized void addUserToQueue(UserAgent ua) {
		if (!dispatchQueue.contains(ua)) {
			dispatchQueue.add(ua);
		}
	}

	public synchronized void increaseWarmUsers() {
		warmUsers++;
		logBoot.info(System.currentTimeMillis() + "," + warmUsers + "," + dispatchQueue.size());
	}

	public synchronized void increaseActiveUsers() {
		activeUsers++;
		logActive.info(System.currentTimeMillis() + "," + activeUsers + "," + dispatchQueue.size());
	}

	public synchronized void decreaseActiveUsers() {
		activeUsers--;
		logActive.info(System.currentTimeMillis() + "," + activeUsers + "," + dispatchQueue.size());
	}

	private synchronized int getWarmUsers() {
		return warmUsers;
	}

	private synchronized void setWarmUsers(int warmUsers) {
		this.warmUsers = warmUsers;
	}

	public boolean isBootPhase() {
		return boot;
	}

	public void setBootPhase(boolean boot) {
		this.boot = boot;
	}

	public boolean isWarmupPhase() {
		return warmup;
	}

	public void setWarmupPhase(boolean warmup) {
		this.warmup = warmup;

	}

	public boolean isCooldownPhase() {
		return cooldown;
	}

	public void setCooldownPhase(boolean cooldown) {
		this.cooldown = cooldown;
	}

	private int getActiveUsers() {
		return activeUsers;
	}

	private void setActiveUsers(int activeUsers) {
		this.activeUsers = activeUsers;
	}

}
