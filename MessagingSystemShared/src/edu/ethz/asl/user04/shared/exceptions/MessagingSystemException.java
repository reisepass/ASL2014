package edu.ethz.asl.user04.shared.exceptions;

public class MessagingSystemException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected int thisClientId;
	protected int otherClientId;
	
	public int getThisClientId() {
		return thisClientId;
	}
	public void setThisClientId(int thisClientId) {
		this.thisClientId = thisClientId;
	}
	public int getOtherClientId() {
		return otherClientId;
	}
	public void setOtherClientId(int otherClientId) {
		this.otherClientId = otherClientId;
	}
	
	public MessagingSystemException() {}
	
	public MessagingSystemException(String message) {
		super(message);
	}
	
}
