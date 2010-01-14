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
	

	private void go() throws UnknownHostException, IOException {
		while (!finished) {
			getConnection().readData();
			if(!dataSent) {
				cd.time = getNtpTime();
				getConnection().sendClientData(cd);
				dataSent = true;
			}
			else if (partners == null) {
				//wait for partners list;
			}
			else if (partners.size() == 0) {
				System.out.println("No matching partners");
				finished = true;
			}
			else if (!pairIdSent) {
				System.out.println("Select pertner id:");
			    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			    int id = Integer.decode(in.readLine());
			    getConnection().sendPairId(id);
			    pairIdSent = true;
			    System.out.println("Pair id sent");
			}
			else if (payload == null) {
				//wait for payload
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
		this.partners = partners;
		System.out.println("Reveived partners list:");
		for(Entry<Integer, String> e : partners.entrySet()) {
			System.out.println(e.getKey().toString() + ": " + e.getValue());
		}
	}

	public void handlePayload(Payload payload) {
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2 && args.length != 4) {
			printUsage();
			return;
		}
		try {
			ConsoleExampleClient client = new ConsoleExampleClient(args[0], args[1]);
			if (args.length == 4)
				client.getConnection().connect(args[2], Integer.decode(args[3]));
			else
				client.getConnection().connect(client.getServerAddress());
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