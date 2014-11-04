package edu.ethz.user04.shared.requests.queuerequests;

import java.io.Serializable;

import edu.ethz.user04.shared.requests.messagerequests.MessagingSystemRequest;

public class ReadSpecificSender extends MessagingSystemRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public int senderID;   // ID of the client whose message is to be read.
	public int queueID;
	public int auther; 
	
	public boolean removeAfterPeek;
	public boolean orderByTime;
	private int context = 0; // Context = 0 is default for non-Request-Response type messages

	


	
	



	public int getContext() {
		return context;
	}
	
	public void setContext(int context) {
		this.context = context;
	}
	
	public ReadSpecificSender(int senderID, int queueID,
			boolean removeAfterPeek, boolean orderByTime, int auther) {
		super();
		this.senderID = senderID;
		this.auther = auther;
		this.queueID = queueID;
		this.removeAfterPeek = removeAfterPeek;
		this.orderByTime = orderByTime;
	}
	
	public ReadSpecificSender(int senderID, int queueID,
			boolean removeAfterPeek, boolean orderByTime, int auther, int context) {
		this(senderID, queueID, removeAfterPeek, orderByTime, auther);
		this.context = context;
	}
	
}
