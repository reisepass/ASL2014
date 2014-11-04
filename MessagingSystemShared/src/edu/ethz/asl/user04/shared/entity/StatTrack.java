package edu.ethz.asl.user04.shared.entity;

import java.io.Serializable;

public class StatTrack implements Serializable{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public long clStarts;
	public long clWaitsinMWQ;
	public long mwStarts;
	public long clOutofMWQ;
	public long clGotOutStream;
	public long mwGetsStreams;
	public long mwSentReadyToClient;
	public long mwReceived; 
	public long clSentReqToMW;
	public long mwWaitsinDBQ; // After finishing the experiment you can estimate how many threads in the queue by doing one serial run over the timestamps here 
	public long mwOutofDBQ; // and here and simply adding one to queue at time mwWaitsinDBQ and removeing one at time mwOutofDBQ
	public long mwStartsSendingToDB;
	public long mwRespFromDB;
	public long mwSentRespToCli;
	public long clRespFromMW;
	public long clClosedConn;
	public long mwFinished;
	public int dbRoundTime; //mwRespFromDB - mwWaitsinDBQ  
	public int dbThinkTime;
	public int mwThinkTime; //Calculated on the client 
	public int mwRoundTime;
	public int mwNoQRound;
	public int clThinkTime;
	public int clRoundTime;
	public int mwTimeInDBQ;
	public int clTimeInQ;
	public int qSizeDB;
	public int qSizeMW;
	public int mwNetworkTime;
	public int mwID;
	public int mwThinkTime2;
	public int mwReadyNetTime;
	
	
	public StatTrack(){
		 clStarts = -1;
		 clWaitsinMWQ = -1;
		 mwStarts = -1;
		 clOutofMWQ = -1;
		 clGotOutStream= -1;
		 mwSentReadyToClient= -1;
		 mwGetsStreams = -1;
		 mwReceived = -1; 
		 clSentReqToMW = -1;
		 mwWaitsinDBQ = -1; // After finishing the experiment you can estimate how many threads in the queue by doing one serial run over the timestamps here 
		 mwOutofDBQ = -1; // and here and simply adding one to queue at time mwWaitsinDBQ and removeing one at time mwOutofDBQ
		 mwStartsSendingToDB = -1;
		 mwRespFromDB = -1;
		 mwSentRespToCli = -1;
		 clRespFromMW = -1;
		 clClosedConn = -1;
		 mwFinished = -1;
		 dbRoundTime = -1; //mwRespFromDB - mwWaitsinDBQ  
		 dbThinkTime = -1;
		 mwThinkTime = -1; //Calculated on the client 
		 mwTimeInDBQ = -1;
		 clTimeInQ = -1;
		 qSizeDB= -1;
		 qSizeMW= -1;
		 mwNetworkTime = -1;
		 mwNoQRound=-1;
		 mwID=-1;
		 mwThinkTime2=-1;
		 mwReadyNetTime=-1;
	}
	public StatTrack merge(StatTrack other){
		StatTrack cl = new StatTrack();;
		StatTrack mw = new StatTrack();;
		//Other is MW
		if(other.clStarts==-1&&other.mwGetsStreams>0){
			cl= this;
			mw = other; 
		}
		else{
			cl = other;
			mw = this;
		}
		StatTrack out = new StatTrack();
		 out.clStarts = cl.clStarts;
		 out.clWaitsinMWQ = cl.clWaitsinMWQ;
		 out.mwStarts = mw.mwStarts;
		 out.clOutofMWQ = cl.clOutofMWQ;
		 out.clGotOutStream = cl.clGotOutStream;
		 out.mwGetsStreams = mw.mwGetsStreams;
		 out.mwSentReadyToClient = mw.mwSentReadyToClient;
		 out.mwReceived = mw.mwReceived;
		 out.clSentReqToMW = cl.clSentReqToMW;
		 out.mwWaitsinDBQ = mw.mwWaitsinDBQ; // After finishing the experiment you can estimate how many threads in the queue by doing one serial run over the timestamps here 
		 out.mwOutofDBQ = mw.mwOutofDBQ; // and here and simply adding one to queue at time mwWaitsinDBQ and removeing one at time mwOutofDBQ
		 out.mwStartsSendingToDB = mw.mwStartsSendingToDB;
		 out.mwRespFromDB = mw.mwRespFromDB;
		 out.mwSentRespToCli = mw.mwSentRespToCli;
		 out.clRespFromMW = cl.clRespFromMW;
		 out.clClosedConn = cl.clClosedConn;
		 out.mwFinished = mw.mwFinished;
		 out.dbRoundTime = mw.dbRoundTime; //mwRespFromDB - mwWaitsinDBQ  
		 out.dbThinkTime = mw.dbThinkTime;
		 out.mwThinkTime = cl.mwThinkTime; //Calculated on the client 
		 out.mwNoQRound = cl.mwNoQRound;
		 out.mwTimeInDBQ = mw.mwTimeInDBQ; 
		 out.qSizeDB=mw.qSizeDB;
		 out.qSizeMW=mw.qSizeMW;
		 out.mwID = mw.mwID;
		 out.mwThinkTime2=mw.mwThinkTime2;
		 out.clTimeInQ = cl.clTimeInQ;
		 out.mwReadyNetTime=mw.mwReadyNetTime;
		 return out;
	}
	public void comput(){
		dbRoundTime = (int) (mwRespFromDB - mwWaitsinDBQ); //Response time includes waiting in the queue
		dbThinkTime =  (int) (mwRespFromDB - mwStartsSendingToDB);
		clTimeInQ = (int) ( clOutofMWQ - clWaitsinMWQ);
		mwReadyNetTime = (int)(mwSentReadyToClient-mwGetsStreams);
		
		mwRoundTime = (int) ( clRespFromMW  - clWaitsinMWQ);  //Response time includes waiting in the queue
		mwNoQRound = (int) ( clRespFromMW  - clOutofMWQ); //MW response time without client queue
		mwThinkTime = (int)(mwRespFromDB - mwReceived) -dbRoundTime ; //Thinktime is everything other than waiting in queues or waiting for responses of other nodes \
		mwNetworkTime = mwNoQRound - dbRoundTime- mwThinkTime;
		mwTimeInDBQ     = (int) (mwOutofDBQ - mwWaitsinDBQ);
		
		clRoundTime = (int) (clClosedConn - clStarts ); //Response time includes waiting in the queue
		clThinkTime = (int) clRoundTime - mwRoundTime;
	}
	/*
  	public void comput(){
	dbRoundTime = (int) (mwRespFromDB - mwWaitsinDBQ); //Response time includes waiting in the queue
	dbThinkTime =  (int) (mwRespFromDB - mwStartsSendingToDB);
	
	
	mwRoundTime = (int) ( clRespFromMW  - clWaitsinMWQ);  //Response time includes waiting in the queue
	mwNoQRound = (int) ( clRespFromMW  - clOutofMWQ);
	mwThinkTime = mwRoundTime - dbRoundTime; //Thinktime is everything other than waiting in queues or waiting for responses of other nodes \
	int mwInternalRoundTime= (int) (mwStarts - mwRespFromDB);
	mwNetworkTime = mwNoQRound -mwInternalRoundTime
	mwTimeInDBQ     = (int) (mwOutofDBQ - mwWaitsinDBQ);
	clTimeInQ = (int) ( clOutofMWQ - clWaitsinMWQ);
	clRoundTime = (int) (clClosedConn - clStarts ); //Response time includes waiting in the queue
	clThinkTime = (int) clRoundTime - mwRoundTime;
	}
	
	thinkWithNet <- mwNoQRound - dbRoundTime
	
	mwNetworkTime = mwNoQRound -(mwStarts - mwRespFromDB)
	mwNetworkTime - mwNoQRound  =  + mwRespFromDB -mwStarts 
	
	 */
	
}
