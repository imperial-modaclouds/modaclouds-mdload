package mdload.client.exploration;

public class State {
private int id;
private int visits;
private int[] coordinates;

/**
 * Constructor
 * @param id Unique identifier of the state
 */
public State(int id) {
	this.id = id;
	this.visits = 0;
}

/**
 * Constructor by coordinates
 * @param id identifier of the state
 * @param coordinates coordinates of the state vector
 */
public State(int id, int[] coordinates) {
	this.id = id;
	this.visits = 0;
	this.coordinates = new int[coordinates.length];
	for (int i = 0; i < coordinates.length; i++) {
		this.coordinates[i] = coordinates[i];
	}

}

/**
 * printing method
 * @return string representation in the form s(x,y)
 */
public String toString() {
	String coordinates = "";
	String tbp;
	for (int i = 0; i < this.coordinates.length; i++) {
		coordinates += this.coordinates[i] + ",";
	}
	coordinates = coordinates.substring(0, coordinates.length() - 1);

	tbp = "s" + "(" + coordinates + ")";

	return tbp;
}

/**
 * simplified string method
 * @return
 */
public String toStringSimple() {
	String coordinates = "";
	String tbp;
	for (int i = 0; i < this.coordinates.length; i++) {
		coordinates += this.coordinates[i] + ",";
	}
	coordinates = coordinates.substring(0, coordinates.length() - 1);

	tbp = coordinates;

	return tbp;
}


public void incrementVisits() {
	visits++;
}

public int getVisits() {
	return visits;
}

public void setVisits(int visits) {
	this.visits = visits;
}

public int[] getCoordinates() {
	return coordinates;
}

public void setCoordinates(int[] coordinates) {
	this.coordinates = coordinates;
}

}
