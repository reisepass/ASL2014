package edu.ethz.asl.user04.messagebroker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.ethz.asl.user04.dbutils.DBManager;
import edu.ethz.asl.user04.dbutils.SQLUtil_v2014;
import edu.ethz.asl.user04.shared.entity.Message;
import edu.ethz.asl.user04.shared.entity.RequestResponse;
import edu.ethz.asl.user04.shared.entity.StatTrack;
import edu.ethz.asl.user04.shared.logging.MessagingSystemLogger;
import edu.ethz.user04.shared.requests.messagerequests.DeleteMessageRequest;
import edu.ethz.user04.shared.requests.messagerequests.MessagingSystemRequest;
import edu.ethz.user04.shared.requests.messagerequests.WriteMessageRequest;
import edu.ethz.user04.shared.requests.queuerequests.CloseConnection;
import edu.ethz.user04.shared.requests.queuerequests.CreateClientRequest;
import edu.ethz.user04.shared.requests.queuerequests.CreateQueueRequest;
import edu.ethz.user04.shared.requests.queuerequests.DeleteQueueRequest;
import edu.ethz.user04.shared.requests.queuerequests.QueryForQueuesWithMessagesForMe;
import edu.ethz.user04.shared.requests.queuerequests.ReadAllMessagesRequest;
import edu.ethz.user04.shared.requests.queuerequests.ReadPrivateMessageRequest;
import edu.ethz.user04.shared.requests.queuerequests.ReadQueueRequest;
import edu.ethz.user04.shared.requests.queuerequests.ReadSpecificSender;

public class MessageHandler implements Runnable {

	public final static Logger LOGGER = MessagingSystemLogger
			.getLoggerForClass(MessageHandler.class.getName());
	
	// OT - If this is enabled, the message handler keeps listening for objects.
	// Message Handler terminates only when it receives a CloseConnectionObject
	// Maybe read this from config? 
	private final static boolean BATCH_REQUESTS = false;

	private final Socket socket;
	DBManager dbC;
	Object messageObject;
	ObjectInputStream oi;
	boolean debugOn = false;
	int sleepBeforeConnectionRetry = 1000;
	int resultChunkSize = 50;
	ObjectOutputStream oo;
	private UUID lastReqIDServer;
	private Exception lastExceptionHandled;

	private AtomicInteger dbQueueCount;
	private AtomicInteger mwQueueCount;
	private int startingMWQ;
	private int startingDBQ;
	/**
	 * messageObject needs to be one object from the queue
	 * 
	 * @param socket
	 * @param dbPool
	 */
	public MessageHandler(Socket socket, DBManager dbPool,AtomicInteger dbQueueCount, AtomicInteger mwQueueCount,int startingMWQ) {
		this.socket = socket;
		this.dbC = dbPool;
		debugOn = false;
		this.dbQueueCount=dbQueueCount;
		this.mwQueueCount=mwQueueCount;
		this.startingMWQ=startingMWQ;
	}

	public MessageHandler(Socket socket, DBManager dbPool,AtomicInteger dbQueueCount, AtomicInteger mwQueueCount, int startingMWQ , boolean debugOn) {
		this.socket = socket;
		this.dbC = dbPool;
		this.debugOn = debugOn;
		this.dbQueueCount=dbQueueCount;
		this.mwQueueCount=mwQueueCount;
		this.startingMWQ=startingMWQ;

	}

