package mdload.client;

import java.util.*;

import mdload.client.ClientDefs;

// import automation.request.SearchAddMicroWidgetToCart;

public class ClientRandomStream {

	public static final Random random = new Random(ClientDefs.RANDOM_SEED);
	
	public ClientRandomStream() {

	}

	public Random getRandom() {
		return random;
	}

}
