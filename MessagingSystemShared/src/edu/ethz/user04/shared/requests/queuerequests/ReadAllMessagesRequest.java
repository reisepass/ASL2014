package edu.ethz.user04.shared.requests.queuerequests;

import java.io.Serializable;

import edu.ethz.user04.shared.requests.messagerequests.MessagingSystemRequest;

/**
 * This DOES NOT read messages from a particular sender.
 * This reads messages accessible to this user ONLY, i.e, this provides a way to get:
 * a. Broadcast messages AND
 * b. Messages where receiver_id = this client
 * @author amr && tribhu
 *
 */
public class ReadAllMessagesRequest extends MessagingSystemRequest implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int receiverID;
	public int queueID;
	// TODO Convert to ENUM for better readability
	public boolean removeAfterPeek;
	public boolean orderByTime;
	public int page; // the messages are returned a limited number at a time. So you have to keep requesting the next page until you get an empty page
	public boolean isOrderByTime() {
		return orderByTime;
	}

	public int getContext() {
		return context;
	}

	private int context = 0; // ReadRequest is broadcast (context = 0) by default
	
	/**
	 * DO NOT USE. This needs receiverID explicitly set
	 * Read all private messages 
	 * @param receiverID
	 * @param queueID
	 * @param removeAfterPeek
	 * @param orderByTime
	 * @param page
	 */
	public ReadAllMessagesRequest(int queueID,int receiverID,
			boolean removeAfterPeek, boolean orderByTime, int page) {
		super();
		this.receiverID= receiverID;
		this.queueID = queueID;
		this.removeAfterPeek = removeAfterPeek;
		this.orderByTime = orderByTime;
		this.page = page;
	}
	
	/**
	 * DO NOT USE. This needs receiverID explicitly set
	 * @param queueID
	 * @param removeAfterPeek
	 * @param orderByTime
	 * @param page
	 * @param context
	 */
	public ReadAllMessagesRequest(int queueID,int receiverID,
			boolean removeAfterPeek, boolean orderByTime, int page, int context) {
		
		this(queueID, receiverID, removeAfterPeek, orderByTime, page);
		this.context = context;
	}
	
	public ReadAllMessagesRequest(int queueID, 
			boolean removeAfterPeek, boolean orderByTime, int page) {
		super();
		this.queueID = queueID;
		this.removeAfterPeek = removeAfterPeek;
		this.orderByTime = orderByTime;
		this.page = page;
	}
	
	public void setReceiverID(int receiverID) {
		this.receiverID = receiverID;
	}
	
}
