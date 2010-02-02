package agh.mobile.contactexchange.client.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;

import agh.mobile.contactexchange.client.AbstractClient;
import agh.mobile.contactexchange.protocol.ClientData;
import agh.mobile.contactexchange.protocol.Payload;

public class ConsoleExampleClient extends AbstractClient {

	Map<Integer, String> partners;
	ClientData cd;
	Payload payload;
	boolean finished = false;
	boolean dataSent, pairIdSent;
	
	public ConsoleExampleClient(String name, String phone) throws IOException {
		super();
		
		init();
		
		cd = new ClientData();
		cd.payload = new Payload();
		cd.payload.name = name;
		cd.payload.phoneNumber = phone;
	}
	
	
	/**
	 * Do all client job
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	private void go() throws UnknownHostException, IOException {
		while (!finished) {
			// read data from server
			getConnection().readData();
			// send data if hasn't already
			if(!dataSent) {
				// set NTP time of exchange
				cd.time = getNtpTime();

				// set own location (krakow)
				cd.cellularLatitude = 50.061389;
				cd.cellularLongitude = 19.938333;
				
				//send own data
				getConnection().sendClientData(cd);
				dataSent = true;
			}
			else if (partners == null) {
				// wait for partners list;
				// when the partners list will arrive, the partners variable 
				// will be set in handlePartnersList() method.
			}
			else if (partners.size() == 0) {
				// if received zero partners, finish client program
				System.out.println("No matching partners");
				finished = true;
			}
			else if (!pairIdSent) {
				// if received one or more partners, print the list and ask
				// for partner id to exchange
				System.out.println("Select pertner id:");
			    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			    int id = Integer.decode(in.readLine());
			    
			    // sent selected partner id
			    getConnection().sendPairId(id);
			    pairIdSent = true;
			    System.out.println("Pair id sent");
			}
			else if (payload == null) {
				// wait for payload.
				// when the payload of selected partner will arrive, the payload
				// variable will be set in handlePayload() method.
			}
		}
		
	}

	public void handleDisconnected() {
		System.out.println("Disconnected");
		finished = true;
	}

	public void handleExchangeDenied() {
		System.out.println("Chosen partner denied exchnge");
		finished = true;
	}

	public void handlePartnersList(Map<Integer, String> partners) {
		// store received partners list
		this.partners = partners;
		System.out.println("Reveived partners list:");
		for(Entry<Integer, String> e : partners.entrySet()) {
			System.out.println(e.getKey().toString() + ": " + e.getValue());
		}
	}

	public void handlePayload(Payload payload) {
		// store received partner's payload
		this.payload = payload;
		System.out.println("Received payload:");
		System.out.println("Name:  " + payload.name);
		System.out.println("Phone: " + payload.phoneNumber);
		finished = true;
	}

	public void handleTimedout() {
		System.out.println("Timedout");
		finished = true;
	}

	public static void main(String[] args) {
		if (args.length != 2 && args.length != 4) {
			printUsage();
			return;
		}
		
		// create client
		try {
			ConsoleExampleClient client = new ConsoleExampleClient(args[0], args[1]);
			if (args.length == 4)
				client.getConnection().connect(args[2], Integer.decode(args[3]));
			else
				client.getConnection().connect(client.getServerAddress());
			// start client.
			client.go();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void printUsage() {
		System.out.println("Usage:");
		System.out.println("java ConsoleExampleClient name phoneNumber [serverAddress portNumber]");		
	}
}