package agh.mobile.contactexchange.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import agh.mobile.contactexchange.protocol.Payload;

public class Connection {
	private static Logger logger = Logger.getLogger("global");

	private final static int BUFF_SIZE = 1024*4;

	private Socket socket;
	private OutputStream os;	
	private InputStream is;
	private ByteBuffer inputBuffer;
	private boolean wasConnected;

	private Set<Connectable> listeners;

	
	Connection() {
		inputBuffer = ByteBuffer.allocate(BUFF_SIZE);
		listeners = new HashSet<Connectable>();
	}
	
	public void connect(InetSocketAddress address) throws UnknownHostException, IOException {
		connect(address.getHostName(), address.getPort());
	}
	
	public void connect(String host, int port) throws UnknownHostException, IOException {
		socket = new Socket(host, port);
		os = socket.getOutputStream();
		is = socket.getInputStream();
		wasConnected = true;
	}
	
	public void registerListener(Connectable listener) {
		listeners.add(listener);
	}
	
	public void unregisterListener(Connectable listener) {
		listeners.remove(listener);
	}
	
	public void readData() throws IOException {
		if (wasConnected && !socket.isConnected()) {
			disconnect();
			return;
		}
			
		if(!hasPendingData())
			return;
		
		byte [] buff = new byte[BUFF_SIZE];
		int n = is.read(buff);
		if (n < 0) {
			disconnect();
			return;
		}
		inputBuffer.put(buff, 0, n);
		
		while (inputBuffer.position() >= 8 && inputBuffer.position() - 8 >= inputBuffer.getInt(4)) {
			inputBuffer.flip();
			int type = inputBuffer.getInt();
			int len =  inputBuffer.getInt();
			byte val[] = new byte [len];
			inputBuffer.get(val);
			
			handleData(type, val);
			
			byte rest[] = new byte [inputBuffer.remaining()];
			inputBuffer.get(rest);
			inputBuffer.clear();
			inputBuffer.put(rest);
		}

		
	}
	
	@SuppressWarnings("unchecked")
	private void handleData(int type, byte[] val) throws IOException {
		switch(type) {
		case MessageType.PARTNERS_LIST:
			Map<Integer, String> partners = (Map<Integer, String>) deserializeMessage(val);
			if (partners != null)
				notifyPartnersListArrived(partners);
			else
				disconnect();
			break;
		case MessageType.PAYLOAD:
			Payload payload = (Payload) deserializeMessage(val);
			if (payload != null)
				notifyPayloadArrived(payload);
			else
				disconnect();
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
	
	private void notifyExchangeDenied() {
		for (Connectable listener : listeners)
			listener.handleExchangeDenied();
	}

	private void notifyTimedout() {
		for (Connectable listener : listeners)
			listener.handleTimedout();		
	}

	private void notifyPayloadArrived(Payload payload) {
		for (Connectable listener : listeners)
			listener.handlePayload(payload);
	}

	private void notifyPartnersListArrived(Map<Integer, String> partners) {
		for (Connectable listener : listeners)
			listener.handlePartnersList(partners);
	}
	
	private void notifyDisconnected() {
		for (Connectable listener : listeners)
			listener.handleDisconnected();
	}

	private Object deserializeMessage(byte[] val) {
		Object msg;
		try {
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(val));
			msg = ois.readObject();
		} catch (IOException e) {
			logger.severe("Cannot deserialize an object. Disconnecting.");
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			logger.severe("Class not found. Disconnecting.");
			e.printStackTrace();
			return null;
		}
		return msg;
	}

	public void disconnect() throws IOException {
		notifyDisconnected();
		wasConnected = false;
		os.close();
		is.close();
		socket.close();
		socket = null;
	}
	
	public void sendClientData(ClientData clientData) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeInt(MessageType.CLIENT_DATA);
		
		ByteArrayOutputStream tmp = new ByteArrayOutputStream();
		(new ObjectOutputStream(tmp)).writeObject(clientData);
		
		dos.writeInt(tmp.toByteArray().length);
		dos.write(tmp.toByteArray());

		baos.writeTo(os);
	}
	
	public void sendPairId(int pairId) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeInt(MessageType.PAIR_ID);;
		dos.writeInt(4); // integer length
		dos.writeInt(pairId);
		
		baos.writeTo(os);
	}

	public boolean isConnected() {
		return (socket == null ? false : socket.isConnected());
	}
	
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
