package edu.ethz.asl.user04.dbutils;

import java.sql.Connection;

public interface DBConnector {
	public Connection newClientConnection();
	public void initDB();
	public String getUser() ;

	public void setUser(String user) ;

	public String getDbip() ;

	public void setDbip(String dbip) ;
	public String getDbPort() ;

	public void setDbPort(String dbPort) ;

	public String getDbName() ;

	public void setDbName(String dbName) ;

	public String getUsrPass() ;

	public void setUsrPass(String usrPass) ;

	public int getNumMaxOpen() ;

	public void setNumMaxOpen(int numMaxOpen) ;

}
