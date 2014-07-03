package mdload.client.exploration;

/**
 * Class: StateEdge
 * User: Fady Kalo
 * Date: 10/07/2013
 * Time: 17:47
 */
public class StateEdge {
//variables.
static int ids = 0;
private int visits;
private int id;

/**
 * Constructor with the visits parameter.
 * @param count numver of the visits
 */
public StateEdge(int count) {
	this.visits = count;
	this.id = ids;
	ids++;
}

public double getVisits() {
	return visits;
}

public void setVisits(int count) {
	this.visits = count;
}

@Override
/**
 * Print the edge in the form e(id,visits)
 */
public String toString() {
	return "E(" + id + "," + visits + ")";
}

public void incrementVisits() {
	visits++;
}
}