	@Override
	public void run() {
		
		StatTrack timeStamps = new StatTrack();
		timeStamps.mwStarts=System.currentTimeMillis();
		timeStamps.mwID=MessagingSystemLogger.middlewareID;
		if(mwQueueCount!=null)
			mwQueueCount.getAndDecrement();
		timeStamps.qSizeMW=startingMWQ;
		try {
			if(debugOn)
				System.out.println("## MWthread begins");
			
			oo = new ObjectOutputStream(socket.getOutputStream());
			oi = new ObjectInputStream(socket.getInputStream());
			timeStamps.mwGetsStreams=System.currentTimeMillis();	
			if(debugOn)
				System.out.println("## MWthread streamsInit");

			do {			
				oo.writeObject(new Boolean(true));			
				timeStamps.mwSentReadyToClient=System.currentTimeMillis();	
				if(debugOn)
					System.out.println("## MWthread acc sent to Client");
				
				messageObject = oi.readObject();
				timeStamps.mwReceived=System.currentTimeMillis();
				MessagingSystemRequest msr = (MessagingSystemRequest) messageObject;
				
				lastReqIDServer=msr.getRequestUUID();
				
				int timesTried = 1;
				timeStamps.mwWaitsinDBQ=System.currentTimeMillis();
				if(dbQueueCount!=null){
					startingDBQ = dbQueueCount.getAndIncrement();
				}
				else{
					startingDBQ=-1;
				}
				
				timeStamps.qSizeDB=startingDBQ;
				if(debugOn)
				LOGGER.log(
						Level.INFO,
						"[DEBUG] aboutToGetDBCon "
								+ msr.getRequestUUID());
				Connection conn = dbC.newClientConnection();
				if(debugOn)
				LOGGER.log(
						Level.INFO,
						"[DEBUG] GotDBCon "
								+ msr.getRequestUUID());
				
				if (conn == null) {
					LOGGER.severe("Failed to get DB connection");
					while (timesTried < 15 && conn == null) {
						try {
							timesTried++;
							Thread.sleep(sleepBeforeConnectionRetry);
							conn = dbC.newClientConnection();
						} catch (InterruptedException e) {
							System.err.println("CantSleep " + e.getMessage());
							LOGGER.log(Level.WARNING, "[BUG] Something went wrong in MessageHandler", e);
						}
					}
					if (conn == null)
						throw new EmptyStackException();
				}
				timeStamps.mwOutofDBQ=System.currentTimeMillis();
				if(dbQueueCount!=null)
					dbQueueCount.decrementAndGet();
				
				SQLUtil_v2014 sqlutil = new SQLUtil_v2014(conn, resultChunkSize);

				// TODO return the id of the message you just wrote back to
				// client
				//
				// Write Message Request
				//
				if (messageObject instanceof WriteMessageRequest) {
					LOGGER.log(
							Level.INFO,
							"[DEBUG] WriteMessageRequest "
									+ msr.getRequestUUID());

					WriteMessageRequest messageRequest = (WriteMessageRequest) messageObject;
					Message message = messageRequest.message;
					ArrayList<Integer> result;
					boolean success = true;
					
					
					
					if (message.getQueueIdList().size() > 1) {
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						result = sqlutil.multiMessage(message.getPayload(),
								message.getSenderId(), message.getReceiverId(),
								message.getContext(), message.getPriority(),
								message.getQueueIdList());
						timeStamps.mwRespFromDB=System.currentTimeMillis();
						
						for (Integer res : result) {
							if (res == -1) {
								success = false;
								break;
							}
						}

					} else {
						// TODO makesure outside this that queuelsit always has
						// atleast one elmentgetQueuesWithRelevantMessages
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						int resultSingle = sqlutil
								.addMessage(message.getPayload(), message
										.getSenderId(),
										message.getReceiverId(), message
												.getContext(), message
												.getPriority(), message
												.getQueueIdList().get(0));
						timeStamps.mwRespFromDB=System.currentTimeMillis();

						success = resultSingle != -1;
					}
					

					oo.writeObject(new RequestResponse(success, msr.getRequestUUID(),timeStamps ));
					timeStamps.mwSentRespToCli=System.currentTimeMillis();
				
				} 
				//
				// Delete Message Request
				//
				else if (messageObject instanceof DeleteMessageRequest) {
					DeleteMessageRequest deleteMessageRequest = (DeleteMessageRequest) messageObject;
					
							
					boolean responseVal = false;
					try {
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						responseVal = sqlutil.deleteMessage(deleteMessageRequest.messageid);
						timeStamps.mwRespFromDB = System.currentTimeMillis();
					} catch (SQLException e) {
						LOGGER.log(Level.SEVERE, "Encountered SQLException for request " + deleteMessageRequest.getRequestUUID(), e);
					}
					
					oo.writeObject(new RequestResponse(responseVal,timeStamps));
					timeStamps.mwSentRespToCli= System.currentTimeMillis();
				}
				//
				// Create Queue Request
				//
				else if (messageObject instanceof CreateQueueRequest) {
					CreateQueueRequest createQueueRequest = (CreateQueueRequest) messageObject;
					boolean recVal = false;

		
					try {
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						recVal = sqlutil.CreateQueue(createQueueRequest.queueid,
								createQueueRequest.clientid, createQueueRequest.queueName);
						timeStamps.mwRespFromDB = System.currentTimeMillis();
					} catch (SQLException e) {
						LOGGER.log(Level.SEVERE, "Encountered SQLException for request " + createQueueRequest.getRequestUUID(), e);
					}
					


					oo.writeObject(new RequestResponse(recVal,timeStamps));
					timeStamps.mwSentRespToCli= System.currentTimeMillis();
				}
				//
				// Delete Queue Request
				//
				else if (messageObject instanceof DeleteQueueRequest) {
					DeleteQueueRequest deleteQueueRequest = (DeleteQueueRequest) messageObject;
					boolean resp;
					try {
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						resp = sqlutil.deleteQueue(deleteQueueRequest.queueid);
						//resp = sqlutil.deleteQueueCascade(deleteQueueRequest.queueid);
						timeStamps.mwRespFromDB=System.currentTimeMillis();
						LOGGER.log(Level.INFO, "request_type=deleteQueue tried on "+deleteQueueRequest.queueid+" and got <"+resp+"> " + deleteQueueRequest.getRequestUUID());
					} catch (SQLException e) {// TODO inform the client
						LOGGER.log(Level.SEVERE, "Encountered SQLException for request " + deleteQueueRequest.getRequestUUID(), e);
						resp = false;
					}

					oo.writeObject(new RequestResponse(resp, timeStamps));
					timeStamps.mwSentRespToCli= System.currentTimeMillis();
				}
				//
				// QueryForQueuesWithMessagesForMe
				//
				else if (messageObject instanceof QueryForQueuesWithMessagesForMe) {
					
					QueryForQueuesWithMessagesForMe queryRequest = (QueryForQueuesWithMessagesForMe) messageObject;
					int receiverId = queryRequest.receiverId;
					ArrayList<Integer> resp;
					

					
					try {
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						resp = sqlutil
								.getQueuesWithRelevantMessages(receiverId);
						timeStamps.mwRespFromDB= System.currentTimeMillis();
					} catch (SQLException e) {
						LOGGER.log(Level.SEVERE, "Encountered SQLException for request " + queryRequest.getRequestUUID(), e);
						resp = null;
					}

					// it was initially resp.size()!=0; but a resp of size 0 means that user simply has no messages 
					oo.writeObject(new RequestResponse(resp!=null, (Object) resp, timeStamps) );
					timeStamps.mwSentRespToCli= System.currentTimeMillis();
					
				}
				//
				// Read Queue Request
				//
				else if (messageObject instanceof ReadQueueRequest) {
					ReadQueueRequest mesgq = (ReadQueueRequest) messageObject;
					Message sendBack = null;
					

					
					if (mesgq.orderByTime) {
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						sendBack = sqlutil.getQueueClosestTime(mesgq.queueId,
								mesgq.removeAfter);
						timeStamps.mwRespFromDB = System.currentTimeMillis();
					} else {
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						sendBack = sqlutil.getQueuePriorityFirst(mesgq.queueId,
								mesgq.removeAfter);
						timeStamps.mwRespFromDB = System.currentTimeMillis();
					}
					
					
					oo.writeObject( new RequestResponse(sendBack!=null && sendBack instanceof Message, (Object) sendBack, timeStamps));
					timeStamps.mwSentRespToCli= System.currentTimeMillis();
				}
				//
				// Close Connection
				//
				else if (messageObject instanceof CloseConnection) {
					if (debugOn)
						System.out.print("<CloseConnection>");
					
					LOGGER.severe("CloseConnection should not exist");
					oo.close();
					sqlutil.closeSQLconnection();

				}
				//
				// Read Private Message Request
				//
				else if (messageObject instanceof ReadPrivateMessageRequest) {
					if (debugOn)
						System.out.print("<ReadPrivateMessageRequest>");
					ReadPrivateMessageRequest mesgq = (ReadPrivateMessageRequest) messageObject;
					Message sendBack;


					
					if(mesgq.getBraodcast){
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						sendBack = sqlutil.getPrivateMessage(-1, mesgq.receiverID,
								mesgq.queueID, mesgq.removeAfterPeek, mesgq.orderByTime, mesgq.getContext());
						timeStamps.mwRespFromDB = System.currentTimeMillis();
					}
					else {
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						sendBack = sqlutil.getPrivateMessageNotBroadcast(-1, mesgq.receiverID, mesgq.queueID, mesgq.removeAfterPeek, mesgq.orderByTime, mesgq.getContext());
						timeStamps.mwRespFromDB = System.currentTimeMillis();
					}
					
					
					

					oo.writeObject( new RequestResponse(sendBack!=null && sendBack instanceof Message, (Object) sendBack, timeStamps));
					timeStamps.mwSentRespToCli= System.currentTimeMillis();
				}
				//
				// Read message which was sent by a authored user
				//
				else if (messageObject instanceof ReadSpecificSender) {
					if (debugOn)
						System.out.print("<ReadPrivateMessageRequest>");
					ReadSpecificSender mesgq = (ReadSpecificSender) messageObject;
					Message sendBack;

					timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
					sendBack = sqlutil.getPrivateMessageFromSender(	mesgq.queueID, mesgq.removeAfterPeek, mesgq.orderByTime, mesgq.senderID, mesgq.getContext(),mesgq.auther);
					timeStamps.mwRespFromDB = System.currentTimeMillis();

					oo.writeObject( new RequestResponse(sendBack!=null && sendBack instanceof Message, (Object) sendBack, timeStamps));
					timeStamps.mwSentRespToCli= System.currentTimeMillis();
				}
				//
				// Read All Messages Request
				//
				else if (messageObject instanceof ReadAllMessagesRequest) {
					ReadAllMessagesRequest param = (ReadAllMessagesRequest) messageObject;
					List<Message> result = null;

					try {
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						result = sqlutil.getAllPrivateMessages(param.queueID,
								param.removeAfterPeek, param.orderByTime,
								param.receiverID, param.page);
						timeStamps.mwRespFromDB = System.currentTimeMillis();
					} catch (SQLException e) {
						LOGGER.log(Level.SEVERE, "Encountered SQLException for request " + param.getRequestUUID(), e);
					}
					
					oo.writeObject( new RequestResponse(result!=null , (Object) result, timeStamps));
					timeStamps.mwSentRespToCli= System.currentTimeMillis();
				}
				else if(messageObject instanceof CreateClientRequest){
					CreateClientRequest clR = (CreateClientRequest)messageObject;
					
					int result=-6;
					try {
						timeStamps.mwStartsSendingToDB=System.currentTimeMillis();
						result = sqlutil.CreateClient(clR.getID());
						timeStamps.mwRespFromDB = System.currentTimeMillis();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					

					oo.writeObject( new RequestResponse(result!=-6 , (Object) new Integer(result), timeStamps));
					timeStamps.mwSentRespToCli= System.currentTimeMillis();
					
				
				}
				else {
					System.out.println("Unable to detect MEssage Type Sent");
					sqlutil.closeSQLconnection();
					oo.close();
				}

				sqlutil.closeSQLconnection();

				try {
					if (!conn.isClosed())
						conn.close();

				} catch (SQLException e) {
					lastExceptionHandled=e;
					LOGGER.log(Level.WARNING, "[BUG] Something went wrong in MessageHandler", e);					
				}
				
				
				
			} while(BATCH_REQUESTS);
		} catch (IOException e) { // Send message back to client to resent
									// Package // if thats the problem
			lastExceptionHandled=e;
			LOGGER.severe(String
					.format("[SUPERBUG] type=error request_type=IOException tier=middleware tag=failedToGetReadObjectCorreclty exceptionPrint='%s'",e.getMessage()
								));
	
			System.err.println("ERR186" + e.getMessage());
			LOGGER.log(Level.WARNING, "[BUG] Something went wrong in MessageHandler", e);

			try {
				
				if(!socket.isClosed()){
				socket.close();
				}

			} catch (IOException e1) {
				lastExceptionHandled=e1;
				e1.printStackTrace();
				LOGGER.log(Level.WARNING, String
						.format("[WARNING] type=warning request_type=ConnectionCloseProblem tier=middleware tag=ConnectionCloseProblem  exceptionPrint='%s'",e1.getMessage()), e);
				LOGGER.log(Level.WARNING, "[BUG] Something went wrong in MessageHandler", e1);
				
			}

		} catch (ClassNotFoundException e) {
			lastExceptionHandled=e;
			System.err.println("ERR188" + e.getMessage());
			LOGGER.severe(String
					.format("[SUPERBUG] type=err.sleepor request_type=ClassNotFoundException tier=middleware tag=ClassNotFoundException, exceptionPrint='%s'"
								,e.getMessage()));
			LOGGER.log(Level.WARNING, "[BUG] Something went wrong in MessageHandler", e);
		}

		if(!socket.isClosed()){
			
			String reqID;
			String lastE;
			if(lastExceptionHandled!=null){
				lastE=lastExceptionHandled.getMessage();
			}
			else{
				lastE = "no_exption_saved";
			}
			if(lastReqIDServer!=null){
				reqID=lastReqIDServer.toString();
			}
			else{
				reqID="no_id_saved";
			}
			
			try {
				oo.close();
				if(!socket.isClosed()){
					LOGGER.log(
							Level.WARNING,
							String.format(
									"[SUPERBUG] type=warning request_type=ConnectionCloseProblem2 tier=middleware tag=ConnectionCloseProblem2 request_id=%s exceptionPrint=This is Try 2 '%s'",
									reqID, "endingThreadwithoutCoslingSocket"), lastE);
				}
			} catch (IOException e) {
				e.printStackTrace();
				LOGGER.log(
						Level.WARNING,
						String.format(
								"[SUPERBUG] type=warning request_type=ConnectionCloseProblem tier=middleware tag=ConnectionCloseProblem request_id=%s exceptionPrint=This is Try 2 '%s'",
								reqID, e.getMessage()), lastE);
				
				
			}


			
		}
		
		
	}

}
