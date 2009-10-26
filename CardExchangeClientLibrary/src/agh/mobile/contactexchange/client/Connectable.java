package agh.mobile.contactexchange.client;

import java.util.Map;

import agh.mobile.contactexchange.protocol.Payload;

interface Connectable {

	// called when list of possible exchange partners arrived from the server
	void handlePartnersList(Map<Integer, String> partners);
	
	// called when payload data of chosen partner arrived from server
	void handlePayload(Payload payload);
	
	// called when client connection to server has timed out
	void handleTimedout();
	
	// called when selected partner denied exchange
	void handleExchangeDenied();
	
	// called when client disconnects from server
	void handleDisconnected();
}
