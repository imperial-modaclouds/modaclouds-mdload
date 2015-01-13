package mdload.client;

import mdload.client.workload.*;
import mdload.client.util.Distribution;

import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

public class UserAgent implements Runnable {
	static Logger logAgent = Logger.getLogger("mdload.client.UserAgent");
	static Logger logThink = Logger.getLogger("mdload.client.UserAgent.Think");
	boolean warm;
	private ConcurrentLinkedDeque<Request> requestQueue;
	private long id;
	private int sessionsAssigned;
	private Dispatcher dispatcher;
	private Distribution distribution;
	private WebDriver driver;
	private long lastActionTime;

	public UserAgent(WebDriver driver, Distribution distribution, Dispatcher dispatcher, long seed) {
		this.driver = driver;
		this.sessionsAssigned = 0;
		this.requestQueue = new ConcurrentLinkedDeque<Request>();
		this.distribution = distribution;
		this.dispatcher = dispatcher;
		this.id = seed;

		synchronized (dispatcher) {
			dispatcher.increaseActiveUsers();
		}
		setWarm(false);
		this.lastActionTime = System.currentTimeMillis(); 
	}

	@Override
	/**
	 * Old run method patched with notification method along with triggers realted to the request type
	 */
	public void run() {
		while (true) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
			}
			// return from run method thereby stopping the thread
			if (isCooldown()) {
				return;
			}
			// boolean wasLastNullRequest = false;

			Iterator<Request> iter = requestQueue.iterator();
			boolean emptyQueue = requestQueue.size() == 0;
			while (iter.hasNext()) {
				Request currentRequest = iter.next();
				currentRequest = requestQueue.poll();

				//			System.out.print("*************** ID: " + id);
				Iterator<Request> exceptionIter = requestQueue.iterator();
				while (exceptionIter.hasNext()) {
					System.out.print(" - " + exceptionIter.next().getClass().getSimpleName());
				}
				System.out.print("\n");

				long t0 = System.currentTimeMillis();
				long respTime;
				// active phase
				try {
					if (currentRequest instanceof NullRequest) {
						// wasLastNullRequest = true;
						// do nothing
					} else if (currentRequest instanceof ThinkRequest) {
						// this is useful to inter-session think times
						dispatcher.decreaseActiveUsers();
						long thinkTime = ((ThinkRequest) currentRequest).getThinkTime();
						think(thinkTime);
						dispatcher.increaseActiveUsers();
						logThink.info(System.currentTimeMillis() + "," + (thinkTime) + "," + id + "," + currentRequest.getClass().getSimpleName());
					} else {
						// wasLastNullRequest = false;
						/* think phase */
						String requestName = currentRequest.getClass().getSimpleName();
						double tt = distribution.next();
						// if( !wasLastNullRequest ){ // don't think if the last request was a NullRequest, but sleep always for first one
						long thinkTime = new Double(tt * 1000).longValue();
						//					I don't want injected requests to be delayed by thinking time.
						if (!currentRequest.isInjected()) {
							dispatcher.decreaseActiveUsers();
							think(thinkTime);
							dispatcher.increaseActiveUsers();
						}
						long thinkStamp = System.currentTimeMillis();

						//					################
						logThink.info(System.currentTimeMillis() + "," + (thinkTime) + "," + id + "," + requestName);
						if (!(currentRequest instanceof EndSession) && !(currentRequest instanceof StartSession)) {
				//			dispatcher.notifyEvent(id, thinkStamp, thinkTime, requestName, 1);
						}
						
						//update time of last action before executing new action
						lastActionTime = System.currentTimeMillis();
						//					### The response form the Server is back
						respTime = currentRequest.action(driver);

						if (currentRequest instanceof EndAtomic) {
							dispatcher.injectionQueue.add(this);
						}
						if (currentRequest instanceof EndWarmup) {
							dispatcher.injectionQueue.add(this);
							sessionsAssigned++;
						}

						long respStamp = System.currentTimeMillis();

						//					################
						logAgent.info(System.currentTimeMillis() + "," + (respTime) + "," + id + "," + requestName);
						if (!(currentRequest instanceof EndSession) && !(currentRequest instanceof StartSession)
								&& !(currentRequest instanceof EndAtomic)) {
			//				dispatcher.notifyEvent(id, respStamp, respTime, requestName, -1);
						}
						//					################
					}

				} catch (Exception ex) {

					ex.printStackTrace();
					String requestName = currentRequest.getClass().getSimpleName();
					if (!isWarm()) {
						long respStamp = System.currentTimeMillis();
						respTime = respStamp - t0;
						//					################
						logAgent.info(System.currentTimeMillis() + "," + (respTime) + "," + id + "," + "WARMUPFAILED(" + currentRequest.getClass().getSimpleName() + ")");
	//						dispatcher.notifyEvent(id, respStamp, respTime, requestName, -1);
						//					################
						try {
							currentRequest = new EndSession();
							respTime = currentRequest.action(driver);

							logAgent.info(System.currentTimeMillis() + "," + respTime + "," + id + "," + currentRequest.getClass().getSimpleName());

						} catch (Exception ex1) {
							ex1.printStackTrace();
						}

					} else {
						//					String requestName = currentRequest.getClass().getSimpleName();
						long respStamp = System.currentTimeMillis();
						respTime = respStamp - t0;
						//					################
						logAgent.info(System.currentTimeMillis() + "," + respTime + "," + id + "," + "SESSIONFAILED(" + requestName + ")");
		//				dispatcher.notifyEvent(id, respStamp, respTime, requestName, -1);
						//					################

						try {
							currentRequest = new EndSession();
							// response = new Logout().action( driver, this );
							respTime = currentRequest.action(driver);
							logAgent.info(System.currentTimeMillis() + "," + (respTime) + "," + id + "," + currentRequest.getClass().getSimpleName());
						} catch (Exception ex1) {
							ex1.printStackTrace();
						}
						// break current session and attempt to be dispatched again
						break;
					}
				}
			}

			// initially we are in the warm up phase (registration) increment the number of active users
			if (!isWarm() && !emptyQueue ) {
				setWarm(true);
				synchronized (dispatcher) {
					dispatcher.increaseWarmUsers();
					dispatcher.notify();
				}
			}

			// add self to dispatcher queue
			closeDriver();
			dispatcher.addUserToQueue(this);
		}
	}

	public void closeDriver() {
		// driver.quit();
	}

	public void doCooldown() {
		driver.quit();
	}

	public WebDriver getDriver() {
		return driver;
	}

	public void setDriver(WebDriver driver) {
		this.driver = driver;
	}

	public void think(long thinkingTime) {
		try {
			Thread.sleep(thinkingTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public long getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public long getLastActionTime(){
		return this.lastActionTime; 
	}

	public boolean isCooldown() {
		return dispatcher.isCooldownPhase();
	}

	public boolean isStationary() {
		// 3 means that 2 sessions have been completed including the initial registration at boot.
		return sessionsAssigned >= 1;
	}

	public boolean isWarm() {
		return warm;
	}

	public void setWarm(boolean state) {
		this.warm = state;
	}

	public void setRequests(Collection<Request> requestList, boolean warmup) {
		for (Request r : requestList) {
			requestQueue.add(r);
		}
//		if (!warmup) {
			sessionsAssigned++;
//		}
	}
	
	public void sleep() {
		try {
			wait();
		} catch (InterruptedException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}
}