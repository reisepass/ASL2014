package edu.ethz.user04.shared.requests.queuerequests;

import java.io.Serializable;

import edu.ethz.user04.shared.requests.messagerequests.MessagingSystemRequest;

public class CreateClientRequest extends MessagingSystemRequest implements
		Serializable {
	private static final long serialVersionUID = 1L;
	
	int clientID;
	public CreateClientRequest(int clientID){
		this.clientID=clientID;
	}
	public int getID(){
		return clientID;
	}
	
}