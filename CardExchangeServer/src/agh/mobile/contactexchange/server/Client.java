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

	public boolean readData() throws IOException {
		int n, len, type;
		n = socketChannel.read(messageBuffer);
		
		if (n <= 0) {
			return false;
		}
		
		while (messageBuffer.position() > 8 && messageBuffer.position() - 8 >= messageBuffer.getInt(4)) {
			messageBuffer.flip();
			type = messageBuffer.getInt();
			len =  messageBuffer.getInt();
			byte val[] = new byte [len];
			messageBuffer.get(val);
			
			if (!handleData(type, val))
				return false;
			
			byte rest[] = new byte [messageBuffer.remaining()];
			messageBuffer.get(rest);
			messageBuffer.clear();
			messageBuffer.put(rest);
		}
	
		return true;
	}

	private boolean handleData(int type, byte[] val) {
		switch (type) {
		case MessageType.CLIENT_DATA:
			ClientData clientDataMsg = new ClientData();
			try {
				clientDataMsg.fromByteArray(val);
			} catch (IOException e) {
				logger.severe("Cannot deserialize ClientData message");
				return false;
			}
			return handleClientDataMsg(clientDataMsg);
		case MessageType.PAIR_ID:
			ByteBuffer b = ByteBuffer.allocate(4).put(val);
			b.flip();
			int pairId = b.getInt();
			return handlePairIdMsg(pairId);
		default:
			logger.warning("Wrong message type. Dropping client's connection.");
			return false;
		}
	}

	private boolean handlePairIdMsg(int pairId) {
		logger.info("Received PairId message");
		if (!isPairIdCorrect(pairId)) {
			logger.info("Received PairId is incorrect");
			try {
				sendDeny();
			} catch (IOException e) {
			}
			return false;
		}
		this.pairId = pairId;
		pairSelectionTime = Calendar.getInstance().getTimeInMillis();
		return true;
	}

	private boolean handleClientDataMsg(ClientData msg) {
		logger.info("Received ClientData message");
		clientData = msg;
		clientDataArrivalTime = Calendar.getInstance().getTimeInMillis();
		
		if (clientData.gpsAccuracy == 0.0)
			clientData.gpsAccuracy = DEFAULT_GPS_ACCURACY;
		if (clientData.cellularAccuracy == 0.0)
			clientData.cellularAccuracy = DEFAULT_CELLULAR_ACCURACY;
		return true;
	}
	
	private boolean isPairIdCorrect(int pairId) {
		if (pairId == id || !allIds.contains(pairId))
			return false;
		else
			return true;
	}
	
	public boolean canBePaired() {
		return (clientDataArrivalTime > 0);
	}
	
	public boolean hasSentPairsList() {
		return (pairsListSentTime > 0);
	}
	
	public boolean hasPairSelected() {
		return (pairSelectionTime > 0);
	}
	
	public long getPairSelectionTime() {
		return pairSelectionTime;
	}

	public long getPairsListSentTime() {
		return pairsListSentTime;
	}
	
	public long getClientDataArrivalTime() {
		return clientDataArrivalTime;
	}

	public long getCreationTime() {
		return creationTime;
	}

	private double computeDistance(Location l1, Location l2) {
		double r = 6371000.0; // earth radius
		double toRads = Math.PI/180.0;
		
		logger.info("lat1:" + l1.lat + ", lon1:" + l1.lon + "; lat2:" + l2.lat + ", lon2:" + l2.lon);
		
		return Math.acos(Math.sin(l1.lat*toRads) * Math.sin(l2.lat*toRads) + 
						 Math.cos(l1.lat*toRads) * Math.cos(l2.lat*toRads) *
						 Math.cos(l2.lon*toRads - l1.lon*toRads)) * r;
	}
	
	private Location getBetterLocation(ClientData cd) {
		boolean hasGPS, hasCell, useGPS;

		hasGPS = cd.gpsAccuracy > 0.0;
		hasCell = cd.cellularAccuracy > 0.0;
		
		if (!hasCell && !hasGPS)
			return null;
		else if (!hasCell)
			useGPS = true;
		else if (!hasGPS)
			useGPS = false;
		else if (cd.gpsAccuracy < cd.cellularAccuracy)
			useGPS = true;
		else
			useGPS = false;
		
		if (useGPS)
			return new Location(cd.gpsLatitude, cd.gpsLongitude, cd.gpsAccuracy);
		else
			return new Location(cd.cellularLatitude, cd.cellularLongitude, cd.cellularAccuracy);
	}
	
	public boolean pairsWith(Client client) {
		
		Location l1 = getBetterLocation(clientData);
		Location l2 = getBetterLocation(client.clientData);
		
		if (l1 == null || l2 == null)
			return false;

		double dist = computeDistance(l1, l2);
		
		logger.info("dist:" + dist);
		
		if (Math.abs(client.clientData.time - clientData.time) >= PAIRING_TIME_RANGE)
			return false;
		if (dist > l1.acc + l2.acc)
			return false;
		
		return true;
	}


	public int getPairId() {
		return pairId;
	}

	public int getId() {
		return id;
	}
	
	public void sendPairsList(List<Client> pairedClients) throws IOException {
		PairsList pairs = new PairsList();
		for (Client c : pairedClients) {
			pairs.put(c.id, c.clientData.payload.name);
		}
		
		byte [] val = pairs.toByteArray();
		int len = val.length;
		ByteBuffer buff = ByteBuffer.allocate(len + 8);
		
		buff.putInt(MessageType.PARTNERS_LIST);
		buff.putInt(len);
		buff.put(val);

		buff.flip();
		socketChannel.write(buff);
		
		pairsListSentTime = Calendar.getInstance().getTimeInMillis();
		logger.info("Partners list send");
	}

	public void sendPayload(Client client) throws IOException {
		byte [] val = client.clientData.payload.toByteArray();
		int len = val.length;
		ByteBuffer buff = ByteBuffer.allocate(len + 8);
		
		buff.putInt(MessageType.PAYLOAD);
		buff.putInt(len); 
		buff.put(val);

		buff.flip();
		socketChannel.write(buff);
		logger.info("Payload send");
	}

	public void sendDeny() throws IOException {
		sendEmptyMsg(MessageType.EXCHANGE_DENIED);
		logger.info("Deny msg send");
	}

	public void sendTimeOut() throws IOException {
		sendEmptyMsg(MessageType.TIMEDOUT);
		logger.info("Timedout msg send");
	}

	private void sendEmptyMsg(int type) throws IOException {
		ByteBuffer buff = ByteBuffer.allocate(8);
		
		buff.putInt(type);
		buff.putInt(0);

		buff.flip();
		socketChannel.write(buff);
	}
}