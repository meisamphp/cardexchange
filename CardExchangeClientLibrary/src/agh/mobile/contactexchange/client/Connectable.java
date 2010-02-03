package agh.mobile.contactexchange.client;

import java.util.Map;

import agh.mobile.contactexchange.protocol.Payload;

/**
 * This interface must be implemented by Card Exchange clients in
 * order to be notified about messages received from the server.
 * Each of these methods may be called right after new data was received.
 * 
 * @author Witold Sowa <witold.sowa@gmail.com>
 *
 */
interface Connectable {

	/**
	 * This method is called when list of possible exchange partners
	 * arrived from the server.
	 * 
	 * @param partners list that arrived from the server.
	 */
	void handlePartnersList(Map<Integer, String> partners);
	
	/**
	 * This method is called when exchange data of another client
	 * arrived from the server.
	 * 
	 * @param payload another client exchange data
	 */
	void handlePayload(Payload payload);
	
	/**
	 * This method is called when TIMEOUT message is received.
	 */
	void handleTimedout();
	
	/**
	 * This method is called when EXCHANGE_DENIED message is received.
	 */
	void handleExchangeDenied();
	
	/**
	 * This method is called when has disconnected from the server.
	 */
	void handleDisconnected();
}
