package edu.ethz.asl.user04.dbutils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import edu.ethz.asl.user04.shared.entity.Message;

public class _SchemeScript {  //F this script. it dont work no mo 
	public static void main( String[] args) throws SQLException{
		DBManager db = new DBManager("myuser2", "127.0.0.1", "5432", "mydb1", "spartan",100);
		Connection tmpC = db.newClientConnection();
		SQLUtil    sql = new SQLUtil(tmpC,5);
		
		sql.addMessage("ABABA", 1, 2, 1, 1, 1);
		sql.CreateClient(123, true);
		Message in = sql.getQueue(1,false);
		/* FIXME
		 * sql.CreateQueue(3, 123);
		sql.CreateQueue(3, 123);
		sql.CreateQueue(5, 123);
		sql.CreateQueue(1, 123);
		sql.CreateQueue(2, 123);
		sql.CreateQueue(3, 123);
		sql.CreateQueue(4, 123);
		sql.CreateQueue(5, 123);
		sql.CreateQueue(6, 123);
		sql.CreateQueue(7, 123);
		sql.CreateQueue(8, 123);
		sql.CreateQueue(9, 123);
		sql.CreateQueue(10,123);
		sql.CreateQueue(11,123);
		sql.CreateQueue(12,123);*/
		sql.addMessage("123", 123, 123, 1, 1, 3);
		sql.addMessage("dadas", 123, 123, 1, 1, 3);
		boolean suc  = sql.deleteQueue(3);
		boolean suc2 = sql.queueIdTaken(5);
		ArrayList<ArrayList<String>> xoxo=sql.listQueues();
		ArrayList<Integer> qList = new ArrayList<Integer>();
		qList.add(1);qList.add(2);qList.add(3);qList.add(4);qList.add(5);
		sql.multiMessage("multiMessage", 2, 123, 1, 1, qList);
		
		int a=0;
		
		
		/*
message_id (int)
sender_id (int)
receiver_id (int)
queue_id (int)
context (int)
priority (int)
time_of_arrival (timestamp)
message (varchar)
idx_sender: (sender_id)
idx_receiver: (receiver_id)
idx_queue: (queue_id)
idx_receiver_priority: (receiver_id, priority)
idx_receiver_timestamp:
(receiver_id, time_of_arrival)
		 */
		
		String createMessageTableSQL = "CREATE TABLE Message(" +
				"messageID 		integer," +
				"senderID		integer," +
				"receiverID		integer," +
				"queueID		integer," +
				"context		integer," +
				"priority		integer," +
				"timeOfArrial	timestamp DEFAULT current_timestamp," +
				"message		varchar(2000));";
		int err1=sql.sendSQL(createMessageTableSQL);
				
		
		String createClientTableSQL = "CREATE TABLE Client(" +
				"clientTyp   varchar," +
				"clientID	 integer PRIMARY KEY," +
				"clientName	 varchar," +
				"creationTime timestamp DEFAULT current_timestamp);";
		int err10=sql.sendSQL(createClientTableSQL);
		
		String createQtable = " CREATE TABLE Queue(" +
				"queueID	integer PRIMARY KEY," +
				"queueName	varchar," +
				"createdBy	integer," +
				"creationTime	timestamp DEFAULT current_timestamp);";
		int err11=sql.sendSQL(createQtable);
		
		String createIndexOnMessage = "CREATE UNIQUE INDEX messageID_idx ON Message (messageID);";
		int err2=sql.sendSQL(createIndexOnMessage);
		createIndexOnMessage = "CREATE INDEX senderID_idx ON Message (senderID);";
		int err3=sql.sendSQL(createIndexOnMessage);
		createIndexOnMessage = "CREATE  INDEX queueID_idx ON Message (queueID);";
		int err4=sql.sendSQL(createIndexOnMessage);
		createIndexOnMessage = "CREATE INDEX timeOfArrial_idx ON Message (timeOfArrial);";
		int err5=sql.sendSQL(createIndexOnMessage);
//		createIndexOnMessage = "CREATE UNIQUE INDEX priority_idx ON Message (priority);";
		int err6=sql.sendSQL(createIndexOnMessage);

	
		int a7 =0;
	
	}
}
