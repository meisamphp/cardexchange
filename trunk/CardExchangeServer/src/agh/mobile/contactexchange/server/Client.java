package agh.mobile.contactexchange.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import agh.mobile.contactexchange.protocol.ClientData;
import agh.mobile.contactexchange.protocol.MessageType;
import agh.mobile.contactexchange.protocol.PairsList;

class Location {
	double lat;
	double lon;
	double acc;
	
	public Location(double lat, double lon, double acc) {
		this.lat = lat;
		this.lon = lon;
		this.acc = acc;
	}
}


/**
 * Server representation of a client. Contains data received from client,
 * transaction state, client id, client data.
 *  
 * @author Witold Sowa <witold.sowa@gmail.com>
 */
class Client {
	private static Logger logger = Logger.getLogger("global");

	private final static int PAIRING_TIME_RANGE = 3000; // 3 sec;
	private final static double DEFAULT_GPS_ACCURACY = 100.0; // 100 m;
	private final static double DEFAULT_CELLULAR_ACCURACY = 10000.0; // 10 km
	
	
	private final static int BUFF_SIZE = 1024*16;
	
	private static int nextId = 0;

	private SocketChannel socketChannel;
	private ByteBuffer messageBuffer;
	private ClientData clientData;
	private long pairSelectionTime = 0;
	private long pairsListSentTime = 0;
	private long clientDataArrivalTime = 0;
	private long creationTime = 0;
	private int id;
	private int pairId;
	private Set<Integer> allIds;
	
	public Client(SocketChannel socketChannel, Set<Integer> ids) {
		this.socketChannel = socketChannel;
		messageBuffer = ByteBuffer.allocate(BUFF_SIZE);
		creationTime = Calendar.getInstance().getTimeInMillis();
		id = nextId++;
		allIds = ids;
		ids.add(id);
	}

