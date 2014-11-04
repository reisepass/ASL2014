package edu.ethz.asl.user04.Clients2014;

import java.util.Random;

import edu.ethz.asl.user04.clientAPI.MessageAPI2014;
import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2;
import edu.ethz.user04.shared.requests.queuerequests.CreateClientRequest;
import edu.ethz.user04.shared.requests.queuerequests.ReadPrivateMessageRequest;

public class StdReadPrivate extends StdClient {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int clientID;
	MessageAPI2014 capi;
	ConfigExperimentV2 cfg;
	int queueID;
	int numRanQue;
	boolean removeAfter;
	int sleepBeforeConnectionRetry = 1000;
	int resultChunkSize = 50;
	

	public StdReadPrivate(int clientID, MessageAPI2014 capi,
			ConfigExperimentV2 cfg, int queueID, boolean removeAfter){
		this.cfg=cfg;
		this.queueID = queueID;
		this.capi=capi;
		this.clientID=clientID;
		this.removeAfter=removeAfter;
		
	}
	public StdReadPrivate(int clientID, MessageAPI2014 capi,
			ConfigExperimentV2 cfg, int queueID,boolean removeAfter,int numRanQue){
		
		this( clientID,  capi,cfg,  queueID, removeAfter);
		this.queueID=-1;
		this.numRanQue=numRanQue;
	}

	@Override
	public void run() {
			capi.createClient(new CreateClientRequest(this.clientID));
			Random rand = new Random();
			while((cfg.experimentLength+cfg.experimentStartTime)>System.currentTimeMillis()){
			
				
				int tmpQue=queueID;
				if(queueID==-1){
					tmpQue = rand.nextInt(numRanQue)+1;
				}
				ReadPrivateMessageRequest rpm = new ReadPrivateMessageRequest(this.clientID, tmpQue, removeAfter, true);
				
				capi.readOnePrivateMessage(rpm);
			}
	}
	
}
