package agh.mobile.contactexchange.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;



public class Server {
	private static Logger logger = Logger.getLogger("global");

	public static final long WAIT_FOR_PAIR_TIMEOUT = 4000; // 4 sec
	public static final long CLIENT_OPERATION_TIMEOUT = 10000; // 10 sec
	public static final long PAIR_SELECTION_TIMEOUT = 20000; // 20 sec

	private Selector selector;;
	private SelectionKey serverKey;
	private Map<SelectionKey, Client> clients;
	private Set<Integer> ids;
	private boolean isRunning;


	public Server(int port) throws IOException {
		selector = Selector.open();
		ServerSocketChannel server = ServerSocketChannel.open();
		server.socket().bind(new java.net.InetSocketAddress(port));
		server.configureBlocking(false);
	    serverKey = server.register(selector, SelectionKey.OP_ACCEPT);
	    
	    clients = new HashMap<SelectionKey, Client>();
	    ids = new HashSet<Integer>();
	}
	
	
	public void serve() throws IOException {
		isRunning = true;
		
		while (isRunning) {
			sendPairsLists();
			exchengePayloads();
			removeTimeoutedClients();
			selector.select(1000);
			for (SelectionKey key : selector.selectedKeys()) {
				if (key == serverKey) {
					if (key.isAcceptable())
						addClientConnection();
				}
				else if (key.isReadable()) {
					handleClientConnection(key);
					
				}
			}
			selector.selectedKeys().clear();
		}
	}
	
	private void removeTimeoutedClients() {
		Set<SelectionKey> toRemove = new HashSet<SelectionKey>();
		Set<Integer> toDeny = new HashSet<Integer>();
		
		for (Entry<SelectionKey, Client> entry : clients.entrySet()) {
			Client client = entry.getValue();
			SelectionKey key = entry.getKey();
			
			long timeoutTime, currentTime;
			if (client.hasPairSelected())
				timeoutTime = client.getPairsListSentTime() + PAIR_SELECTION_TIMEOUT + CLIENT_OPERATION_TIMEOUT;
			else if (client.hasSentPairsList())
				timeoutTime = client.getPairsListSentTime() + PAIR_SELECTION_TIMEOUT;
			else if (client.canBePaired())
				timeoutTime = client.getClientDataArrivalTime() + CLIENT_OPERATION_TIMEOUT;
			else
				timeoutTime = client.getCreationTime() + CLIENT_OPERATION_TIMEOUT;
			currentTime = Calendar.getInstance().getTimeInMillis();
			
			if (timeoutTime < currentTime) {
				try {
					client.sendTimeOut();
				} catch (IOException e) {
					logger.warning("Cannot TIMEDOUT msg. Dropping client's connection.");
					e.printStackTrace();
					toRemove.add(key);
				}
				toDeny.add(client.getId());
				toRemove.add(key);
			}
		}
		
		for (SelectionKey key : toRemove) {
			removeClient(key);
		}
		
		denyClients(toDeny);
	}


	private void exchengePayloads() {
		Set<SelectionKey> toRemove = new HashSet<SelectionKey>();
		Set<Integer> toDeny = new HashSet<Integer>();
		
		for (Entry<SelectionKey, Client> entry1 : clients.entrySet()) {
			Client client1 = entry1.getValue();
			SelectionKey key1 = entry1.getKey();
			
			if (!client1.hasPairSelected())
				continue;
			
			for (Entry<SelectionKey, Client> entry2 : clients.entrySet()) {
				Client client2 = entry2.getValue();
				SelectionKey key2 = entry2.getKey();
				
				if (!client2.hasPairSelected() || client2 == client1)
					continue;
				
				if (client2.getPairId() == client1.getId() && client2.getId() == client1.getPairId()) {
					try {
						client1.sendPayload(client2);
					} catch (IOException e) {
						logger.warning("Cannot send payload. Dropping client's connection.");
						e.printStackTrace();
						toRemove.add(key1);
					}

					toRemove.add(key1);
					toDeny.add(client1.getId());
				}
				else if (client2.getPairId() == client1.getId() && client2.getId() != client1.getPairId()) {
					try {
						client2.sendDeny();
						toRemove.add(key2);
						toDeny.add(client2.getId());
					} catch (IOException e) {
						logger.warning("Cannot send deny msg. Dropping client's connection.");
						e.printStackTrace();
						toRemove.add(key2);
					}
				}
			}
		}
		
		for (SelectionKey key : toRemove) {
			removeClient(key);
		}
		
		denyClients(toDeny);
	}


