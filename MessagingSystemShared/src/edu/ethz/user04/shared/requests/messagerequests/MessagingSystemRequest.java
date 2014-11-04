package edu.ethz.user04.shared.requests.messagerequests;

import java.io.Serializable;
import java.util.UUID;

import edu.ethz.asl.user04.shared.entity.ConfigExperimentV2;

public class MessagingSystemRequest implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final UUID requestUUID;
	private ConfigExperimentV2 cfg;
	
	public MessagingSystemRequest() {
		this.requestUUID = UUID.randomUUID();
		cfg = null;
	}

	public MessagingSystemRequest( ConfigExperimentV2 cfg) {
		this.requestUUID = UUID.randomUUID();
		this.cfg = cfg;
	}
	
	public UUID getRequestUUID() {
		return this.requestUUID;
	}
	
	public void setCFG(ConfigExperimentV2 cfg){
		this.cfg = cfg;
	}
	
	public ConfigExperimentV2 getCFG(){
		return cfg;
	}
}
