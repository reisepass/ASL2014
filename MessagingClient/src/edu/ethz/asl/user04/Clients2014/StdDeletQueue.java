package edu.ethz.asl.user04.Clients2014;

import java.io.Serializable;

import edu.ethz.asl.user04.clientAPI.MessageAPI2014;
import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2014;
import edu.ethz.user04.shared.requests.queuerequests.DeleteQueueRequest;

public class StdDeletQueue extends StdClient implements Serializable, Runnable {
	/**
	 * FUN FUN FUN FUN
	 */
	private static final long serialVersionUID = -1784111427520114075L;

	int clientID;
	String message;
	MessageAPI2014 capi;
	ConfigExperimentV2014 cfg;
	int queueID;
	int sleepBeforeConnectionRetry = 1000;
	int resultChunkSize = 50;
	long maxBack;
	public StdDeletQueue(int clientID, MessageAPI2014 capi,
			ConfigExperimentV2014 cfg, int queueID ) {
		super();
		this.clientID = clientID;
		this.capi = capi;
		this.cfg = cfg;
		this.queueID = queueID;
		this.maxBack = 5000;
	}
	public StdDeletQueue(int clientID, MessageAPI2014 capi,
			ConfigExperimentV2014 cfg, int queueID,long maxBack ) {
		super();
		this.clientID = clientID;
		this.capi = capi;
		this.cfg = cfg;
		this.queueID = queueID;
		this.maxBack = maxBack;

	}



	@Override
	public void run() {
		
		System.out.println("Client Info:  DeleteQueue deleteQueue = waiting "+ (cfg.experimentLength-maxBack) );
		try {
			Thread.sleep( cfg.experimentLength-maxBack );
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		boolean worked = false;
		while(!worked){
			System.out.println("Client Info:  DeleteQueue deleteQueue = wokeup, after"+ (cfg.experimentLength-maxBack)+ " dqID= "+queueID );
			DeleteQueueRequest dqr = new DeleteQueueRequest(queueID);
		    dqr.setCFG(cfg);
			worked = capi.deleteQueue(dqr);
			try {
				Thread.sleep(100);
				// TODO SEND Error TO Log
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}		
		System.out.println(" Finished Client (deleteQueue)"+clientID);
	}

}
