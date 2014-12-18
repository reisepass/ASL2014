package edu.ethz.asl.user04.Clients2014;

import java.io.Serializable;
import java.util.Random;

import edu.ethz.asl.user04.clientAPI.MessageAPI2014;
import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2014;
import edu.ethz.asl.user04.shared.entity.Message;
import edu.ethz.asl.user04.shared.entity.StatTrack;
import edu.ethz.user04.shared.requests.messagerequests.DoNothingJustReturn;
import edu.ethz.user04.shared.requests.messagerequests.WriteMessageRequest;
import edu.ethz.user04.shared.requests.queuerequests.CreateClientRequest;

public class StdNoDBBufferedWait extends StdClient implements Serializable, Runnable {
	/**
	 * FUN FUN FUN FUN
	 */
	private static final long serialVersionUID = -1784111427520114075L;

	int clientID;
	String message;
	MessageAPI2014 capi;
	ConfigExperimentV2014 cfg;
	int queueID;
	int messageLength;
	int sleepBeforeConnectionRetry = 1000;
	int resultChunkSize = 50;
	long buffer = 700;

	public StdNoDBBufferedWait(int clientID, int messageLength, MessageAPI2014 capi,
			ConfigExperimentV2014 cfg, int queueID) {
		super();
		this.clientID = clientID;
		this.messageLength = messageLength;
		this.capi = capi;
		this.cfg = cfg;
		this.queueID = queueID;

	}

	public StdNoDBBufferedWait(int clientID, MessageAPI2014 capi,
			ConfigExperimentV2014 cfg, long buffer) {
		super();
		this.clientID = clientID;
		this.capi = capi;
		this.cfg = cfg;
		this.buffer = buffer;

	}
	
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";


	@Override
	public void run() {
		capi.createClient(new CreateClientRequest(this.clientID));
		while ((cfg.experimentLength + cfg.experimentStartTime) > System
				.currentTimeMillis()) {
			message = "";
			Message prepMessage = new Message(clientID, queueID, message);
			DoNothingJustReturn wrq = new DoNothingJustReturn(prepMessage);

			wrq.setCFG(cfg);

			//System.out.println("SendingMsg" + clientID);
			 
			StatTrack timing = capi.sendJustReturn(wrq);
			 
		Long waittime = buffer-timing.clRoundTime;
			if(waittime<0){
				waittime=(long) 0;
			}
				try {
					Thread.sleep(waittime);
					// TODO SEND Error TO Log
				} catch (InterruptedException e) {

					e.printStackTrace();
				}
			
		}
		System.out.println(" Finished Client "+clientID);
	}

}
