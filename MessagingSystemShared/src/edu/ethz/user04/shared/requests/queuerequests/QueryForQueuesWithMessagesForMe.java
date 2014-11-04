package edu.ethz.user04.shared.requests.queuerequests;

import java.io.Serializable;

import edu.ethz.user04.shared.requests.messagerequests.MessagingSystemRequest;

public class QueryForQueuesWithMessagesForMe extends MessagingSystemRequest implements Serializable {
	
	

	private static final long serialVersionUID = 1L;
	public int receiverId;
	
	public QueryForQueuesWithMessagesForMe() {
		super();
	}
	
	public QueryForQueuesWithMessagesForMe(int receiverId) {
		super();
		this.receiverId = receiverId;
	}
	
	public void setReceiverId (int receiverId){
		this.receiverId = receiverId;
	}

}
