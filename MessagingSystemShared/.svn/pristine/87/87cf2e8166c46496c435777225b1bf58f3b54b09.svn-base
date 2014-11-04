package edu.ethz.asl.user04.shared.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
public abstract class Client implements Serializable, Runnable {

	
	
	public abstract ExperimentConfiguration getCFG();
	public abstract void setCFG(ExperimentConfiguration cFG);
	public abstract ArrayList<Integer> getSafeQueueList();
	public abstract void setSafeQueueList(ArrayList<Integer> list);
	public abstract void setUnSafeQueueList(ArrayList<Integer> list);
	
	public abstract ArrayList<Integer> getUnSafeQueueList();
	protected Random rand = new Random();
	public int randomSafeQ(){
		int idx=rand.nextInt(getSafeQueueList().size()) ;
		return getSafeQueueList().get(idx);
	}
	public int randomUnSafeQ(){
		int idx=rand.nextInt(getUnSafeQueueList().size()) ;
		return getUnSafeQueueList().get(idx);
	}
	public abstract  String getUniqueName();

	public abstract void setWaitMilSecBetweenActions(long time);
	
	public abstract int getClientID();
	
	public abstract long getWaitMilSecBetweenActions();
	public abstract void setClientID( int id);
	public abstract Client clone();
	
	public abstract void clientAction();
	public void run(){
		busyWait(getWaitMilSecBetweenActions());
		clientAction();
	}
	private void busyWait(long timeWait){
		long timeStart =System.currentTimeMillis();
		while(System.currentTimeMillis()-timeStart<timeWait){
			double noOp = 7/3; 
		}
	}
		
		
}
