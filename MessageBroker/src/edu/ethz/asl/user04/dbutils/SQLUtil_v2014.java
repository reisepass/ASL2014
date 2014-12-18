
package edu.ethz.asl.user04.dbutils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DebugGraphics;

import edu.ethz.asl.user04.messagebroker.MessageHandler;
import edu.ethz.asl.user04.shared.entity.Message;
import edu.ethz.asl.user04.shared.logging.MessagingSystemLogger;
import edu.ethz.user04.shared.requests.messagerequests.MessagingSystemRequest;


/**
 * Assumed DB schema : 
 * 
 * messages :  int messageid,senderid,receiverid,queueid,context,priority; time: timeofarrival; String message
 * queues   :  int queueid  ,createdby; time: creationtime
 * clients  :  int clienttype,clientid; time: creationtime
 * @author mort
 *
 */
public class SQLUtil_v2014 {
	
	public final static Logger LOGGER = MessagingSystemLogger
			.getLoggerForClass(SQLUtil_v2014.class.getName());
	
	protected Connection conn;
	ArrayList<String> messagesTableCols;
	ArrayList<String> colQueues;
	int resultChunkSize;
	private MessagingSystemRequest lastMSG;
	private int lastClientID;
	
	public SQLUtil_v2014(Connection inpC,int resultChunkSize){
		conn=inpC;
		messagesTableCols = new ArrayList<String>();
		messagesTableCols.add("messageid");
		messagesTableCols.add("senderid");
		messagesTableCols.add("receiverid");

		messagesTableCols.add("queueid");
		messagesTableCols.add("context");
		messagesTableCols.add("priority");
		messagesTableCols.add("message");
		//messagesTableCols.add("timeofarrial");
		
		colQueues= new ArrayList<String>();
		colQueues.add("queueid");
		colQueues.add("createdby");
		colQueues.add("creationtime");
		
		this.resultChunkSize=50;//TODO this is choosen arbitrarily
		lastMSG = null;

	}
	
	public SQLUtil_v2014(Connection inpC, int resultChunkSize, MessagingSystemRequest msg){
		conn=inpC;
		messagesTableCols = new ArrayList<String>();
		messagesTableCols.add("messageid");
		messagesTableCols.add("senderid");
		messagesTableCols.add("receiverid");

		messagesTableCols.add("queueid");
		messagesTableCols.add("context");
		messagesTableCols.add("priority");
		messagesTableCols.add("message");
		//messagesTableCols.add("timeofarrial");
		
		colQueues= new ArrayList<String>();
		colQueues.add("queueid");
		colQueues.add("createdby");
		colQueues.add("creationtime");
		
		this.resultChunkSize=50;//TODO this is choosen arbitrarily
		lastMSG=msg;
		
	}
	
	private Connection getDBConnection(){
		return conn;
	}
	

	public int sendSQL(String inpCommand) {
		int errorCode = 1;
		Statement statmt=null;
		
		boolean er;
		try{
			statmt=conn.createStatement();
			if(statmt==null){ //TODO make this a more expressive Error object
				if(lastMSG!=null){
					LOGGER.log(
							Level.INFO,	String.format(
									"[DEBUG] type=error request_type=createStatementNULL request_id=%s tier=middleware tag=failedRequest_in_sendsql ",
									lastMSG.getRequestUUID()));
					}
					else{
						LOGGER.log(Level.INFO,String.format(
								"[DEBUG] type=error request_type=createStatementNULL request_id=%s tier=middleware tag=failedRequest_in_sendsql ",
								"no_id_saved"));
					}
				return -1;
			}
			statmt.execute(inpCommand);
			//System.out.println("erVal: "+er);
			er = true;
		}
		catch(SQLException e){
			System.out.println( e);

			if(lastMSG!=null){
			LOGGER.log(
					Level.INFO,	String.format(
							"[DEBUG] type=error request_type=SQLException request_id=%s tier=middleware tag=failedrequest_in_sendsql exceptionPrint='%s'",
							lastMSG.getRequestUUID(), e));
			}
			else{
				LOGGER.log(Level.INFO,String.format(
						"[DEBUG] type=error request_type=SQLException request_id=%s tier=middleware tag=failedrequest_in_sendsql exceptionPrint='%s' ",
						"no_id_saved", e));
			}
			er = false;
		}
		finally{
			if(statmt!=null)
				try {
					statmt.close();
				} catch (SQLException e) {
					if(lastMSG!=null){
						LOGGER.log(
								Level.INFO,	String.format(
										"[DEBUG] type=error request_type=SQLException request_id=%s tier=middleware tag=failedrequest_in_sendsql exceptionPrint='%s' ",
										lastMSG.getRequestUUID(), e));
						}
						else{
							LOGGER.log(Level.INFO,String.format(
									"[DEBUG] type=error request_type=SQLException request_id=%s tier=middleware tag=failedrequest_in_sendsql exceptionPrint='%s' ",
									"no_id_saved", e));
						}
					
					
				}
			
		}
		return (er)?1:-1;
	}
	
