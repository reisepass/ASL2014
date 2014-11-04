package edu.ethz.asl.user04.shared.entity;

import java.io.Serializable;

public class ConfigExperimentV2 implements Serializable {
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7643385531989657456L;
	public final int numberOfMessageHandlerThreads;
	public final int numberOfDBConnections;
	public final int numberOfMiddleWareMachines;
	
	public final int numberSafeQueues;// AKA never deleted, NEver empty 
	public final int num2WayMessage;
	public final int numSendMessageOnly;
	public final int numPullMessage;// AKA delete after reading 
	public final int numPeekMessage;
	public final int numRealisticMixture;
	public final int numSendMultiMessage;
	public final int numReadAllRelevant;
	public final double dbFillLevelPerQueue;
	public final int messageLength;
	public final long experimentStartTime; //in Epoch Mills This should be set automatically
	public final long experimentLength; // In milliseconds 
	
	public ConfigExperimentV2(){
		super();
		this.numberOfMessageHandlerThreads = 25;
		this.numberOfDBConnections = 25;
		this.numberOfMiddleWareMachines = 1;
	
		this.numberSafeQueues = 5;
		this.num2WayMessage = 0;
		this.numSendMessageOnly = 0;
		this.numPullMessage = 0;
		this.numPeekMessage = 0;
		this.numRealisticMixture = 0;
		this.numSendMultiMessage = 0;
		this.numReadAllRelevant = 0;
		this.dbFillLevelPerQueue = 10000.0;
		this.messageLength = 100;
		this.experimentStartTime=System.currentTimeMillis();
		this.experimentLength=1000*120;
		
	}
	//TODO make a constructor that takes a single string as input 

	public ConfigExperimentV2(int numberOfMessageHandlerThreads,
			int numberOfDBConnections, int numberOfMiddleWareMachines, 
			int numberSafeQueues, int num2WayMessage, int numSendMessageOnly,
			int numPullMessage, int numPeekMessage, int numRealisticMixture,
			int numSendMultiMessage, int numReadAllRelevant,
			double dbFillLevelPerQueue, int messageLength,
			 long experimentLength) {
		super();
		this.numberOfMessageHandlerThreads = numberOfMessageHandlerThreads;
		this.numberOfDBConnections = numberOfDBConnections;
		this.numberOfMiddleWareMachines = numberOfMiddleWareMachines;
		this.numberSafeQueues = numberSafeQueues;
		this.num2WayMessage = num2WayMessage;
		this.numSendMessageOnly = numSendMessageOnly;
		this.numPullMessage = numPullMessage;
		this.numPeekMessage = numPeekMessage;
		this.numRealisticMixture = numRealisticMixture;
		this.numSendMultiMessage = numSendMultiMessage;
		this.numReadAllRelevant = numReadAllRelevant;
		this.dbFillLevelPerQueue = dbFillLevelPerQueue;
		this.messageLength = messageLength;
		this.experimentStartTime = System.currentTimeMillis();
		this.experimentLength = experimentLength;
	}

	@Override
	public String toString() {
		return "#$@!@ConfigExperimentV2 [numberOfMessageHandlerThreads="
				+ numberOfMessageHandlerThreads + ", numberOfDBConnections="
				+ numberOfDBConnections + ", numberOfMiddleWareMachines="
				+ numberOfMiddleWareMachines + ", numberSafeQueues="
				+ numberSafeQueues + ", num2WayMessage=" + num2WayMessage
				+ ", numSendMessageOnly=" + numSendMessageOnly
				+ ", numPullMessage=" + numPullMessage + ", numPeekMessage="
				+ numPeekMessage + ", numRealisticMixture="
				+ numRealisticMixture + ", numSendMultiMessage="
				+ numSendMultiMessage + ", numReadAllRelevant="
				+ numReadAllRelevant + ", dbFillLevelPerQueue="
				+ dbFillLevelPerQueue + ", messageLength=" + messageLength
				+ ", experimentStartTime=" + experimentStartTime
				+ ", experimentLength=" + experimentLength + "]";
	}
	
	
	

	
	
}