	private void denyClients(Set<Integer> toDeny) {
		Set<Integer> toDeny2 = new HashSet<Integer>();
		Set<SelectionKey> toRemove = new HashSet<SelectionKey>();

		for (Entry<SelectionKey, Client> entry : clients.entrySet()) {
			Client client = entry.getValue();
			SelectionKey key = entry.getKey();
			
			if (client.hasPairSelected() && toDeny.contains(client.getPairId())) {
				try {
					client.sendDeny();
				} catch (IOException e) {
					logger.warning("Cannot EXCHENGE_DENIED msg. Dropping client's connection.");
					e.printStackTrace();
					toRemove.add(key);
				}
				toDeny2.add(client.getId());
				toRemove.add(key);
			}
		}
		
		for (SelectionKey key : toRemove) {
			removeClient(key);
		}
		
		if (toDeny2.size() > 0)
			denyClients(toDeny2);
	}


	private void removeClient(SelectionKey key) {
		try {
			key.channel().close();
		} catch (IOException e1) {
			logger.warning("Exception while closing connection");
			e1.printStackTrace();
		}
		key.cancel();
		ids.remove(clients.get(key).getId());
		clients.remove(key);
		
		logger.info("Client removed");
	}

	private void sendPairsLists() {
		Set<SelectionKey> toRemove = new HashSet<SelectionKey>();
		for (Entry<SelectionKey, Client> entry : clients.entrySet()) {
			Client client = entry.getValue();
			SelectionKey key = entry.getKey();
			
			if (!client.canBePaired() || client.hasSentPairsList())
				continue;
			
			long currentTime = Calendar.getInstance().getTimeInMillis();
			long dataArrivalTime = client.getClientDataArrivalTime();
			if (dataArrivalTime + WAIT_FOR_PAIR_TIMEOUT > currentTime)
				continue;
			
			List<Client> pairedClients = new LinkedList<Client>();
			for (Client client2 : clients.values()) {
				if (!client2.canBePaired() || client == client2)
					continue;
				if(client.pairsWith(client2))
					pairedClients.add(client2);
			}
			
			try {
				client.sendPairsList(pairedClients);
			} catch (IOException e) {
				logger.warning("Cannot send pairs list. Dropping client's connection.");
				e.printStackTrace();
				toRemove.add(key);
			}
		}
		
		for (SelectionKey key : toRemove) {
			removeClient(key);
		}
	}


	public void stop() {
		System.out.println("Shutting down...");
		
		isRunning = false;
		selector.wakeup();
		
		serverKey.cancel();
		try {
			serverKey.channel().close();
		} catch (IOException e) {}
		for (SelectionKey key : new LinkedList<SelectionKey>(clients.keySet()))
			removeClient(key);
	}
	
	
	private void addClientConnection() {
		ServerSocketChannel server = (ServerSocketChannel) serverKey.channel();
		
		try {
			SocketChannel clientChannel = server.accept();
			if (clientChannel == null)
				return;
			
			clientChannel.configureBlocking(false);
			
			Client client = new Client(clientChannel, ids);
	        SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
	        clients.put(clientKey, client);
		}
		catch (IOException e) {
			logger.warning("Error while adding client's connection.");
		}
	}


	private void handleClientConnection(SelectionKey key) {
		Client client = clients.get(key);
		boolean succeed;
		try {
			succeed = client.readData();
		} catch (IOException e) {
			logger.warning("Error while reciving data frim the client. Dropping connection.");
			logger.warning(e.getMessage());
			succeed = false;
		}
		
		if(!succeed) {
			removeClient(key);
			return;
		}
	}


	public static void main(String[] args) {
		
		int port;
		final Server server;

		if (args.length != 1) {
			printUsage();
			return;
		}
		try {
			port = Integer.decode(args[0]);
		}
		catch (NumberFormatException e) {
			printUsage();
			return;
		}

		try {
			server = new Server(port);
		} catch (IOException e) {
			logger.severe("Cannot start server");
			logger.severe(e.getMessage());
			return;
		}
		
		Thread hook = new Thread() {
			public void run() {
				server.stop();
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
			
		Runtime.getRuntime().addShutdownHook(hook);
		
		System.out.println("Starting server on port " + port + "...");
		try {
			server.serve();
		} catch (IOException e) {
			logger.severe("Exception occured while running server: ");
			logger.severe(e.getMessage());
			return;
		}
	}

	
	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("java CEServer tcpPortNumber");		
	}

}
