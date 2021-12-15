package ghidra.app.plugin.sensorRE;

import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonWriter;

import docking.help.Help;
import docking.help.HelpService;
import ghidra.app.DeveloperPluginPackage;
import ghidra.app.events.ProgramActivatedPluginEvent;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.framework.model.*;
import ghidra.framework.plugintool.*;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.listing.Program;
import ghidra.program.util.*;
import ghidra.util.Msg;
import ghidra.util.datastruct.IntObjectHashtable;

/**
  * Debug Plugin to show domain object change events.
  */
//@formatter:off
@PluginInfo(
	status = PluginStatus.RELEASED,
	packageName = DeveloperPluginPackage.NAME,
	category = PluginCategoryNames.TESTING,
	shortDescription = "Capture domain object events",
	description = "This plugin captures domain object events " +
			"as they are generated, then save these events to a json file that " + 
			" can be transmitted to an RPC server for SensorRE client. The maximum number of messages shown is " +
			EventCollectorForSensorREPluginDockerProvider.LIMIT,
	eventsConsumed = { ProgramActivatedPluginEvent.class }
)
//@formatter:on
public class EventCollectorForSensorREPlugin extends Plugin implements DomainObjectListener {

	private Program currentProgram;
	private EventCollectorForSensorREPluginDockerProvider eventCollectorDocker;
	private IntObjectHashtable<String> eventHt;
	
	
	private ArrayList<String> eventJsonArray; //Contains captured events in json format
	/*
	 * Future implementation will ask user for file name
	 * to store events as json objects
	 */
//	String jsonOutFileName = "/home/scada/eventCollectorJsonFile.json"; 
//	File outFile; 
//	FileWriter writer; 

	private int count;
	private int callBack;

	/**
	  * Constructor
	  */
	public EventCollectorForSensorREPlugin(PluginTool tool) {

		super(tool);

		eventHt = new IntObjectHashtable<>();
		eventJsonArray = new ArrayList<>();
		count = 0;
		callBack = 0;
		eventCollectorDocker = new EventCollectorForSensorREPluginDockerProvider(tool, eventJsonArray, getName());
		
		// Note: this plugin is categorized as 'Developer' category and as such does not need help 
		HelpService helpService = Help.getHelpService();
		helpService.excludeFromHelp(eventCollectorDocker);
	}

	/**
	 * Put event processing code here.
	 */
	@Override
	public void processEvent(PluginEvent event) {
		if (event instanceof ProgramActivatedPluginEvent) {
			ProgramActivatedPluginEvent ev = (ProgramActivatedPluginEvent) event;
			Program newProg = ev.getActiveProgram();
			if (currentProgram != null) {
				currentProgram.removeListener(this);
			}
			if (newProg != null) {
				newProg.addListener(this);
				Msg.debug(this, "processEvent() occured:" + count++);
			}
		}
	}

	/**
	 * Tells a plugin that it is no longer needed.  The plugin should remove
	 * itself from anything that it is registered to and release any resources.
	 */
	@Override
	public void dispose() {
		if (currentProgram != null) {
			currentProgram.removeListener(this);
		}
	}

