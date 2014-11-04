package edu.ethz.user04.shared.requests.messagerequests;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.ethz.asl.user04.shared.entity.Message;

public class WriteMessageRequest extends MessagingSystemRequest implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final int DEFAULT_PRIORITY = 5;
	private static final int DEFAULT_BROADCAST = -1;
	public Message message;
	public boolean isReqestResponse;

	public WriteMessageRequest(Message message) {
		super();
		this.message = message;
	}
	
	public Message getMessage() {
		return message;
	}

	public WriteMessageRequest(Message message, boolean isRequestResponse) {
		super();
		this.message = message;
		this.isReqestResponse = isRequestResponse;
	}
	
	public WriteMessageRequest(int queue, String payload) {
		this(queue, payload, DEFAULT_BROADCAST);
	}
	
	public WriteMessageRequest(List<Integer> queueList, String payload) {
		this(queueList, payload, DEFAULT_BROADCAST);
	}

	public WriteMessageRequest(int queue, String payload, int receiverID) {
		this(DEFAULT_PRIORITY, queue, payload, receiverID);
	}
	
	public WriteMessageRequest(int priority, int queue, String payload) {
		this(priority, queue, payload, DEFAULT_PRIORITY);
	}
	
	public WriteMessageRequest(int priority, int queue, String payload, int receiverID) {
		message = new Message(queue, payload, receiverID);
		message.setPriority(priority);
	}
	
	public WriteMessageRequest(List<Integer> queueList, String payload, int receiverID) {
		this(DEFAULT_PRIORITY, queueList, payload, receiverID);
	}
	
	public WriteMessageRequest(int priority, List<Integer> queueList,
			String payload, int receiverID) {
		// If queueList is empty, default to broadcast
		message = new Message(DEFAULT_BROADCAST, payload, receiverID);
		if (queueList.size() > 1) {
			message = new Message(DEFAULT_BROADCAST, payload, receiverID);
			message.setQueueIdList(queueList);
		}
		message.setPriority(priority);
	}
	
}
