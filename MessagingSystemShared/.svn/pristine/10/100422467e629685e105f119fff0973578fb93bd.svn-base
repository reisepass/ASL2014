/**
 * 
 */
package edu.ethz.asl.user04.shared.exceptions;

/**
 * Used when Clients are reading from an empty queue
 */
public class EmptyQueueException extends MessagingSystemException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected int queueID;
	
	public EmptyQueueException(String message, int queueID) {
		super(message);
		this.queueID = queueID;
	}

	public int getQueueID() {
		return queueID;
	}

}