	/**
	 * This is the callback method for DomainObjectChangedEvents.
	 */
	@Override
	public void domainObjectChanged(DomainObjectChangedEvent ev) {
		if (tool != null && eventCollectorDocker.isVisible()) {
			try {
				update(ev);
				Msg.debug(this, "domainObjectChanged called " + ++callBack + " times for event with " + ev.numRecords() + " records!" );
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get the font for the text area; font property will show up on the
	 * plugin property sheet.
	 */
	public Font getFont() {
		return eventCollectorDocker.getFont();
	}

	/**
	 * Set the font for the text area; font property will show up on the
	 * plugin property sheet.
	
	 */
	public void setFont(Font font) {
		eventCollectorDocker.setFont(font);
		tool.setConfigChanged(true);
	}

	/**
	 * Apply the updates that are in the change event.
	 */
	private void update(DomainObjectChangedEvent event) throws Exception{
		
		EventCollectorObj eventCollectorObj = null; //To store individual event

		
		/*
		 * Since there can be multiple events with multiple calls to update()
		 * Need to clear the event array each time to prevent writing duplications
		 * to file
		 */
		//eventJsonArray.clear();
		
		/*
		 * Open/reopen file to write events to
		 */
		//writer = new FileWriter(outFile, true);
			
		/*
		 * To support creation/read/write json object
		 */
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		for (int i = 0; i < event.numRecords(); i++) {
			String s = null;
			String start = null;
			String end = null;
			String oldValue = null;
			String newValue = null;
			String affectedObj = null;
			String dateStr = new Date() + ": ";
			int eventType = 0;
			

			DomainObjectChangeRecord docr = event.getChangeRecord(i);
			eventType = docr.getEventType();
			if (docr instanceof ProgramChangeRecord) {
				ProgramChangeRecord record = (ProgramChangeRecord) docr;

				try {
					start = "" + record.getStart();
					end = "" + record.getEnd();
					oldValue = "" + record.getOldValue();
					newValue = "" + record.getNewValue();
					affectedObj = "" + record.getObject();
				}
				catch (Exception e) {
					eventCollectorObj = new EventCollectorObj(dateStr, 
							                                    getEventName(eventType), 
							                                    null, null, event.getSource().toString(), 
							                                    "=> *** Exception: Event data is not available ***" );
				}
			}
			else if (docr instanceof CodeUnitPropertyChangeRecord) {
				CodeUnitPropertyChangeRecord record = (CodeUnitPropertyChangeRecord) docr;
				eventCollectorObj = new EventCollectorObj(dateStr, getEventName(eventType), 
						                                    oldValue, newValue, event.getSource().toString(),
						                                    " (" + eventType + ") ==> propertyName = " + record.getPropertyName() + ", code unit address = " + record.getAddress());
			}
			else {
				s = getEventName(eventType, DomainObject.class);
				if (s != null) {
					eventCollectorObj = new EventCollectorObj(dateStr, s, 
							                                    oldValue, newValue, 
							                                    event.getSource().toString(),
							                                    null);
				}
			}
			
			/*
			 * Time to display event to console and write to file in json format
			 * But first, need to catch all other cases in which eventCollectorObj is still null,
			 * so we will have to construct event change based only on basic data such as
			 * eventType, old/new value, and of course the source program/binary
			 */
			if (eventCollectorObj == null) {
				eventCollectorObj = new EventCollectorObj(dateStr, 
						                                    getEventName(eventType), 
						                                    oldValue, 
						                                    newValue, 
						                                    event.getSource().toString(), null);
				s = gson.toJson(eventCollectorObj) + "\n";
				eventJsonArray.add(s);
				if (oldValue != null && !oldValue.equals(newValue)) {
					eventCollectorDocker.displayEvent(s);
				}
			}else {
				s = gson.toJson(eventCollectorObj) + "\n";
				eventJsonArray.add(s);
				eventCollectorDocker.displayEvent(s);
			}
		}
		
//		for (String s : eventJsonArray) {
//			writer.write(s);
//		}
//		eventCollectorDocker.displayEvent("Wrote " + eventJsonArray.size() + " events to file: " + "\"" + jsonOutFileName + "\"\n");
//		writer.write("Wrote " + eventJsonArray.size() + " events to file: " + "\"" + jsonOutFileName + "\"\n");
//		writer.close();
	}

	/**
	 * Use reflection to get the name of the given eventType.
	 */
	private String getEventName(int eventType) {

		String eventName = eventHt.get(eventType);
		if (eventName != null) {
			return eventName;
		}
		eventName = getEventName(eventType, ChangeManager.class);

		if (eventName == null) {
			// could be from the DomainObject class...
			eventName = getEventName(eventType, DomainObject.class);
		}

		eventHt.put(eventType, eventName);
		return eventName;
	}

	private String getEventName(int eventType, Class<?> c) {
		String eventName = null;
		Field[] fields = c.getFields();
		for (Field field : fields) {
			try {
				Object obj = field.get(null);
				int value = field.getInt(obj);
				if (eventType == value) {
					eventName = field.getName();
					break;
				}
			}
			catch (IllegalArgumentException e) {
				//ignore
			}
			catch (IllegalAccessException e) {
				//ignore
			}
		}
		return eventName;
	}


	/*
	 * Inner Class to hold event object which contains relevant  
	 * information about the changed event e.g., event type, time, event source, etc..
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
}
