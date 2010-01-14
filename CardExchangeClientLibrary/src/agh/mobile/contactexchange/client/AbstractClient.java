package agh.mobile.contactexchange.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;

import com.google.code.sntpjc.Client;

public abstract class AbstractClient implements Connectable {

	private final static String ADDRESS_SERVER_URL = "http://student.agh.edu.pl/~wsowa/CEaddr.txt";
	private static final String NTP_SERVER_ADDRESS = "tempus1.gum.gov.pl";
	private static final int NTP_SERVER_TIMEOUT = 1000;
	private static final int NTP_RETRIES_LIMIT = 3;;
	
	private Connection connection = new Connection();
	
	private Client ntpClient;
	private double timeOffset;
	
	public void init() throws IOException {
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
	
	public InetSocketAddress getServerAddress() throws IOException {
		URL url = new URL(ADDRESS_SERVER_URL);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setDoOutput(true);
		con.setReadTimeout(2000);
        
		con.connect();
		
		BufferedReader rd  = new BufferedReader(new InputStreamReader(con.getInputStream()));   
        String line = rd.readLine();
        
        if (line == null)
        	throw new IOException("Cannot read address from address server");
    	
        String addr[] = line.split(" ", 2);
    	return new InetSocketAddress(addr[0], Integer.valueOf(addr[1]));
	}
	
	public long getNtpTime() {
		return getNtpTime(System.currentTimeMillis()); 
	}
	
	public long getNtpTime(long localTime) {
		return localTime + (long) timeOffset * 1000; 
	}
	
	public Connection getConnection() {
		return connection;
	}
}
