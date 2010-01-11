package agh.mobile.contactexchange.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import agh.mobile.contactexchange.protocol.ClientData;
import agh.mobile.contactexchange.protocol.MessageType;

class Client {
	private static Logger logger = Logger.getLogger("global");

	private final static int PAIRING_TIME_RANGE = 3000; // 3 sec;
	
	
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
			ClientData clientDataMsg = (ClientData) deserializeMessage(val);
			return (clientDataMsg == null ? false : handleClientDataMsg(clientDataMsg));
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


	private Object deserializeMessage(byte[] val) {
		Object msg;
		try {
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(val));
			msg = ois.readObject();
		} catch (IOException e) {
			logger.severe("Cannot deserialize an object. Dropping client's connection.");
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			logger.severe("Class not found. Dropping client's connection.");
			e.printStackTrace();
			return null;
		}
		return msg;
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

	
	public boolean pairsWith(Client client) {
		// TODO: extend matching algorithm with other parameters
		// like position and cellID
		if (Math.abs(client.clientData.time - clientData.time) < PAIRING_TIME_RANGE)
			return true;
		else
			return false;
	}


	public int getPairId() {
		return pairId;
	}

	public int getId() {
		return id;
	}
	
	public void sendPairsList(List<Client> pairedClients) throws IOException {
		Map<Integer, String> pairs = new HashMap<Integer, String>();
		for (Client c : pairedClients) {
			pairs.put(c.id, c.clientData.payload.name);
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		(new ObjectOutputStream(baos)).writeObject(pairs);
		byte [] val = baos.toByteArray();
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
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		(new ObjectOutputStream(baos)).writeObject(client.clientData.payload);
		byte [] val = baos.toByteArray();
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
