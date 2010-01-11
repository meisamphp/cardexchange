package agh.mobile.contactexchange.protocol;

public enum ClientLocationType {
	GPS, // GPS based location (latitude/longitude)
	NETWORK, // cell network based location (latitude/longitude)
	CELL_ID,// cell network based location (cell id)
}