	/**
	 * Read data send by the client into a message of appropriate type
	 * and handle that message.
	 * 
	 * @return True if client has sent proper data.
	 * @throws IOException if error while reading data occurred.
	 */
	public boolean readData() throws IOException {
		int n, len, type;
		n = socketChannel.read(messageBuffer);
		
		// connection was broken.
		if (n <= 0) {
			return false;
		}
		
		// first int32 in a message is the message type,
		// second int32 in a message is the message body length
		// while there is enough data to read to receive a message...
		while (messageBuffer.position() > 8 && messageBuffer.position() - 8 >= messageBuffer.getInt(4)) {
			
			messageBuffer.flip();
			
			// get message type
			type = messageBuffer.getInt();
			
			// get message length
			len =  messageBuffer.getInt();
			
			// get message body
			byte val[] = new byte [len];
			messageBuffer.get(val);
			
			// handle message
			if (!handleData(type, val))
				return false;
			
			// move remaining bytes to beginning of a buffer.
			byte rest[] = new byte [messageBuffer.remaining()];
			messageBuffer.get(rest);
			messageBuffer.clear();
			messageBuffer.put(rest);
		}
	
		return true;
	}

	
	/**
	 * Handle message according to its type.
	 * 
	 * @param type Message type. One of
	 * {@link agh.mobile.contactexchange.protocol.MessageType} fields.
	 * @param val body of a message
	 * @return True if message was deserialized and handled correctly.
	 */
	private boolean handleData(int type, byte[] val) {
		switch (type) {

		case MessageType.CLIENT_DATA:
			ClientData clientDataMsg = new ClientData();
			try {
				// deserialize client data message
				clientDataMsg.fromByteArray(val);
			} catch (IOException e) {
				logger.severe("Cannot deserialize ClientData message");
				return false;
			}
			
			// handle client data message
			return handleClientDataMsg(clientDataMsg);
			
		case MessageType.PAIR_ID:
			// deserialize pair id message
			ByteBuffer b = ByteBuffer.allocate(4).put(val);
			b.flip();
			int pairId = b.getInt();
			
			// handle client data message
			return handlePairIdMsg(pairId);
			
		default:
			// incorrect message type
			logger.warning("Wrong message type. Dropping client's connection.");
			return false;
		}
	}

	
	/**
	 * Handles pair id message. This message contain a id of another client
	 * that client wants to pair with
	 * 
	 * @param pairId Id of a client to pair with.
	 * @return True if pairId is id of other client known to server
	 * or false otherwise.
	 */
	private boolean handlePairIdMsg(int pairId) {
		logger.info("Received PairId message");
		
		if (!isPairIdCorrect(pairId)) {
			logger.info("Received PairId is incorrect");
			try {
				// send "denied" if selected pairID was incorrect
				sendDeny();
			} catch (IOException e) {
			}
			return false;
		}
		
		// store selected pair id
		this.pairId = pairId;

		// store selection time
		pairSelectionTime = Calendar.getInstance().getTimeInMillis();
		
		return true;
	}

	
	/**
	 * Handles client data message. This message contain client time and
	 * location and payload he is going to exchange.
	 * 
	 * @param msg cliend data message object.
	 * @return Always True.
	 */
	private boolean handleClientDataMsg(ClientData msg) {
		logger.info("Received ClientData message. id=" + id + ", name=" + msg.payload.name + ", time=" + msg.time);
		
		// store client data and its arrival time.
		clientData = msg;
		clientDataArrivalTime = Calendar.getInstance().getTimeInMillis();
		
		logger.info("GPS pos: " + msg.gpsLatitude + ", " + msg.gpsLongitude + ", " + msg.gpsAccuracy);
		logger.info("Cell pos: " + msg.cellularLatitude + ", " + msg.cellularLongitude + ", " + msg.cellularAccuracy);
		
		// use dafault location accuracy if precise accuracy isn't known.
		if (clientData.gpsAccuracy == 0.0)
			clientData.gpsAccuracy = DEFAULT_GPS_ACCURACY;
		if (clientData.cellularAccuracy == 0.0)
			clientData.cellularAccuracy = DEFAULT_CELLULAR_ACCURACY;
		
		return true;
	}
	
	
	/**
	 * Whether the pair id is correct.
	 * 
	 * @param pairId Pair id to check.
	 * @return True if given id is different than own id and is id
	 * of a client registered in to server.
	 */
	private boolean isPairIdCorrect(int pairId) {
		if (pairId == id || !allIds.contains(pairId))
			return false;
		else
			return true;
	}
	
	
	/**
	 * Whether the client can be paired.
	 * 
	 * @return True if the client data has arrived
	 */
	public boolean canBePaired() {
		return (clientDataArrivalTime > 0);
	}
	
	
	/**
	 * Whether potential pairs list was sent to the client
	 * 
	 * @return True if the list was sent.
	 */
	public boolean hasSentPairsList() {
		return (pairsListSentTime > 0);
	}
	
	
	/**
	 * Whether the client has already selected another client to pair with
	 * 
	 * @return True if selection already happened.
	 */
	public boolean hasPairSelected() {
		return (pairSelectionTime > 0);
	}
	

	/**
	 * @return Time when pair id message arrived
	 */
	public long getPairSelectionTime() {
		return pairSelectionTime;
	}

	
	/**
	 * @return Time when pairs list message was sent.
	 */
	public long getPairsListSentTime() {
		return pairsListSentTime;
	}
	
	
	/**
	 * @return Time when client data message arrived.
	 */
	public long getClientDataArrivalTime() {
		return clientDataArrivalTime;
	}

	
	/**
	 * @return Time when this object was created.
	 */
	public long getCreationTime() {
		return creationTime;
	}

	
	/**
	 * Compute distance between two locations.
	 * 
	 * @param l1 location of one client
	 * @param l2 location of another client
	 * @return distance in metters.
	 */
	private double computeDistance(Location l1, Location l2) {
		double r = 6371000.0; // earth radius
		double toRads = Math.PI/180.0;
				
		return Math.acos(Math.sin(l1.lat*toRads) * Math.sin(l2.lat*toRads) + 
						 Math.cos(l1.lat*toRads) * Math.cos(l2.lat*toRads) *
						 Math.cos(l2.lon*toRads - l1.lon*toRads)) * r;
	}
	
	
	
