package edu.ethz.asl.user04.messagebroker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.ethz.asl.user04.dbutils.DBManager;
import edu.ethz.asl.user04.shared.logging.MessagingSystemLogger;

public class ListenerService implements Runnable {
	
	public final static Logger LOGGER = MessagingSystemLogger
			.getLoggerForClass(ListenerService.class.getName());
	
	private final int serverPort;
	private ExecutorService pool;
	private ServerSocket serverSocket;
	private DBManager connectionPool;
	private AtomicInteger dbQueueCount;
	private AtomicInteger mwQueueCount;
	public ListenerService(int serverPort, ExecutorService pool,DBManager cPool, AtomicInteger dbQueueCount, AtomicInteger mwQueueCount ) throws IOException {
		this.serverPort = serverPort;
		this.pool = pool;
		connectionPool = cPool;
		this.dbQueueCount=dbQueueCount;
		this.mwQueueCount=mwQueueCount;
		serverSocket = new ServerSocket(serverPort);
	}

	@Override
	public void run() {
		while (true) {
			try {
				System.out.println("### Started Socket");
				// Block and wait for a client connection
                Socket client = serverSocket.accept();
                
                // Assign a thread to handle client network communication
               // System.out.println("ListenerService: client connected...");
                
                int mwQStart=-1;
                if(mwQueueCount!=null)
                	mwQStart=mwQueueCount.getAndIncrement();
                long gotNewSocket = System.currentTimeMillis();
                MessageHandler handler = new MessageHandler(client,connectionPool,dbQueueCount,mwQueueCount,mwQStart,gotNewSocket,true);
            //    System.out.println(connectionPool.describeConnectionPool());
                
                pool.execute(handler);
                
                /**
                 * TODO Instantiate MessageHandler with Message instead of Socket
                 * Hence, the steps will be as follows:
                 * 1. Block and wait for client connection
                 * 2. Open Datastream between this connection and read the Message object
                 * 3. Instantiate MessageHandler with this Message
                 * 
                 * Advantages:
                 * - We can make MessageHandler socket-oblivious
                 * Disadvantages:
                 * - MessageHandler cannot directly write the result into the socket
                 */
                
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Oh noes! Something went wrong while listening for incoming connections!", e);
			}
		}
	}

}
