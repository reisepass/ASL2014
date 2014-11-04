/**
 * 
 */
package edu.ethz.asl.user04.shared.exceptions;

/**
 * Used when clients want to delete a queue.
 */
public class DeleteQueueException extends MessagingSystemException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected int queueID;
	
	public DeleteQueueException(String message, int queueID) {
		super(message);
		this.queueID = queueID;
	}

	public int getQueueID() {
		return queueID;
	}
}
