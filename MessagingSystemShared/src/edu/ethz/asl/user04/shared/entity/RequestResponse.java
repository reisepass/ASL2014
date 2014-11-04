package edu.ethz.asl.user04.shared.entity;

import java.io.Serializable;
import java.util.UUID;

public class RequestResponse implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2298962223714761011L;
	public boolean success;
	private String extraInfo;
	private Object payload;
	public  StatTrack timing;
	public String getExtraInfo() {
		return extraInfo;
	}

	public void setExtraInfo(String extraInfo, StatTrack time) {
		this.extraInfo = extraInfo;
		this.timing = time;
		
	}

	public Object getPayload() {
		return payload;
	}

	public void setPayload(Object payload, StatTrack time) {
		this.payload = payload;
		this.timing = time;
	}

	public boolean isSuccess() {
		return success;
	}

	public UUID getRequestUUID() {
		return requestUUID;
	}

	private UUID requestUUID;
	

	
	public RequestResponse(boolean success) {
		super();
		this.success = success;
		extraInfo = "NoExtraInfo";
		this.timing = null;
	}
	public RequestResponse(boolean success, StatTrack time) {
		super();
		this.success = success;
		extraInfo = "NoExtraInfo";
		this.timing = time;
	}
	public RequestResponse(boolean success, Object load, StatTrack time ) {
		super();
		this.success = success;
		extraInfo = "NoExtraInfo";
		this.payload=load;
		this.timing = time; 
	}

	public RequestResponse(boolean success, String extraInfo, StatTrack time ) {
		super();
		this.success = success;
		this.extraInfo = extraInfo;
		this.timing = time; 
	}
	
	public RequestResponse(boolean success, UUID requestUUID, StatTrack time) {
		super();
		this.success = success;
		this.requestUUID = requestUUID;
		this.timing = time;
	}

}
