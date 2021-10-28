/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package autocollect;

import java.awt.BorderLayout;

import javax.swing.*;
import java.util.Date;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.List;


import docking.ActionContext;
import docking.action.DockingAction;
import docking.action.ToolBarData;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.framework.plugintool.*;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.util.HelpLocation;
import ghidra.util.Msg;
import resources.Icons;
import ghidra.app.events.ProgramLocationPluginEvent;
import ghidra.program.util.*;
import ghidra.util.datastruct.IntObjectHashtable;
import ghidra.framework.model.ToolListener;

/**
 * TODO: Provide class-level documentation that describes what this plugin does.
 */
//@formatter:off
@PluginInfo(
	status = PluginStatus.STABLE,
	packageName = "AutoCollect",
	category = PluginCategoryNames.SUPPORT,
	shortDescription = "Capture, Collect, and Report Changes",
	description = "Capture, Collect, and Report Changes"
)
//@formatter:on
public class AutoCollectPlugin extends Plugin implements ToolListener {

	MyProvider provider;
	private IntObjectHashtable<String> eventHt;
	private String padString;

	public AutoCollectPlugin(PluginTool tool) {
		super(tool);
		String pluginName = getName();
		provider = new MyProvider(this, pluginName);
		String topicName = this.getClass().getPackage().getName();
		String anchorName = "HelpAnchor";
		provider.setHelpLocation(new HelpLocation(topicName, anchorName));
		
		eventHt = new IntObjectHashtable<>();
		String dateStr = new Date() + ": ";
		padString = dateStr.replaceAll(".", " ");
		
		tool.addToolListener(this);
		tool.addListenerForAllPluginEvents(this);
		
	}

	
	@Override
	public void processEvent(PluginEvent event) {
		provider.processEvent(event);
	}
	
	@Override
	public void processToolEvent(PluginEvent event) {
		provider.processEvent(event);
	}
	
	@Override
	public void dispose() {
		tool.removeListenerForAllPluginEvents(this);
	}

		
	// TODO: If provider is desired, it is recommended to move it to its own file
	private static class MyProvider extends ComponentProviderAdapter  {
		private JPanel panel;
		private JFrame frame;
		private DockingAction action;
		private JTextArea textArea;
		private JPanel reportPanel;
		private String reportURL;
		
	    private JButton b; 
	    private JLabel l;
		public MyProvider(Plugin plugin, String owner) {
			super(plugin.getTool(), owner, owner);
			buildPanel();
			createActions();
		}

		// Customize GUI
		private void buildPanel() {
			panel = new JPanel(new BorderLayout());
			textArea = new JTextArea(5, 10);
			textArea.setEditable(false);
			panel.add(new JScrollPane(textArea));
			panel.setBackground(Color.red);
			setVisible(true);
		}

		// TODO: Customize actions
		private void createActions() {
			frame = new JFrame("InputFrame");
			frame.setVisible(false);		
			reportURL = "http://localhost";
			action = new DockingAction("Set Reporting URL", getName()) {
				@Override
				public void actionPerformed(ActionContext context) {
					String result = (String)JOptionPane.showInputDialog(
				               frame,
				               "Select the URL of the Reporting Server", 
				               "Reporting Server URL",            
				               JOptionPane.PLAIN_MESSAGE,
				               null,            
				               null, 
				               reportURL
				            );
					if(result != null && result.length() > 0 && 
							(result.startsWith("https://") || result.startsWith("http://"))){
						reportURL = result;
						Msg.showInfo(getClass(), panel, "Success", "Reporting URL changed to "+reportURL);
		            }else if (result != null) {
		            	Msg.showWarn(getClass(), panel, "Error", "Reporting URL not acceptable, remaining as "+reportURL);
		            } else {
		            	// User pressed cancel, don't bother them
		            }
				}
			};
			action.setToolBarData(new ToolBarData(Icons.ADD_ICON, null));
			action.setEnabled(true);
			action.markHelpUnnecessary();
			dockingTool.addLocalAction(this, action);
		}
		
		private void printLocationDetails(PluginEvent event) {
			if (event instanceof ProgramLocationPluginEvent) {
				ProgramLocationPluginEvent l = (ProgramLocationPluginEvent) event;
				ProgramLocation location = l.getLocation();
				textArea.append("\t" + location.toString());
				textArea.append("\n");
			}
		}
		public void processEvent(PluginEvent event) {
			String date = new Date().toString();
			if (true) {
				textArea.append(date + "     " + event.getEventName());
				textArea.append("\n");	
			} else {
				textArea.append(date + "     " + event.toString());
				printLocationDetails(event);
				textArea.append("\n");	
			}
			
		}
		private void clear() {
			textArea.setText("");
		}
		@Override
		public JComponent getComponent() {
			return panel;
		}
	}
}
