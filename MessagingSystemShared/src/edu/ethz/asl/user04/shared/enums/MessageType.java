package edu.ethz.asl.user04.shared.enums;

public enum MessageType {
	
	ONE_WAY (0),
	REQUEST_RESPONSE (1);
	
	private final int messageType;
	
	MessageType(int messageType) {
		this.messageType = messageType;
	}

	
}
