package edu.ethz.user04.shared.requests.queuerequests;

import java.io.Serializable;

import edu.ethz.user04.shared.requests.messagerequests.MessagingSystemRequest;

/**
 * Get the first result off the top of the queue. Does not check for ownership of message
 * @author mort
 *
 */
public class ReadQueueRequest extends MessagingSystemRequest implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6445762385690365044L;
	public int queueId; 
	public boolean orderByTime;// If tree results are ordered by. If false it uses the default priority
	public boolean removeAfter;//
	public ReadQueueRequest(int queueId, boolean orderByTime,
			boolean removeAfter) {
		super();
		this.queueId = queueId;
		this.orderByTime = orderByTime;
		this.removeAfter = removeAfter;
	}
	

	
}
