package ghidra.app.plugin.sensorRE;

/*
 * Class to hold event object which contains relevant  
 * information about specific changed event e.g., event type, time, event source, etc..
 * Note, this class definition should be updated/changed as needed to meet
 * SensorRE requirements
 */
class EventCollectorObj {
	private String time;
	private String eventType;
	private String oldValue;
	private String newValue;
	private String sourceProgram;
	private String other;
	
	//Default constructor
	public EventCollectorObj() {
		time=null;
		eventType=null;
		oldValue=null;
		newValue=null;
		sourceProgram=null;
		other=null;
	}
	
	public EventCollectorObj(String time, String eventType, String oldValue, String newValue, String sourceProgram, String other) {
		this.time=time;
		this.eventType=eventType;
		this.oldValue=oldValue;
		this.newValue=newValue;
		this.sourceProgram=sourceProgram;
		this.other=other;
	}
	
	
	/*
	 * Setters and getters
	 */
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
		return "EventCollectorObj [time=" + time + ", eventType=" + eventType + ", oldValue=" + oldValue
				+ ", newValue=" + newValue + ", sourceProgram=" + sourceProgram + ", other=" + other + "]";
	}
}