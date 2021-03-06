package edu.ethz.asl.user04.messagebroker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import edu.ethz.asl.user04.dbutils.DBManager;
import edu.ethz.asl.user04.shared.entity.Message;
import edu.ethz.asl.user04.shared.logging.MessagingSystemLogger;

public class MessageBrokerService {

	protected static final int HANDLERS = 15;
	protected static final int CONNECTIONLIMIT =25;//100 is the postgres internal limit and it needs some for itself
	protected static final int SERVER_PORT = 5009;
	
	private ExecutorService pool;
	private DBManager       dbPool;
	private AtomicInteger dbQueueCount;
	private AtomicInteger mwQueueCount;
	
	public MessageBrokerService() {
		run();
	}
	
	public void run(){
		ListenerService listener;
		try {
			Properties server_prop = new Properties();
			server_prop.load(new FileInputStream("properties/mbs.properties"));
			System.out.println("### Starting Pool1");
			pool = Executors.newFixedThreadPool(Integer.parseInt(server_prop
							.getProperty("mw_message_handlers_pool_size",
									(new Integer(HANDLERS)).toString())));
			
			dbPool= new DBManager(server_prop.getProperty("db_username"),
					server_prop.getProperty("db_url"),
					server_prop.getProperty("db_port"),
					server_prop.getProperty("db_name"),
					server_prop.getProperty("db_password"),
					Integer.parseInt(server_prop.getProperty("db_connection_limit")));
			System.out.println("### Starting mw1");
			dbQueueCount = null;
			mwQueueCount = null;
			try{
				if(server_prop.getProperty("mw_useDBQCounter")=="true"){
					dbQueueCount = new AtomicInteger(0);
				}
				if(server_prop.getProperty("mw_useMWQCounter")=="true"){
					mwQueueCount = new AtomicInteger(0);
				}
				
			}
			finally{
				
			}
			
			listener = new ListenerService(Integer.parseInt(server_prop
					.getProperty("mw_server_port",
							(new Integer(SERVER_PORT)).toString())), pool,
					dbPool,dbQueueCount,mwQueueCount );
	        pool.submit(listener);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void shutdown() {
		System.out.println("Shutting down");
		pool.shutdown();
		System.out.println("Shutdown complete");
	}
	
	public static void main(String[] args) throws IOException {

		// MessagingSystemLogger.setup();
		MessageBrokerService mbs = new MessageBrokerService();
	}

}
