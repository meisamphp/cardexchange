package agh.mobile.contactexchange.client;

import java.io.IOException;

import com.google.code.sntpjc.Client;

public abstract class AbstractClient implements Connectable {

	private static final String NTP_SERVER_ADDRESS = "tempus1.gum.gov.pl";
	private static final int NTP_SERVER_TIMEOUT = 1000;
	private static final int NTP_RETRIES_LIMIT = 3;;
	
	private Connection connection = new Connection();
	
	private Client ntpClient;
	private double timeOffset;
	
	public AbstractClient() throws IOException {
		ntpClient = new Client(NTP_SERVER_ADDRESS, NTP_SERVER_TIMEOUT);
		
		int tryNumber = 0;
		while (true) {
			try{
				tryNumber++;
				timeOffset = ntpClient.getLocalOffset();
				break;
			} catch (IOException e) {
				if (tryNumber >= NTP_RETRIES_LIMIT)
					throw e;
			}
		}
		
		connection = new Connection();
		connection.registerListener(this);
	}
	
	public long getNtpTime() {
		return System.currentTimeMillis() + (long) timeOffset * 1000; 
	}
	
	public Connection getConnection() {
		return connection;
	}
}
