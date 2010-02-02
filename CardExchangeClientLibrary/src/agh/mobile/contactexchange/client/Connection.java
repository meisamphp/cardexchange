package agh.mobile.contactexchange.client;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import agh.mobile.contactexchange.protocol.ClientData;
import agh.mobile.contactexchange.protocol.MessageType;
import agh.mobile.contactexchange.protocol.PairsList;
import agh.mobile.contactexchange.protocol.Payload;

/**
 * Connection to the exchange server
 * 
 * @author wsowa
 *
 */
public class Connection {
	private static Logger logger = Logger.getLogger("global");

	private final static int BUFF_SIZE = 1024*4;

	private Socket socket;
	private OutputStream os;	
	private InputStream is;
	private ByteBuffer inputBuffer;
	private boolean wasConnected;

	/**
	 * Registered listeners for data incoming from server.
	 */
	private Set<Connectable> listeners;

	
	Connection() {
		inputBuffer = ByteBuffer.allocate(BUFF_SIZE);
		listeners = new HashSet<Connectable>();
	}
	
	
	/**
	 * Connect to exchange server
	 * 
	 * @param address server address information.
	 * @throws UnknownHostException when host name cannot be resolved.
	 * @throws IOException when another error occurs while connecting.
	 */
	public void connect(InetSocketAddress address) throws UnknownHostException, IOException {
		connect(address.getHostName(), address.getPort());
	}
	
	
	/**
	 * @param host server host name
	 * @param port TCP port that server listens on.
	 * @throws UnknownHostException when host name cannot be resolved.
	 * @throws IOException when another error occurs while connecting.
	 */
	public void connect(String host, int port) throws UnknownHostException, IOException {
		socket = new Socket(host, port);
		os = socket.getOutputStream();
		is = socket.getInputStream();
		wasConnected = true;
	}
	
	
	/**
	 * Register a listener for notifications about data sent to the client
	 * 
	 * @param listener A listener to register
	 */
	public void registerListener(Connectable listener) {
		listeners.add(listener);
	}
	

	/**
	 * Unregister a notifications listener.
	 * 
	 * @param listener A listener to unregister
	 */
	public void unregisterListener(Connectable listener) {
		listeners.remove(listener);
	}
	
	
	
	/**
	 * Try to read data from the server. This method must be called in a loop
	 * in order to get fresh data from the server. When new data are received
	 * a proper notification is sent to all listeners registered with 
	 * {@link #registerListener(Connectable)}. This method may result in
	 * disconnection when server drops connection while reading.
	 * 
	 * @throws IOException when error occurred while receiving data.
	 */
	public void readData() throws IOException {
		
		// disconnect if connection was droped
		if (wasConnected && !socket.isConnected()) {
			disconnect();
			return;
		}

		// return if no data is available
		if(!hasPendingData())
			return;
		
		//read data from connection
		byte [] buff = new byte[BUFF_SIZE];
		int n = is.read(buff);
		
		// disconnect if connection was already closed
		if (n < 0) {
			disconnect();
			return;
		}
		
		// put data to buffer
		inputBuffer.put(buff, 0, n);
		
		// while received data contain at least one message...
		while (inputBuffer.position() >= 8 && inputBuffer.position() - 8 >= inputBuffer.getInt(4)) {
			inputBuffer.flip();
			
			// get message type, length and body
			int type = inputBuffer.getInt();
			int len =  inputBuffer.getInt();
			byte val[] = new byte [len];
			inputBuffer.get(val);
			
			// handle message
			handleData(type, val);
			
			// move remaining bytes to beginning of a buffer.
			byte rest[] = new byte [inputBuffer.remaining()];
			inputBuffer.get(rest);
			inputBuffer.clear();
			inputBuffer.put(rest);
		}
		
	}
	
	
	/**
	 * Deserialize message and notify listeners that the message
	 * was received from the server.
	 * 
	 * @param type message type
	 * @param val message body
	 * @throws IOException if error occurred while deserializing message.
	 */
	private void handleData(int type, byte[] val) throws IOException {
		switch(type) {
		case MessageType.PARTNERS_LIST:
			PairsList partners = new PairsList();
			partners.fromByteArray(val);
			notifyPartnersListArrived(partners);
			break;
		case MessageType.PAYLOAD:
			Payload payload = new Payload();
			payload.fromByteArray(val);
			notifyPayloadArrived(payload);
			break;
		case MessageType.TIMEDOUT:
			notifyTimedout();
			break;
		case MessageType.EXCHANGE_DENIED:
			notifyExchangeDenied();
			break;
		default:
			logger.warning("Wrong message type. Disconnecting.");
			disconnect();
		}
	}
	
	
	/**
	 * Notify listeners that server sent "exchange denied" message.
	 */
	private void notifyExchangeDenied() {
		for (Connectable listener : listeners)
			listener.handleExchangeDenied();
	}
	

	/**
	 * Notify listeners that server sent "timeout" message.
	 */
	private void notifyTimedout() {
		for (Connectable listener : listeners)
			listener.handleTimedout();		
	}

	
	/**
	 * Notify listeners that server sent another client exchange data
	 * 
	 * @param payload Exchange data from selected partner.
	 */
	private void notifyPayloadArrived(Payload payload) {
		for (Connectable listener : listeners)
			listener.handlePayload(payload);
	}

	
	/**
	 * Notify listeners that server sent potential partners list
	 * 
	 * @param partners a Map with potential partners with a partner id
	 * in key and a name in value.
	 */
	private void notifyPartnersListArrived(Map<Integer, String> partners) {
		for (Connectable listener : listeners)
			listener.handlePartnersList(partners);
	}
	
	
	/**
	 * Notify listeners about disconnection.
	 */
	private void notifyDisconnected() {
		for (Connectable listener : listeners)
			listener.handleDisconnected();
	}

	/**
	 * Disconnect from a server.
	 * 
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		notifyDisconnected();
		wasConnected = false;
		os.close();
		is.close();
		socket.close();
		socket = null;
	}
	
	
	/**
	 * Sent a client data message to the server.
	 * 
	 * @param clientData own time, position and exchange data.
	 * @throws IOException if error occurred while sending message
	 */
	public void sendClientData(ClientData clientData) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		// serialize object
		byte cd[] = clientData.toByteArray();

		// put message type, length and body to a buffer
		dos.writeInt(MessageType.CLIENT_DATA);
		dos.writeInt(cd.length);
		dos.write(cd);

		// send message
		baos.writeTo(os);
	}
	
	
	/**
	 * Sent to the server the id of a client selected from a partners list.
	 * 
	 * @param pairId the id of another client that the client wants to pair
	 * with. 
	 * @throws IOException client that the client wants to pair with.
	 */
	public void sendPairId(int pairId) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);

		// put message type, length and body to a buffer
		dos.writeInt(MessageType.PAIR_ID);;
		dos.writeInt(4); // integer length
		dos.writeInt(pairId);
		
		// send message
		baos.writeTo(os);
	}
	

	/**
	 * @return True if connected to the server.
	 */
	public boolean isConnected() {
		return (socket == null ? false : socket.isConnected());
	}
	
	/**
	 * @return True if new data can be read from the server,
	 */
	public boolean hasPendingData() {
		if (!isConnected())
			return false;
		
		try {
			return (is.available() > 0);
		} catch (IOException e) {
			return false;
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		if (isConnected())
			disconnect();
		super.finalize();
	}
}
