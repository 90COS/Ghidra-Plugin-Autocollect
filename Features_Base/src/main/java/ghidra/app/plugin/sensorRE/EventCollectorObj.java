package ghidra.app.plugin.sensorRE;

import ghidra.program.model.address.Address;

/*
 * Class to hold event object which contains relevant  
 * information about specific changed event e.g., event type, time, event source, etc..
 * Note, this class definition should be updated/changed as needed to meet
 * SensorRE requirements
 */
class EventCollectorObj {
	
	/* Just for tracking purpose */
	static int instances = 0;
	
	/* 
	 * All private data members
	 * Will need to add/remove as needed to
	 * match SensorRE requirements
	 */
	private String time;
	private String eventType;
	private String recordType;
	private String oldValue;
	private String newValue;
	private String startAddr;
	private String endAddr;
	private String sourceProgram;
	private String other;
	
	//Default constructor
	public EventCollectorObj() {
		time=null;
		eventType=null;
		oldValue=null;
		newValue=null;
		startAddr = null;
		endAddr = null;
		sourceProgram=null;
		other=null;
		instances++;
	}
	
	public EventCollectorObj(String time, String eventType, 
			String recordType, String oldValue, String newValue, 
			String startAddr, String endAddr, String sourceProgram, String other) {
		this.time=time;
		this.eventType=eventType;
		this.recordType=recordType;
		this.oldValue=oldValue;
		this.newValue=newValue;
		this.startAddr=startAddr;
		this.endAddr=endAddr;
		this.sourceProgram=sourceProgram;
		this.other=other;
		instances++;
	}
	
	
	/*
	 * Setters and getters
	 */
	
	public int getInstances() {
		return instances;
	}
	
	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getOldValue() {
		return oldValue;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public String getNewValue() {
		return newValue;
	}

	
	public String getRecordType() {
		return recordType;
	}

	public void setRecordType(String recordType) {
		this.recordType = recordType;
	}

	public String getStartAddr() {
		return startAddr;
	}

	public void setStartAddr(String startAddr) {
		this.startAddr = startAddr;
	}

	public String getEndAddr() {
		return endAddr;
	}

	public void setEndAddr(String endAddr) {
		this.endAddr = endAddr;
	}

	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}

	public String getSourceProgram() {
		return sourceProgram;
	}

	public void setSourceProgram(String sourceProgram) {
		this.sourceProgram = sourceProgram;
	}

	public String getOther() {
		return other;
	}

	public void setOther(String other) {
		this.other = other;
	}

	@Override
	public String toString() {
		return "EventCollectorObj [time=" + time + ", eventType=" + eventType + ", recordType=" + recordType
				+ ", oldValue=" + oldValue + ", newValue=" + newValue + ", startAddr=" + startAddr + ", endAddr="
				+ endAddr + ", sourceProgram=" + sourceProgram + ", other=" + other + "]";
	}



}