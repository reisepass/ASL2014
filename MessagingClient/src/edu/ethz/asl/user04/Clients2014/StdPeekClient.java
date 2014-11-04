package edu.ethz.asl.user04.Clients2014;

import java.io.Serializable;

import edu.ethz.asl.user04.clientAPI.MessageAPI2014;
import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2014;
import edu.ethz.asl.user04.shared.entity.Message;
import edu.ethz.user04.shared.requests.queuerequests.CreateClientRequest;
import edu.ethz.user04.shared.requests.queuerequests.ReadPrivateMessageRequest;

public class StdPeekClient extends StdClient implements Serializable, Runnable {
	/**
	 * FUN FUN FUN FUN 
	 */
	private static final long serialVersionUID = -1784111427520114075L;
	
	int clientID;
	MessageAPI2014 capi;
	ConfigExperimentV2014 cfg;
	int queueID;
	int sleepBeforeConnectionRetry = 1000;
	int resultChunkSize = 50;

	public StdPeekClient(int clientID,  MessageAPI2014 capi,
			ConfigExperimentV2014 cfg, int queueID) {
		super();
		this.clientID = clientID;
		this.capi = capi;
		this.cfg = cfg;
		this.queueID = queueID;
		
	}






	@Override
	public void run() {
		
			capi.createClient(new CreateClientRequest(this.clientID));
			while((cfg.experimentLength+cfg.experimentStartTime)>System.currentTimeMillis()){
				

				ReadPrivateMessageRequest wrq = new  ReadPrivateMessageRequest(clientID, queueID, false, true);

				wrq.setCFG(cfg);
				
				Message msgBack=capi.readOnePrivateMessage(wrq);
				if(msgBack==null){
					try {
						Thread.sleep(100);
						//TODO SEND Error TO Log 
					} catch (InterruptedException e) {
					
						e.printStackTrace();
					}
				}
				
			}
	}
	
	

}