	/**
	 * Return best known location of a client using accuracy of a GPS and
	 * cellular position data.
	 * 
	 * @param cd client data received from the client
	 * @return Best known location of a client
	 */
	private Location getBetterLocation(ClientData cd) {
		boolean hasGPS, hasCell, useGPS;

		// negative accuracy means that no location data is set.
		hasGPS = cd.gpsAccuracy > 0.0;
		hasCell = cd.cellularAccuracy > 0.0;
		
		// if no location data is available
		if (!hasCell && !hasGPS)
			return null;
		
		// if only GPS position is availabe
		else if (!hasCell)
			useGPS = true;
		
		// if only cellular position is available
		else if (!hasGPS)
			useGPS = false;
		
		// if both position are available and GPS is more accurate
		else if (cd.gpsAccuracy < cd.cellularAccuracy)
			useGPS = true;
		
		// if both position are available and cellular is more accurate
		else
			useGPS = false;
		
		if (useGPS)
			return new Location(cd.gpsLatitude, cd.gpsLongitude, cd.gpsAccuracy);
		else
			return new Location(cd.cellularLatitude, cd.cellularLongitude, cd.cellularAccuracy);
	}
	
	
	
	/**
	 * Check whether the client can be paired with another client.
	 * 
	 * @param client another client to try to pair with
	 * @return True if the client pairs with anther client.
	 */
	public boolean pairsWith(Client client) {
		
		Location l1 = getBetterLocation(clientData);
		Location l2 = getBetterLocation(client.clientData);
		
		// can't pair if location of any client is unknown
		if (l1 == null || l2 == null)
			return false;

		double dist = computeDistance(l1, l2);
		
		logger.info("dist:" + dist);

		// can't pair if client's didin't decide to exchange within
		// small enough time range.
		if (Math.abs(client.clientData.time - clientData.time) >= PAIRING_TIME_RANGE)
			return false;
		
		// can't pair if distance between clients is to high.
		if (dist > l1.acc + l2.acc)
			return false;
		
		// pair otherwise
		return true;
	}


	/**
	 * @return Id of a client that the client wants to pair with.
	 */
	public int getPairId() {
		return pairId;
	}

	
	/**
	 * @return Id of a client
	 */
	public int getId() {
		return id;
	}
	
	
	/**
	 * Send a pairs list message to the client.
	 *  
	 * @param pairedClients a list of clients that tha client can pair with.
	 * @throws IOException if error occurred while sending.
	 */
	public void sendPairsList(List<Client> pairedClients) throws IOException {
		
		// create the PairsList message.
		PairsList pairs = new PairsList();
		
		// put pairs to the message
		for (Client c : pairedClients) {
			pairs.put(c.id, c.clientData.payload.name);
		}
		
		// serialize the message
		byte [] val = pairs.toByteArray();
		int len = val.length;
		ByteBuffer buff = ByteBuffer.allocate(len + 8);
		
		// put message type, length and body to a buffer.
		buff.putInt(MessageType.PARTNERS_LIST);
		buff.putInt(len);
		buff.put(val);

		// send the message.
		buff.flip();
		socketChannel.write(buff);
		
		pairsListSentTime = Calendar.getInstance().getTimeInMillis();
		logger.info("Partners list send");
	}
	

	/**
	 * Send another client's payload to the client
	 * 
	 * @param client another client which payload will be sent.
	 * @throws IOException if error occurred while sending.
	 */
	public void sendPayload(Client client) throws IOException {
		
		// serialize payload data.
		byte [] val = client.clientData.payload.toByteArray();
		int len = val.length;
		ByteBuffer buff = ByteBuffer.allocate(len + 8);
		
		// put message type, length and body to a buffer.
		buff.putInt(MessageType.PAYLOAD);
		buff.putInt(len); 
		buff.put(val);

		// send the message.
		buff.flip();
		socketChannel.write(buff);
		logger.info("Payload send");
	}

	
	/**
	 * Send "denied" message
	 * 
	 * @throws IOException if error occurred while sending.
	 */
	public void sendDeny() throws IOException {
		sendEmptyMsg(MessageType.EXCHANGE_DENIED);
		logger.info("Deny msg send");
	}

	
	/**
	 * Send "timeout" message
	 * 
	 * @throws IOException if error occurred while sending.
	 */
	public void sendTimeOut() throws IOException {
		sendEmptyMsg(MessageType.TIMEDOUT);
		logger.info("Timedout msg send");
	}

	/**
	 * Send empty message of various type
	 * 
	 * @throws IOException if error occurred while sending.
	 */
	private void sendEmptyMsg(int type) throws IOException {
		ByteBuffer buff = ByteBuffer.allocate(8);
		
		buff.putInt(type);
		buff.putInt(0);

		buff.flip();
		socketChannel.write(buff);
	}
}
