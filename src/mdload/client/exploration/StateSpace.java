package mdload.client.exploration;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Class: StateSpace
 * User: Fady Kalo
 * Date: 10/07/2013
 * Time: 17:47
 */
public class StateSpace {

private Graph space;
private int dimension;
private int ids = 0;
private State currentPos;

/**
 * State space default constructor
 */
public StateSpace() {
	this.space = new UndirectedSparseGraph<State, StateEdge>();
	this.dimension = 0;
}

/**
 * StateSpace constructor by dimension
 *
 * @param dimensions dimensions of the state space in terms of clusters or type of requests
 * @param userAgents number of agents participating to the test.
 */
public StateSpace(int dimensions, int userAgents) {
	this.space = new UndirectedSparseGraph<State, StateEdge>();
	this.dimension = dimensions;
	if (dimensions == 3) {
		for (int i = 0; i <= userAgents; i++) {
//			System.out.println("#states: " + (userAgents - i) + " " + i);
			createPlane(dimensions, i, userAgents);
		}
	} else {
		createPlane(dimensions, 0, userAgents);
	}
	System.out.println("#states: " + ids);
	System.out.println("***************************** State Space Created *****************************");

//	System.out.println(space.toString());

}

/**
 * Method for the creation of a two dimensional state space.
 *
 * @param dim        number of dimension to be used.
 * @param z          coordinates of the third dimension.
 * @param userAgents number of the user agents.
 */
public void createPlane(int dim, int z, int userAgents) {
	int[] coordinates = new int[dim];
	ArrayList thisLine;
	for (int y = 0; y <= userAgents - z; y++) {
		State thisNode;
		thisLine = new ArrayList<State>();
		for (int x = 0; x <= userAgents - y - z; x++) {
			coordinates[0] = x;
			coordinates[1] = y;
			if (dim == 3) {
				coordinates[2] = z;
			}
			thisNode = new State(ids, coordinates);
			space.addVertex(thisNode);
			//This is the starting point for the state space.
			if (x == 0 && y == 0 && z == 0) {
				currentPos = thisNode;
			}
			for (int i = 0; i < coordinates.length; i++) {
				if (coordinates[i] > 0) {
					State targetNode = findState(space.getVertices(), thisNode, -1, i);
					space.addEdge(new StateEdge(0), targetNode, thisNode, EdgeType.UNDIRECTED);
				}
			}
			ids++;
			thisLine.add(thisNode);
		}
	}
}

/**
 * Measure of local distance with the manhattan dinstance
 *
 * @param s input state.
 */
public void localDistance(State s) {
	Collection<StateEdge> edges = space.getIncidentEdges(s);
	Collection<State> nei = space.getNeighbors(s);
	for (State n : nei) {
		StateEdge e = (StateEdge) space.findEdge(s, n);
//		System.out.println("From: "+s+"To: "+n+" #:"+e.getVisits());
	}
}

public State getCurrentPos() {
	return currentPos;
}

public void setCurrentPos(State currentPos) {
	this.currentPos = currentPos;
}

private int totalStates(int dimensions, int userAgents) {
	int total;
	switch (dimensions) {
		case 2:
			total = (userAgents + 1) * userAgents / 2;
			break;
		default:
			total = -1;
			break;
	}
	return total;
}

/**
 * @param event Request or Response, which are respectively represented by a +1 and -1
 * @param index The index of the dimension to be updated, index are stored in the same way as space euclidean vectors.
 */
public void update(int event, int index) {
	Collection<State> neighbors = space.getNeighbors(currentPos);
	for (State n : neighbors) {
//			System.out.println("From: " + s + " To: " + n + " #:" + e.getVisits());
		System.out.println("From: " + currentPos + " To: " + n);
	}
	State nextState = findState(neighbors, currentPos, event, index);
	if (nextState != null) {
		System.out.println(" ");
		StateEdge e = (StateEdge) space.findEdge(currentPos, nextState);
		e.incrementVisits();
		currentPos = nextState;
		//this is incremented when currentPos has successfully being updated to the new current state.
		currentPos.incrementVisits();
	} else {
	}
}

/**
 * Finds a neighbor state given the coordinates of the current state and the event happened
 *
 * @param set   scope of the search function, usually the neighbor set
 * @param curr  current state reference
 * @param event type of event, either a request or a response
 * @param index index of the dimension in the vector
 *
 * @return the state if found, null otherwise.
 */
public State findState(Collection<State> set, State curr, int event, int index) {
	State toReturn = null;
	Iterator iter = set.iterator();
	boolean flag;
	boolean found = false;
	int[] currentCoords = curr.getCoordinates();
	int[] otherCoords;
	int check;
	while (iter.hasNext() && !found) {
		flag = true;
		State comparingState = (State) iter.next();
		otherCoords = comparingState.getCoordinates();
		for (int i = 0; i < currentCoords.length; i++) {
			if (i != index) {
				flag = flag && (currentCoords[i] == otherCoords[i]);
			} else {
				check = currentCoords[i] + event;
				flag = flag && (check == otherCoords[i]);
			}
		}

		if (flag) {
			found = true;
			toReturn = comparingState;
		}
	}
	return toReturn;
}

/**
 * Wraps the method based on the strategy used.
 *
 * @param mode the strategy to use the test with.
 *
 * @return return the target state coherent with the rule specified in the mode parameter.
 */
public State getNextRequest(String mode) {
	State state;

	switch (mode) {
		case "MIN_VISIT_NODE":
			state = leastVisitedState();
			break;
		case "MIN_VISIT_EDGE":
			state = leastVisitedEdge();
			break;
		case "MAX_DISTANCE_ORIGIN":
			state = maxDistanceNode();
			break;
		case "MIRROR":
			state = mirrorRequest();
			break;
		default:
			state = null;
	}
	return state;
}

/**
 * Method not implemented
 * @return nothing now.
 */
private State mirrorRequest() {
	return null;
}

/**
 * calculates the manhattan distance given the vectore representation
 *
 * @param array vector of the coordinates of a state
 *
 * @return manhattan distance of the vector
 */
private int calcDistance(int[] array) {
	int distance = 0;
	for (int i : array) {
		distance += i;
	}
	return distance;
}

/**
 * Calcuation of the node at the maximum distance
 *
 * @return the state of the graph that fits the Max Distance Strategy
 */
public State maxDistanceNode() {
	State targetState;
//	StateEdge targetEdge;
	Collection<StateEdge> neighbors = space.getNeighbors(currentPos);
//	System.out.println("IS NEIGHBORS EMPTY: "+neighbors.isEmpty());
	Iterator iter = neighbors.iterator();
	State thisState = (State) iter.next();
	targetState = thisState;
	int minDistance = calcDistance(thisState.getCoordinates());
	while (iter.hasNext()) {
		thisState = (State) iter.next();
		int thisDistance = calcDistance(thisState.getCoordinates());
		if (thisDistance >= minDistance) {
			minDistance = thisDistance;
			targetState = thisState;
		}
	}

	return targetState;

}

/**
 * Calcuation of the node at the Least Visits
 * @return the state of the graph that fits the Least Visits Strategy
 */
public State leastVisitedState() {
	State targetState;

	Collection<StateEdge> neighbors = space.getNeighbors(currentPos);
	Iterator iter = neighbors.iterator();
	State thisState = (State) iter.next();
	targetState = thisState;
	long minVisits = (long) thisState.getVisits();

	while (iter.hasNext()) {
		thisState = (State) iter.next();
		long thisVisits = (long) thisState.getVisits();
		if (thisVisits <= minVisits) {
			minVisits = thisVisits;
			targetState = thisState;
		}
	}
	return targetState;
}

/**
 * Calcuation of the node at the Least Visits Edge
 * @return the state of the graph that fits the Least Edge Visits Strategy
 */

public State leastVisitedEdge() {
	State targetState;

	Collection<StateEdge> neighbors = space.getNeighbors(currentPos);
	Iterator iter = neighbors.iterator();
	State thisState = (State) iter.next();
	StateEdge thisEdge = (StateEdge) space.findEdge(currentPos, thisState);
	long minVisits = (long) thisEdge.getVisits();
	targetState = thisState;

	while (iter.hasNext()) {
		thisState = (State) iter.next();
		thisEdge = (StateEdge) space.findEdge(currentPos, thisState);
		long thisVisits = (long) thisEdge.getVisits();
		if (thisVisits <= minVisits) {
			minVisits = thisVisits;
//			targetEdge = thisEdge;
			targetState = thisState;
		}
	}
	return targetState;
}

/**
 * @param s1 Current state
 * @param s2 Objective state
 *
 * @return The dimension for which state s1 changed in state s2
 */
public int stateDifference(State s1, State s2) {
	boolean flag = false;
	int direction = 0;
	int[] c1 = s1.getCoordinates();
	int[] c2 = s2.getCoordinates();
	int i = 0;
	while (i < c2.length && !flag) {
		if (c1[i] != c2[i]) {
			flag = true;
//			i'm making them +1 to have positive value, as 0 and -0 would be loose information as they are the same
			if (c1[i] < c2[i]) {
				direction = i + 1;
			} else {
				direction = -(i + 1);
			}
		}
		i++;
	}
	return direction;
}

/**
 * Saves the data into a csv file.
 */
public void toCsv() {
	Collection<State> vertexes = space.getVertices();
	PrintWriter writer = null;
	try {
		writer = new PrintWriter("/home/ubuntu/ofbench-client/results/test/graph_stats.csv", "UTF-8");
	} catch (FileNotFoundException | UnsupportedEncodingException e) {
		e.printStackTrace();
	}
	for (State v : vertexes) {
		if (writer != null) {
			writer.println(v.toStringSimple() + ',' + v.getVisits());
		}
	}

	writer.close();
}

public Graph getSpace() {
	return space;
}

public void setSpace(Graph space) {
	this.space = space;
}

}
