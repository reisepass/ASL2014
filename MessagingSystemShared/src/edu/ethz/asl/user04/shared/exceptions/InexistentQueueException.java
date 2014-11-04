/**
 * 
 */
package edu.ethz.asl.user04.shared.exceptions;

/**
 * Used when Clients are sending and reading from non-existing queues
 */
public class InexistentQueueException extends MessagingSystemException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected int queueID;
	
	public InexistentQueueException(String message, int queueID) {
		super(message);
		this.queueID = queueID;
	}

	public int getQueueID() {
		return queueID;
	}
}
