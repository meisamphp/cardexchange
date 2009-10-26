package agh.mobile.contactexchange.protocol;

public interface MessageType {
	public static final int CLIENT_DATA     = 0;
	public static final int PARTNERS_LIST   = 1; 
	public static final int PAIR_ID         = 2;
	public static final int PAYLOAD         = 3;
	public static final int TIMEDOUT        = 4;
	public static final int EXCHANGE_DENIED = 5;
}