	public boolean closeSQLconnection(){
		try {
			conn.close();
			return true;
		} catch (SQLException e) {
			System.err.println( e);
			if(lastMSG!=null){
				LOGGER.log(
						Level.INFO,	String.format(
								"[DEBUG] type=error request_type=SQLException request_id=%s tier=middleware tag=failedrequest_in_closesqlconnection exceptionPrint='%s' ",
								lastMSG.getRequestUUID(), e));
				}
				else{
					LOGGER.log(Level.INFO,String.format(
							"[DEBUG] type=error request_type=SQLException request_id=%s tier=middleware tag=failedrequest_in_closesqlconnection exceptionPrint='%s' ",
							"no_id_saved", e));
				}
			
			e.printStackTrace();
			return false;
		}
	}
	
	
	public ArrayList<ArrayList<String>> respondSQL(String inpCommand, ArrayList<String> requestedColumNames) {
		ArrayList<ArrayList<String>> responses = new ArrayList<ArrayList<String>>();
		Statement statmt=null;

		boolean er =false;
			try{
					statmt=getDBConnection().createStatement();
					if(statmt==null){ //TODO make this a more expressive Error object
						if(lastMSG!=null){
							LOGGER.log(
									Level.INFO,	String.format(
											"[DEBUG] type=error request_type=SQLException request_id=%s tier=middleware exceptionPrint='%s' tag=failedrequest_in_respondsql ",
											lastMSG.getRequestUUID(),"createstatmentreturnednull"));
							}
							else{
								LOGGER.log(Level.INFO,String.format(
										"[DEBUG] type=error request_type=SQLException request_id=%s tier=middleware exceptionPrint='%s' tag=failedrequest_in_respondsql ",
										"no_id_saved","createstatmentreturnednull" ));
							}
						return responses;
					}
			ResultSet rs=statmt.executeQuery(inpCommand);
			
			while (rs.next()) {
				
				ArrayList<String> curStrings = new ArrayList<String>();
				for(String colName : requestedColumNames){
					curStrings.add(rs.getString(colName));
				
				}
				responses.add(curStrings);
			}
 
			
		}
		catch(SQLException e){
			if(lastMSG!=null){
				LOGGER.log(
						Level.INFO,	String.format(
								"[DEBUG] type=error request_type=SQLException request_id=%s tier=middleware tag=failedrequest_in_respondsql exceptionPrint='%s' ",
								lastMSG.getRequestUUID(), e));
				}
				else{
					LOGGER.log(Level.INFO,String.format(
							"[DEBUG] type=error request_type=SQLException request_id=%s tier=middleware tag=failedrequest_in_respondsql exceptionPrint='%s' ",
							"no_id_saved", e));
				}
			System.out.println( e);
		}
		finally{
			if(statmt!=null)
				try {
					statmt.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
		}
		return responses;
	}
	
	

	
	 
	public int CreateClient(int clientID) throws SQLException{ //Returns client ID
		
		String makeClient = "INSERT INTO clients (clientid) VALUES ("+clientID+");";
		if(!isClientIDtaken(clientID))
			return sendSQL(makeClient);
		else
			return -1;
	}
	
	public boolean isClientIDtaken(int clientID) throws SQLException{
		String sql="SELECT * FROM clients where clientid="+clientID+";";
		ArrayList<String> cols = new ArrayList<String>();
		cols.add("clientid");
 		 ArrayList<ArrayList<String>> ret =  respondSQL(sql,cols);
 		 return (ret.size()>0) ? true : false; 
		
	}
	public boolean CreateQueue(int pruposedID, int clientID, String queueName) throws SQLException{
		if(!queueIdTaken(pruposedID)){
			String sql= "INSERT INTO queues (queueid, queuename, createdby) VALUES("+pruposedID+",'"+ queueName+"',"+clientID+")"+";";
			boolean res;
			int resp = sendSQL(sql);
			if(resp ==1){
				res = true;
			}
			else res = false;
			System.out.println("query sent, db resp: "+resp);
			return res;
		}
		else
			return false; 
	}
	public boolean queueIdTaken(int qID) throws SQLException  {
		String sql = "SELECT queueid FROM queues WHERE queueid="+qID+";";
		ArrayList<String> cols = new ArrayList<String>();
		cols.add("queueid");
 		 ArrayList<ArrayList<String>> ret =  respondSQL(sql,cols);
 		 return (ret.size()>0) ? true : false; 
 		 
	}
	public boolean deleteMessage( long messageId)throws SQLException{
		String sql = "DELETE FROM messages WHERE messageid="+messageId+";";
		int err = sendSQL(sql);
	    if(err==-1)
	    	return false;
	    else
	    	return true;
	}
	public boolean deleteQueue(int qID) throws SQLException{
		if(queueIdTaken(qID)){
			String sql = "DELETE FROM messages WHERE queueid="+qID+";";
		    int err = sendSQL(sql);
		    System.out.println("err1: "+err);
		    if(err==-1){
				LOGGER.log(
						Level.INFO,	String.format(
								"[DEBUG] type=warning request_type=deleteQueue request_id=%s tier=middleware tag=failedrequest_in_respondsql myNote='%s' ",
								lastMSG.getRequestUUID(),"DELETE FROM messages WHERE queueid= returned false"));
		    	return false;
		    }
		    else{
		    	String sqlQ = "DELETE FROM queues WHERE queueid="+qID+";";
			err = sendSQL(sqlQ);
			System.out.println("err2: "+err);
			if(err==-1){
				LOGGER.log(
						Level.INFO,	String.format(
								"[DEBUG] type=warning request_type=deleteQueue request_id=%s tier=middleware tag=failedrequest_in_respondsql myNote='%s' ",
								lastMSG.getRequestUUID(),"DELETE FROM queues WHERE queueid= returned false"));
			}
			return (err==-1)? false : true;
		    }
		}
		LOGGER.log(
				Level.INFO,	String.format(
						"[DEBUG] type=warning request_type=deleteQueue request_id=%s tier=middleware tag=failedrequest_in_respondsql myNote='%s' ",
						lastMSG.getRequestUUID(),"queueIdTaken(qID) returned false"));
		return false; 
	}
	public boolean deleteQueueCascade(int qID) throws SQLException{
		if(queueIdTaken(qID)){
	    	String sqlQ = "DELETE FROM queues WHERE queueid="+qID+";";
			LOGGER.log(
					Level.INFO,	String.format(
							"[DEBUG] type=warning request_type=deleteQueue tier=middleware tag=failedrequest_in_respondsql myNote='%s' ",
							sqlQ));
			int err = sendSQL(sqlQ);
			LOGGER.log(
					Level.INFO,	String.format(
							"[DEBUG] type=warning request_type=deleteQueue tier=middleware tag=failedrequest_in_respondsql myNote='%s' resp=%d ",
							sqlQ,err));
			System.out.println("err2: "+err);
			return (err==-1)? false : true;
		}
		
		return false; 
	}
	public ArrayList<ArrayList<String>> listQueues() throws SQLException{
		return listQueues(0);
	}
	public ArrayList<ArrayList<String>> listQueues(int whichChunk) throws SQLException{
		String sql = "SELECT * FROM queues LIMIT "+resultChunkSize+" OFFSET "+(resultChunkSize*whichChunk)+";";
		ArrayList<ArrayList<String>> result = respondSQL(sql,colQueues);
		
		return result; 
	}
	/**
	 * @param receiverId
	 * @return
	 * @throws SQLException
	 * gets a list of queues which either have broadcast messages or messages for this particular receiver
	 */
	
	public ArrayList<Integer> getQueuesWithRelevantMessages (int receiverId) throws SQLException{
		String sql = "SELECT DISTINCT queueid FROM messages where receiverid in (-1"+","+receiverId+")";
		ArrayList<String> colName = new ArrayList<String>();
		colName.add("queueid");
		ArrayList<ArrayList<String>> result = respondSQL(sql, colName);
		ArrayList<Integer> back = new ArrayList<Integer>();
		if(result!=null){
			for(int i =0; i<result.size(); i++){
				back.add(Integer.parseInt(result.get(i).get(0)));
			}
		}
		return back;
		
	}
	
	public ArrayList<Integer> multiMessage(String MsgText, int Sender, int Receiver, int Context, int prirority, List<Integer> queueids){
		ArrayList<Integer> out = new ArrayList<Integer>();
		for( Integer qID : queueids){
			out.add(addMessage(MsgText, Sender, Receiver, Context, prirority, qID));
		}
		return out;
	}
	
	
	/**
	 * We assume MessageTexts dont have special characters that could mess up the query
	 * @param MsgText
	 * @param Sender
	 * @param Receiver
	 * @param Context
	 * @param prirority
	 * @param queueid
	 * @return
	 */
	public int addMessage( String MsgText, int Sender, int Receiver, int Context, int prirority, int queueid){
		String makeClient = " INSERT INTO messages (senderid,receiverid,queueid,context,priority,message) " +
				"VALUES ("+Sender+","+Receiver+","+queueid+","+Context+","+prirority+",'"+MsgText+"');";
		try {
			if( queueIdTaken(queueid) ){
			
			
				//TODO return messageID 
					int errCode =-1;
				  
					  errCode= sendSQL(makeClient);
				
				return errCode; 
			}
		} catch (SQLException e) {
			
			
			e.printStackTrace();
			return -2;
		}
		return -3;
	}
	
	private Message constructMessage ( ArrayList<String> stdOutput){
	
		int messageid = Integer.parseInt(stdOutput.get(0));//TODO change this to long
		
		int SendID = Integer.parseInt(stdOutput.get(1));
		int receiverid    = Integer.parseInt(stdOutput.get(2));
		int qid = Integer.parseInt(stdOutput.get(3));
		int context = Integer.parseInt(stdOutput.get(4));
		int pri   = Integer.parseInt(stdOutput.get(5));
		
		String mes = stdOutput.get(6);
		Message out = new Message (SendID,qid,mes);
		out.setMessageId(messageid);
		out.setReceiverId(receiverid);
		out.setContext(context);
		out.setPriority(pri);
		
		return out;
	}
	
	/**
	 * 
	 * @param QueueID
	 * @return
	 */
	public Message getQueue(int QueueID,boolean removeAfter){  	
		return getQueuePriorityFirst(QueueID,removeAfter); 
	}
	
	
	
	
	
	public Message getQueuePriorityFirst(int QueueID,boolean removeAfter){  
		ArrayList<Message> outMessages = new ArrayList<Message>();
		String getMessagesOfThisQueue = "SELECT * FROM messages WHERE queueid="+QueueID+"" +
				"receiverid=-1 ORDER BY timeofarrival ASC LIMIT 2;";
		Message out =null;
		
		
			ArrayList<ArrayList<String>> respo = respondSQL(getMessagesOfThisQueue,messagesTableCols);
			
			int a =0;
			if( respo.size()>0)
				out= constructMessage(respo.get(0));
			if(removeAfter){
				String sqlRemove= "DELETE FROM messages WHERE messageid="+out.getMessageId()+";";
				sendSQL(sqlRemove);
			}
		
	
		
		
		
		return out; 
	}
	
	public Message getQueueClosestTime(int QueueID,boolean removeAfter){  
		ArrayList<Message> outMessages = new ArrayList<Message>();
		String getMessagesOfThisQueue = "SELECT * FROM messages WHERE queueid="+QueueID+"" +
				"receiverid=-1 ORDER BY timeofarrival DESC LIMIT 2;";
		Message out =null;
		
	
			ArrayList<ArrayList<String>> respo = respondSQL(getMessagesOfThisQueue,messagesTableCols);
			
			int a =0;
			if( respo.size()>0)
				out= constructMessage(respo.get(0));
			
			if(removeAfter){
				String sqlRemove= "DELETE FROM messages WHERE messageid="+out.getMessageId()+";";
				sendSQL(sqlRemove);
			}
		
		
		
		
		return out; 
	}
	
	

	

	

	public Message getPrivateMessageNotBroadcast(int senderID, int receiverID, int queueID, boolean removeAfter, boolean orderByTime, int context) {
		String getMessagesOfThisQueue;
		
		
		
		if(queueID ==-1){  // required for the request reponse functionality
						  // message receivers should search for messages for them, regardless of who sent them or in which queue they are
							// and reply to them
			
			getMessagesOfThisQueue = "SELECT * FROM messages WHERE "
									+ " receiverid = "+ receiverID
									+ " AND context !=0 "
									+ " ORDER BY ORDER BY timeofarrival LIMIT 1;";
		}
		
		else	if(senderID==-1){   // get a message for that particular reciever
			getMessagesOfThisQueue = "SELECT * FROM messages WHERE "
					+ "receiverid =" + receiverID 
					+ " AND queueid=" + queueID
					+ (context == 0? "" : " AND context=" + context)
					+ " ORDER BY timeofarrival LIMIT 1;";
		}
		else{
			
			getMessagesOfThisQueue = "SELECT * FROM messages WHERE "
					+ "senderid = "+senderID
					+ " AND receiverid =" + receiverID 
					+ " AND queueid=" + queueID
					+ (context == 0? "" : " AND context=" + context)
					+ " ORDER BY timeofarrival LIMIT 1;";
			
		}
		
		Message out =null;
		
		LOGGER.info("Executing query: " + getMessagesOfThisQueue);
		
		
			ArrayList<ArrayList<String>> respo = respondSQL(getMessagesOfThisQueue,messagesTableCols);
			
			int a =0;
			if(respo.size()==0){
				 return null;
			}
			out= constructMessage(respo.get(0));
			if(removeAfter){
				String sqlRemove= "DELETE FROM messages WHERE messageid="+out.getMessageId()+";";
				sendSQL(sqlRemove);
			}
	
		return out; 
	}
	
	

	
	public Message getPrivateMessage(int senderID, int receiverID, int queueID, boolean removeAfter, boolean orderByTime, int context) {
		String getMessagesOfThisQueue;
		
		
		if(queueID ==-1){  // required for the request reponse functionality
						  // message receivers should search for messages for them, regardless of who sent them or in which queue they are
							// and reply to them
			
			getMessagesOfThisQueue = "SELECT * FROM messages WHERE "
									+ " receiverid in (-1, 0,"+ receiverID+")"
									+ " AND context !=0 "
									+ " ORDER BY timeofarrival LIMIT 1;";
		}
		
		else	if(senderID==-1){   // get a message for that particular reciever
			getMessagesOfThisQueue = "SELECT * FROM messages WHERE "
					+ "receiverid in (-1, 0," + receiverID +")"
					+ " AND queueid=" + queueID
					+ (context == 0? "" : " AND context=" + context)
					+ " ORDER BY timeofarrival LIMIT 1;";
		}
		else{
			
			getMessagesOfThisQueue = "SELECT * FROM messages WHERE "
					+ "senderid = "+senderID
					+ " AND receiverid in (-1, 0," + receiverID +")"
					+ " AND queueid=" + queueID
					+ (context == 0? "" : " AND context=" + context)
					+ " ORDER BY timeofarrival LIMIT 1;";
			
		}
		
		Message out =null;
		
		LOGGER.info("Executing query: " + getMessagesOfThisQueue);
		
		
			ArrayList<ArrayList<String>> respo = respondSQL(getMessagesOfThisQueue,messagesTableCols);
			
			int a =0;
			if(respo.size()==0){
				 return null;
			}
			out= constructMessage(respo.get(0));
			if(removeAfter){
				String sqlRemove= "DELETE FROM messages WHERE messageid="+out.getMessageId()+";";
				sendSQL(sqlRemove);
			}
	
		return out; 
	}
	
	/**
	 * Author - OT
	 * This retrieves 1 single message with senderid = clientid
	 * If context = 0, context is ignored
	 * Added to handle RequestResponse case
	 * @param QueueID
	 * @param removeAfter
	 * @param orderByTime
	 * @param clientID Sender ID
	 * @param context
	 * @return
	 */
	public Message getPrivateMessageFromSender(int QueueID,boolean removeAfter,boolean orderByTime, int clientID, int context, int auther){  
		ArrayList<Message> outMessages = new ArrayList<Message>();
		String getMessagesOfThisQueue = "SELECT * FROM messages WHERE "
				+ " senderid = " + auther
				+ " AND receiverid in (-1, 0,"+ clientID+")"
				+ " AND queueid=" + QueueID
				+ (context == 0? "" : " AND context=" + context)
				+ " ORDER BY timeofarrival LIMIT 1;";
		Message out =null;
		
		LOGGER.info("Executing query: " + getMessagesOfThisQueue);
		
			ArrayList<ArrayList<String>> respo = respondSQL(getMessagesOfThisQueue,messagesTableCols);
			
			int a =0;
			if(respo.size()==0){
				 return null;
			}
			out= constructMessage(respo.get(0));
			if(removeAfter){
				String sqlRemove= "DELETE FROM messages WHERE messageid="+out.getMessageId()+";";
				sendSQL(sqlRemove);
			}
		
			
		
		
		return out; 
	}
	
	/**
	 * WhichChunk should start at 0 
	 * @param QueueID
	 * @param removeAfter
	 * @param orderByTime
	 * @param receiverID
	 * @param whichChunk
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<Message> getAllPrivateMessages(int QueueID,boolean removeAfter,boolean orderByTime, int receiverID,int whichChunk) throws SQLException{
		ArrayList<Message> outMessages = new ArrayList<Message>();
	
		String offset="";
		if(!removeAfter)
			offset=" OFFSET "+(resultChunkSize*whichChunk);
		
		
		String getMessagesOfThisQueue = "SELECT * FROM messages"
				+ " WHERE receiverid in (-1, 0," + receiverID + ")"
				+ " AND queueid=" + QueueID
				+ " ORDER BY " + ((orderByTime) ? "timeofarrival ASC" : "priority ASC")
				+ " LIMIT " + resultChunkSize + offset;
		
		LOGGER.info("Query executed: " + getMessagesOfThisQueue);
		
			
		
	
			ArrayList<ArrayList<String>> respo = respondSQL(getMessagesOfThisQueue,messagesTableCols);
			if(respo.size()==0){
				int a =0;
			}
			
			System.out.println("Size = " + respo.size() + ". Response messages from DB: ,");
			for( ArrayList<String> messageRet: respo){
				Message tmpM = constructMessage(messageRet);
				System.out.println(tmpM);
				if(removeAfter){
					String sqlRemove = "DELETE FROM messages WHERE messageid="
							+ tmpM.getMessageId();
					sendSQL(sqlRemove);
				}
				outMessages.add(tmpM);
				
			}
			
			

		
		System.out.println("end of method");
		return outMessages; 
	}
	
	
}
