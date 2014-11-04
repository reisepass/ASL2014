package edu.ethz.user04.shared.requests.queuerequests;

import java.io.Serializable;

import edu.ethz.user04.shared.requests.messagerequests.MessagingSystemRequest;

public class CreateQueueRequest extends MessagingSystemRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int  queueid;
	public int clientid;
	public String queueName;

	public CreateQueueRequest(int queueid, int clientid, String queueName) {
		this.queueid = queueid;
		this.clientid = clientid;
		this.queueName = queueName;
	}
	
	public CreateQueueRequest(int queueid, String queueName) {
		super();
		this.queueid = queueid;
		this.queueName = queueName;
	}

	public void setClientid(int clientid) {
		this.clientid = clientid;
	}
	

	
	

}
