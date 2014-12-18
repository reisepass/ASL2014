package dbBenchmark;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.ethz.asl.user04.clientAPI.MessageAPI2014;
import edu.ethz.asl.user04.dbutils.DBManager;
import edu.ethz.asl.user04.dbutils.SQLUtil_v2014;
import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2014;
import edu.ethz.asl.user04.shared.entity.Message;
import edu.ethz.asl.user04.shared.entity.StatTrack;
import edu.ethz.asl.user04.shared.logging.MessagingSystemLogger;

public class dbReader implements Runnable {
	public final static Logger LOGGER = MessagingSystemLogger.getLoggerForClass("DBaloneLogg");
	DBManager dbPool;
	int num_queues;
	long buffer;
	int clientId;
	ConfigExperimentV2014 expConf;
	int resultChunkSize = 50;
	public dbReader(int cliID, DBManager db,long buffer,ConfigExperimentV2014 cfg) {
		this.clientId=cliID;
		this.expConf=cfg;
		this.num_queues = expConf.numQueues;
		this.dbPool=db;
		this.buffer=buffer;
	}
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		Long myStart = System.currentTimeMillis();
		ConfigExperimentV2014 cfg=expConf;
		boolean wasCreated=false;
		while(myStart+expConf.experimentLength>System.currentTimeMillis()){
		
			StatTrack outTimes = new StatTrack();
			
			outTimes.clStarts = System.currentTimeMillis();
			outTimes.clWaitsinMWQ = System.currentTimeMillis();
			outTimes.clOutofMWQ = System.currentTimeMillis();
			outTimes.clGotOutStream = System.currentTimeMillis();
			
			outTimes.mwStarts=System.currentTimeMillis();
			outTimes.mwGetsStreams=System.currentTimeMillis();
			outTimes.mwSentReadyToClient=System.currentTimeMillis();	
			outTimes.mwReceived=System.currentTimeMillis();
			int queueID = (int) (Math.random() * 1000 % num_queues) + 1;
			
			
			
			outTimes.mwWaitsinDBQ=System.currentTimeMillis();
			Connection conn = dbPool.newClientConnection();
			outTimes.mwOutofDBQ=System.currentTimeMillis();
			
			SQLUtil_v2014 sqlutil = new SQLUtil_v2014(conn,resultChunkSize);
			UUID requestUUID = UUID.randomUUID();
			if(!wasCreated){
				try {
					sqlutil.CreateClient(clientId);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println(" Warning Could not create client"+clientId);
				}
			}
			outTimes.clSentReqToMW= System.currentTimeMillis();
			outTimes.mwStartsSendingToDB=System.currentTimeMillis();
			Message resultSingle = sqlutil.getPrivateMessage(-1, clientId,queueID, true, true, 0);
			
			
			outTimes.mwRespFromDB=System.currentTimeMillis();
			outTimes.clRespFromMW = System.currentTimeMillis();
			boolean resp = resultSingle != null;
			String custom="nothing";
			if(resultSingle != null){
			custom=resultSingle.getPayload().substring(0, 4);
			}
			sqlutil.closeSQLconnection();
			try {
				if (!conn.isClosed())
					conn.close();

			} catch (SQLException e) {
				
				LOGGER.log(Level.WARNING, "[BUG] Something went wrong in MessageHandler", e);					
			}
			
			outTimes.clClosedConn = System.currentTimeMillis();
			
			LOGGER.log(
					Level.INFO,
					String.format(
							"[CSVFRMT]  %s, %d, %d, %d, %d, %d, %d, %d, %d, %s ,%d ,%s, %d, %d, %d, %d, %d, %d, %s, %d, %d, %s, %d ",
							cfg.description,
							cfg.experimentStartTime,
							cfg.experimentLength,
							cfg.numberOfMessageHandlerThreads,
							cfg.numberOfMiddleWareMachines,
							cfg.numberOfDBConnections,
							cfg.numberOfClientMachines,
							cfg.numQueues,
							cfg.num_clients,
							requestUUID,
							this.clientId,
							"popDBalone",
							outTimes.clThinkTime,
							outTimes.clRoundTime,
							outTimes.mwThinkTime,
							outTimes.mwRoundTime,
							outTimes.dbRoundTime,
							outTimes.dbThinkTime,
							resp ? "[s]" : "[f]",				
							outTimes.mwTimeInDBQ,
							outTimes.clTimeInQ,
							custom,
							1
							
							)
							+String.format(", %d, %d, %d, %d, %d, %d, %d, %d, %d",
									outTimes.clWaitsinMWQ-outTimes.clStarts, //Connection init time 
									outTimes.clClosedConn-outTimes.clRespFromMW, // Close connection time
									outTimes.mwGetsStreams-outTimes.mwStarts, //Open stream time on mw
									outTimes.mwSentReadyToClient-outTimes.mwGetsStreams, //Time to send ready Message
									outTimes.mwWaitsinDBQ-outTimes.mwSentReadyToClient, //Time to receive Request from client 
									outTimes.mwWaitsinDBQ-cfg.experimentStartTime,  //used for q size calc
									outTimes.mwOutofDBQ-cfg.experimentStartTime, //used for q size calc
									outTimes.clWaitsinMWQ-cfg.experimentStartTime,   //used for q size calc
									outTimes.clOutofMWQ-cfg.experimentStartTime  //used for q size calc
							)
							+String.format(", %d, %d, %d, %d, %d, %d",
									outTimes.qSizeDB,
									outTimes.qSizeMW,
									outTimes.mwNoQRound,
									outTimes.mwNetworkTime,
									outTimes.mwID,
									outTimes.mwReadyNetTime
							)
					);
			
			Long waittime = buffer-outTimes.clRoundTime;
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
	}

}
