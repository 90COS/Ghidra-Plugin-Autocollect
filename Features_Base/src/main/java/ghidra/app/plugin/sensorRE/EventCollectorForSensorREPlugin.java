package ghidra.app.plugin.sensorRE;

import java.awt.Font;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
//import com.google.gson.JsonElement;
//import com.google.gson.stream.JsonWriter;

import docking.help.Help;
import docking.help.HelpService;
import ghidra.app.DeveloperPluginPackage;
import ghidra.app.events.ProgramActivatedPluginEvent;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.framework.model.*;
import ghidra.framework.plugintool.*;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.util.*;
import ghidra.util.Msg;
import ghidra.util.datastruct.IntObjectHashtable;

/**
  * Plugin to show domain object change events.
  */
//@formatter:off
@PluginInfo(
	status = PluginStatus.RELEASED,
	packageName = DeveloperPluginPackage.NAME,
	category = PluginCategoryNames.TESTING,
	shortDescription = "Capture domain object (binary file being analyzed) events",
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
	private EventCollectorObj eventCollectorObj; 
	private int count;
	private int callBack;
	private Gson gson;

	/**
	  * Constructor
	  */
	public EventCollectorForSensorREPlugin(PluginTool tool) {

		super(tool);

		eventHt = new IntObjectHashtable<>();
		eventJsonArray = new ArrayList<>();
		gson = new GsonBuilder().setPrettyPrinting().create();
		eventCollectorObj = null;
		count = 0;
		callBack = 0;
		eventCollectorDocker = new EventCollectorForSensorREPluginDockerProvider(tool, eventJsonArray, getName());
		
		// Note: this plugin is categorized as 'Developer' category and as such does not need help 
		HelpService helpService = Help.getHelpService();
		helpService.excludeFromHelp(eventCollectorDocker);
	}

	/**
	 * Plug in events as they come.
	 * @param event: plug in generated events
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
			Msg.debug(this, "domainObjectChanged callback " + ++callBack + "X for event with " + ev.numRecords() + " records!" );
			update(ev);
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
	private void update(DomainObjectChangedEvent event) {
		
		String eventString;
		String eventName;
		String startAddr;
		String endAddr;
		String oldValue;
		String newValue;
		String affectedObj;
		String dateStr;
		int eventType;
		
			
		/*
		 * Since DomainObjectChangedEvent object can hold multiple events,
		 * as such, need to make sure that we process all of the events reported
		 */
		for (int i = 0; i < event.numRecords(); i++) {
			eventString = null;
			eventName = null;
			startAddr = null;
			endAddr = null;
			oldValue = null;
			newValue = null;
			affectedObj = null;
			dateStr = new Date().toString();
			eventType = 0;
			eventCollectorObj = null;

			DomainObjectChangeRecord docr = event.getChangeRecord(i);
			eventType = docr.getEventType();
			
			
			/*
			 * Is this event related to any specific program/binary changes such as
			 * address and/or offset of the program being analyzed?
			 * ProgramChangeRecord has 3 private data members:
			 * Address start;
			 * Address end;
			 * Object affectedObj; 
			 */
			if (docr instanceof ProgramChangeRecord) {
				ProgramChangeRecord record = (ProgramChangeRecord) docr;
				try {
					startAddr = "" + record.getStart();
					endAddr = "" + record.getEnd();
					oldValue = "" + record.getOldValue();
					newValue = "" + record.getNewValue();
					affectedObj = "" + record.getObject();
					
					
					eventCollectorObj = new EventCollectorObj(dateStr, 
	                            getEventName(eventType), 
	                            ProgramChangeRecord.class.getSimpleName(),
	                            oldValue, newValue, startAddr, endAddr, event.getSource().toString(), 
	                            "***Program change event***" );
					
				}
				catch (Exception e) {
					eventCollectorObj = new EventCollectorObj(dateStr, 
							                                    getEventName(eventType), 
							                                    ProgramChangeRecord.class.getSimpleName(),
							                                    null, null, null, null, event.getSource().toString(), 
							                                    "=> *** Exception: Event data is not available ***" );
				}
			}else if (docr instanceof CodeUnitPropertyChangeRecord) {
				CodeUnitPropertyChangeRecord record = (CodeUnitPropertyChangeRecord) docr;
				eventCollectorObj = new EventCollectorObj(dateStr, getEventName(eventType), 
															CodeUnitPropertyChangeRecord.class.getSimpleName(),
						                                    oldValue, newValue, null, null, event.getSource().toString(),
						                                    " (" + eventType + ") propertyName: " 
						                                    + record.getPropertyName() 
						                                    + "; code unit address: " + record.getAddress().toString());
			}else if (docr instanceof DomainObjectChangeRecord) {
				DomainObjectChangeRecord record = (DomainObjectChangeRecord) docr;
				eventCollectorObj = new EventCollectorObj(dateStr, getEventName(eventType), 
															DomainObjectChangeRecord.class.getSimpleName(),
						                                    record.getOldValue().toString(), record.getNewValue().toString(), 
						                                    null, null, event.getSource().toString(),
						                                    "SubEvent Type:" + record.getSubEventType());
			}else{//To catch all other unknown cases
				eventCollectorObj = new EventCollectorObj(dateStr, getEventName(eventType, DomainObject.class), 
														docr.getClass().getSimpleName(),
														docr.getOldValue().toString(), docr.getNewValue().toString(), 
							                            null, null,
							                            event.getSource().toString(),
														"Unknown event type");
			}
		
			/*
			 * Time to display to plugin console and save to array
			 * for writing to file later when user requests
			 * In the case for ProgramChangeRecord, there are times when
			 * oldValue and newValue are null, should we include those events?
			 */
			//if (oldValue != null && !oldValue.equals(newValue)) {
				eventString = gson.toJson(eventCollectorObj) + "\n";
				eventCollectorDocker.displayEvent(eventString);
				eventJsonArray.add(eventString);
			//}
		}//For loop
	
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

}
