/**
 * 
 */
package edu.ethz.asl.user04.shared.exceptions;

/**
 * Used when clients are unable to create a new queue. This may be because:
 * a. Queue already exists
 * b. Something else went wrong
 */
public class CreateQueueException extends MessagingSystemException {
	
	private static final long serialVersionUID = 1L;
	protected int queueID;
	
	public CreateQueueException(String message, int queueID) {
		super(message);
		this.queueID = queueID;
	}

	public int getQueueID() {
		return queueID;
	}

}
