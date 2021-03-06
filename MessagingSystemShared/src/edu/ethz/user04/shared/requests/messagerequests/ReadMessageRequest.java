package edu.ethz.user04.shared.requests.messagerequests;

import java.io.Serializable;


/**
 * Retrieves message from top of queue ordered by priority or by timestamp.
 *  but you can only the messages that are actually for you.
 * @author mort
 *
 */
public class ReadMessageRequest extends MessagingSystemRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int senderID;
	public int queueID;
	public boolean removeAfterPeek;
	public boolean orderByTime;
	public ReadMessageRequest(int sId, int qId, boolean removeAfterPeek, boolean orderByTime){
		senderID = sId;
		queueID = qId;
		this.removeAfterPeek = removeAfterPeek;
		this.orderByTime = orderByTime;
	}

}
