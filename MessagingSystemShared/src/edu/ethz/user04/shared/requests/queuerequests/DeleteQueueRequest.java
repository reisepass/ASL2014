package edu.ethz.user04.shared.requests.queuerequests;

import java.io.Serializable;

import edu.ethz.user04.shared.requests.messagerequests.MessagingSystemRequest;
	

public class DeleteQueueRequest extends MessagingSystemRequest implements Serializable {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int queueid;

	public DeleteQueueRequest(int queueid) {
		super();
		this.queueid = queueid;
	}
	
}
