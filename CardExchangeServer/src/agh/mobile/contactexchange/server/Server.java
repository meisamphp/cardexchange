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



/**
 * Main CardExchange server class.
 * 
 * @author wsowa
 */
public class Server {
	private static Logger logger = Logger.getLogger("global");

	/**
	 * Time which must to pass after receiving data from client before
	 * server try to pair clients. This timeout allows client which use
	 * different connections connect to server in different time. 
	 */
	public static final long WAIT_FOR_PAIR_TIMEOUT = 4000; // 4 sec
	
	
	/**
	 * Client's connection will be dropped if client is inactive longer
	 * than this timeout value.
	 */
	public static final long CLIENT_OPERATION_TIMEOUT = 10000; // 10 sec
	
	
	/**
	 * Client's connection will be dropped if client won't select a pair
	 * before this time will pass. This is special case of
	 * CLIENT_OPERATION_TIMEOUT.
	 */
	public static final long PAIR_SELECTION_TIMEOUT = 20000; // 20 sec

	
	/**
	 * Non-blocking read selector. Clients' connections sockets and server
	 * socket are registered with this selector so incoming data can be
	 * handled asynchronously.
	 */
	private Selector selector;;
	
	
	/**
	 * Non-blocking read selection key of server socket.
	 */
	private SelectionKey serverKey;
	
	
	/**
	 * Registered clients. The key is a selection key used by selector to
	 * point that new data has arrived from a client. The value is a {@link Client} object
	 */
	private Map<SelectionKey, Client> clients;
	
	
	/**
	 * Set of all clients IDs that is being passed to the every {@link Client} object.
	 */
	private Set<Integer> ids;
	

