package edu.ethz.user04.shared.requests.queuerequests;

public class CreateReadDeleteQueue {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int operation;
	int queueId;
	public CreateReadDeleteQueue(int op, int qId){
		operation = op;
		queueId = qId;
	}
}
