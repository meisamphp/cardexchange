package agh.mobile.contactexchange.protocol;

/**
 * Types of messages being send between client and server.
 * All messages are in format:
 * int32: type
 * int32: body length
 * variant: message body
 * 
 * All numeric data are signed and bigendian. Strings are UTF8 byte arrays
 * preceded with 2-byte unsigned bigendian integer conteining number of bytes
 * in the array.
 * 
 * Correct messages flow is:
 * <ol>
 * <li> CLIENT_DATA
 * <li> PARTNERS_LIST
 * <li> PAIR_ID
 * <li> PAYLOAD
 * </ol>
 * 
 * Alternative messages flow is:
 * <ol>
 * <li> CLIENT_DATA
 * <li> PARTNERS_LIST
 * <li> EXCHANGE_DENIED
 * </ol>
 * 
 * Messages exchange can be interrupted at any moment by the TIMEDOUT
 * message sent from the server to a client.
 * 
 * @author wsowa
 */
public interface MessageType {
	
	/**
	 * Contains client NTP time, location data and data to exchange.
	 * This message is sent from a client to the server. The length of
	 * the message body is variant, but is at least 56 bytes.
	 */
	public static final int CLIENT_DATA     = 0;
	
	/**
	 * Contains list of pairs (id, name) with partners that client
	 * can pair with. This message is sent from the server to a client.
	 * The length of the message body is variant, but is at least 4 bytes.
	 */
	public static final int PARTNERS_LIST   = 1; 
	
	/**
	 * Contains one integer with id of partners client that the client
	 * is willing to exchange with. This message is sent from a client
	 * to the server. The length of the message body is equal 4 bytes.
	 */
	public static final int PAIR_ID         = 2;
	
	/**
	 * Contains data to exchange coming from another client. This message
	 * is sent from the server to a client. The length of the message body
	 * is variant.
	 */
	public static final int PAYLOAD         = 3;
	
	/**
	 * Indicates that client was inactive too long. This message is sent
	 * from the server to a client and has no body.
	 */
	public static final int TIMEDOUT        = 4;
	
	/**
	 * Indicates that selected client didn't accept exchange. This message
	 * is sent from the server to a client and has no body.
	 */
	public static final int EXCHANGE_DENIED = 5;
}