	/**
	 * Whether the client is running or not.
	 */
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
	
	
	/**
	 * Start server to listen on incoming connection. This method will
	 * return when the {@link #stop()} will be called from another thread
	 * or an error IOException will occur.
	 * 
	 * @throws IOException connection error occurred.
	 */
	public void serve() throws IOException {
		isRunning = true;
		
		// server while the server is running
		while (isRunning) {
			
			// look for pairs and send potential partners lists
			sendPairsLists();
			
			// send payload data to clients which were successfully paired
			exchengePayloads();
			
			// remove clients that are idle too long.
			removeTimeoutedClients();
			
			// check whether new data or connection arrived. wait at most
			// one second.
			selector.select(1000);
			
			// for connection having any data to read
			for (SelectionKey key : selector.selectedKeys()) {
				
				// if new connection arrived
				if (key == serverKey) {
					if (key.isAcceptable())
						// accept client connection
						addClientConnection();
				}
				
				// if new data arrived from a client
				else if (key.isReadable()) {
					// receive, interpret and handle client data
					handleClientConnection(key);
				}
			}
			selector.selectedKeys().clear();
		}
	}
	
	
	/**
	 * Remove clients that are inactive too long.
	 */
	private void removeTimeoutedClients() {
		Set<SelectionKey> toRemove = new HashSet<SelectionKey>();
		Set<Integer> toDeny = new HashSet<Integer>();
		
		//for each client...
		for (Entry<SelectionKey, Client> entry : clients.entrySet()) {
			Client client = entry.getValue();
			SelectionKey key = entry.getKey();
			
			long timeoutTime, currentTime;
			
			// get the current time and a timeout time of a client.
			// timeout time is depended of a state of a client.
			if (client.hasPairSelected())
				timeoutTime = client.getPairsListSentTime() + PAIR_SELECTION_TIMEOUT + CLIENT_OPERATION_TIMEOUT;
			else if (client.hasSentPairsList())
				timeoutTime = client.getPairsListSentTime() + PAIR_SELECTION_TIMEOUT;
			else if (client.canBePaired())
				timeoutTime = client.getClientDataArrivalTime() + CLIENT_OPERATION_TIMEOUT;
			else
				timeoutTime = client.getCreationTime() + CLIENT_OPERATION_TIMEOUT;
			currentTime = Calendar.getInstance().getTimeInMillis();
			
			// if client has timed out, then sent a "timedout" message and drop connection
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
		
		// remove timeouted clients.
		for (SelectionKey key : toRemove) {
			removeClient(key);
		}
		
		// send "denied" to clients that have selected
		// to pair with client that has timed out.
		denyClients(toDeny);
	}


	/**
	 * Exchange payload between clients.
	 */
	private void exchengePayloads() {
		Set<SelectionKey> toRemove = new HashSet<SelectionKey>();
		Set<Integer> toDeny = new HashSet<Integer>();
		
		// for each client
		for (Entry<SelectionKey, Client> entry1 : clients.entrySet()) {
			Client client1 = entry1.getValue();
			SelectionKey key1 = entry1.getKey();
			
			// omit clients which hasn't selected pair yet.
			if (!client1.hasPairSelected())
				continue;
			
			// for each client
			for (Entry<SelectionKey, Client> entry2 : clients.entrySet()) {
				Client client2 = entry2.getValue();
				SelectionKey key2 = entry2.getKey();
				
				// omit clients which hasn't selected pair yet. don't exchange with itself.
				if (!client2.hasPairSelected() || client2 == client1)
					continue;
				
				// if clients have selected each other...
				if (client2.getPairId() == client1.getId() && client2.getId() == client1.getPairId()) {
					try {
						// exchange payloads
						client1.sendPayload(client2);
					} catch (IOException e) {
						logger.warning("Cannot send payload. Dropping client's connection.");
						e.printStackTrace();
						toRemove.add(key1);
					}
					 
					toRemove.add(key1);
					toDeny.add(client1.getId());
				}
				// if clients' selection isn't symmetrical...
				else if (client2.getPairId() == client1.getId() && client2.getId() != client1.getPairId()) {
					try {
						// send "denied" to another client.
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
		
		// send "exchange denied" to clients which chose to pair with
		// clients that has just exchanged payloads.
		denyClients(toDeny);
	}


	/**
	 * Send "exchange denied" message to clients that has already
	 * selected one of given clients to exchange with.
	 * 
	 * @param toDeny ids of clients which denies exchange.
	 */
	private void denyClients(Set<Integer> toDeny) {
		Set<Integer> toDeny2 = new HashSet<Integer>();
		Set<SelectionKey> toRemove = new HashSet<SelectionKey>();

		// for each client
		for (Entry<SelectionKey, Client> entry : clients.entrySet()) {
			Client client = entry.getValue();
			SelectionKey key = entry.getKey();
			
			// if client has already selected a pair and wants to exchange with
			// a client whose id is in given list, sent a "denied" message.
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
		
		// remove these clients which "denied" message was sent to.
		for (SelectionKey key : toRemove) {
			removeClient(key);
		}
		
		// send a "denied" message to clients who selected denied clients.
		if (toDeny2.size() > 0)
			denyClients(toDeny2);
	}


	/**
	 * Remove and disconnect connection to a client.
	 * 
	 * @param key registration token of a client to remove
	 */
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


	/**
	 * Find pairs of clients that can be paired and distribute
	 * lists of potential exchange partners to these clients.
	 * 
	 * For each set of grouped clients the list will be distributed after
	 * WAIT_FOR_PAIR_TIMEOUT time will expire counting from the moment of
	 * receiving data from first client in the group. That allows to wait
	 * for that from other, potentially paired clients.
	 */
	private void sendPairsLists() {
		Set<SelectionKey> toRemove = new HashSet<SelectionKey>();
		
		// for each clients
		for (Entry<SelectionKey, Client> entry : clients.entrySet()) {
			Client client = entry.getValue();
			SelectionKey key = entry.getKey();
			
			// if the client can't be paired (i.e. hasn't sent own data yet)
			// or has already been paired, go to next client
			if (!client.canBePaired() || client.hasSentPairsList())
				continue;
			
			// don't send client's that has just sent own data. wait some time
			// for new other clients.
			long currentTime = Calendar.getInstance().getTimeInMillis();
			long dataArrivalTime = client.getClientDataArrivalTime();
			if (dataArrivalTime + WAIT_FOR_PAIR_TIMEOUT > currentTime)
				continue;
			
			// try to pair with every other client in the clients list
			// that can be paired. If clients can be paired, add a pair
			// to a list.
			List<Client> pairedClients = new LinkedList<Client>();
			for (Client client2 : clients.values()) {
				if (!client2.canBePaired() || client == client2)
					continue;
				if(client.pairsWith(client2))
					pairedClients.add(client2);
			}
			
			// send partrners list to the client.
			try {
				client.sendPairsList(pairedClients);
			} catch (IOException e) {
				logger.warning("Cannot send pairs list. Dropping client's connection.");
				e.printStackTrace();
				toRemove.add(key);
			}
		}
		
		// remove clients which caused troubles.
		for (SelectionKey key : toRemove) {
			removeClient(key);
		}
	}


	/**
	 * Stop server if it is running in different thread.
	 */
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
	
	
	/**
	 * Accept new client connection.
	 */
	private void addClientConnection() {
		
		// get server socket channel
		ServerSocketChannel server = (ServerSocketChannel) serverKey.channel();
		
		try {
			// accept client connection
			SocketChannel clientChannel = server.accept();
			if (clientChannel == null)
				return;
			
			clientChannel.configureBlocking(false);
			
			// create client object
			Client client = new Client(clientChannel, ids);
			
			// register client connection socket in selector so
			// new incoming data will be picked up in main server loop.
	        SelectionKey clientKey = clientChannel.register(selector, SelectionKey.OP_READ);
	        
	        // put client to clients list.
	        clients.put(clientKey, client);
		}
		catch (IOException e) {
			logger.warning("Error while adding client's connection.");
		}
	}


	/**
	 * Read and handle data incoming from one of registered clients.
	 * 
	 * @param key registration token of client from whom data have
	 * 		   arrived
	 */
	private void handleClientConnection(SelectionKey key) {
		
		// get client object from clients list
		Client client = clients.get(key);
		boolean succeed;
		try {
			// read and handle new data from client connection.
			succeed = client.readData();
		} catch (IOException e) {
			logger.warning("Error while reciving data frim the client. Dropping connection.");
			logger.warning(e.getMessage());
			succeed = false;
		}
		
		if(!succeed) {
			// drop client if error occurred while reading data.
			removeClient(key);
			return;
		}
	}


	public static void main(String[] args) {
		
		int port;
		final Server server;

		// parse command line arguments.
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

		// create server instance
		try {
			server = new Server(port);
		} catch (IOException e) {
			logger.severe("Cannot start server");
			logger.severe(e.getMessage());
			return;
		}
		
		// Add shut down hook for gentle shut down.
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
		
		// starting server
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
