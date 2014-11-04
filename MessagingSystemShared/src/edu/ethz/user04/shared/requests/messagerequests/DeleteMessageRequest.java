package edu.ethz.user04.shared.requests.messagerequests;

import java.io.Serializable;

public class DeleteMessageRequest extends MessagingSystemRequest implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int clientid;
	public long messageid;
	public DeleteMessageRequest(int clientid, long messageid) {
		super();
		this.clientid = clientid;
		this.messageid = messageid;
	}
	
	public DeleteMessageRequest(long messageid) {
		super();
		this.messageid = messageid;
	}

	public void setClientid(int clientid) {
		this.clientid = clientid;
	}
	
}
