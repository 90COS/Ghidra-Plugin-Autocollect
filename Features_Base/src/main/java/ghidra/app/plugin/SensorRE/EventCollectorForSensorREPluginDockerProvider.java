package ghidra.app.plugin.sensorRE;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
//import java.awt.Color;
//import java.awt.Component;
//import java.awt.Container;
//import java.awt.Cursor;
//import java.awt.Dimension;
//import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Color;
//import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//import java.awt.KeyboardFocusManager;
//import java.awt.Point;
//import java.awt.Rectangle;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.ComponentAdapter;
//import java.awt.event.ComponentEvent;
//import java.awt.event.ContainerEvent;
//import java.awt.event.ContainerListener;
//import java.awt.event.KeyEvent;
//import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
import java.util.List;
//import java.util.Map;
//import java.util.Set;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
//import javax.swing.event.ListSelectionEvent;
//import javax.swing.event.ListSelectionListener;
//import javax.swing.event.MouseInputAdapter;

//import org.apache.commons.lang3.StringUtils;
//import org.jdesktop.animation.timing.Animator;
//import org.jdesktop.animation.timing.TimingTargetAdapter;

import docking.ActionContext;
//import docking.DialogComponentProviderPopupActionManager;
//import docking.DockingActionProxy;
//import docking.DockingDialog;
//import docking.DockingWindowManager;
//import docking.TaskScheduler;
//import docking.Tool;
import docking.WindowPosition;
//import docking.action.ActionContextProvider;
import docking.action.DockingAction;
//import docking.action.DockingActionIf;
import docking.action.ToolBarData;
//import docking.actions.KeyBindingUtils;
//import docking.event.mouse.GMouseListenerAdapter;
//import docking.help.HelpService;
//import docking.menu.DialogToolbarButton;
//import docking.util.AnimationUtils;
import docking.widgets.filechooser.GhidraFileChooser;
//import docking.widgets.label.GDHtmlLabel;
//import docking.widgets.label.GDLabel;
//import docking.widgets.list.GList;
//import docking.wizard.IllegalPanelStateException;
//import docking.wizard.PanelManager;
//import docking.wizard.WizardManager;
//import docking.wizard.WizardPanel;
//import ghidra.framework.client.RepositoryServerAdapter;
//import ghidra.framework.model.ServerInfo;
import ghidra.framework.plugintool.ComponentProviderAdapter;
import ghidra.framework.plugintool.PluginTool;
//import ghidra.util.HelpLocation;
//import ghidra.util.MessageType;
import ghidra.util.Msg;
import ghidra.util.StatusListener;
//import ghidra.util.StatusListener;
//import ghidra.util.Swing;
//import ghidra.util.SystemUtilities;
//import ghidra.util.exception.AssertException;
import ghidra.util.layout.MiddleLayout;
//import ghidra.util.task.Task;
//import ghidra.util.task.TaskListener;
//import ghidra.util.task.TaskMonitor;
//import ghidra.util.task.TaskMonitorComponent;
import resources.ResourceManager;
import ghidra.framework.client.ClientUtil;
//import utility.function.Callback;
import ghidra.framework.client.RepositoryServerAdapter;
import ghidra.framework.main.*;
import ghidra.framework.model.ServerInfo;

//import java.awt.event.*;



public class EventCollectorForSensorREPluginDockerProvider extends ComponentProviderAdapter {
	final static int LIMIT = 300;
	
	/*
	 * The following are all GUI required data members
	 */
	private final static ImageIcon UPLOAD_ICON = ResourceManager.loadImage("images/up.png");
	private final static ImageIcon PLUGIN_ICON = ResourceManager.loadImage("images/plugin.png");
	private static final ImageIcon SAVE_ICON = ResourceManager.loadImage("images/disk.png");
	private static final ImageIcon ERASE_ICON = ResourceManager.loadImage("images/erase16.png");
	private JButton clearButton;
	private JButton connectButton;
	private JButton cancelButton;
	private JFrame mainFrame;
	private JTextArea textArea;
	private JScrollPane scrollPane;
	private DockingAction clearAction, RPCServer, saveEventToFile;
	
	
	
	/*
	 * All RPC server connection data members
	 */
	//private RepositoryChooser rpcServerChooser;
	private ServerInfoComponent serverInfoComponent;
	private String serverName;
	private int serverPort;
	private ServerInfo serverInfo;
	private RepositoryServerAdapter rpcServer;
	
	/*
	 * Changed events related data members
	 */
	private List<String> eventList;
	private File jsonFileName;
	private ArrayList<String> eventJsonArray; //Contains captured events in json format
	private FileWriter writer;
	
		
	public EventCollectorForSensorREPluginDockerProvider(PluginTool tool, ArrayList<String> eventJsonArray, String name) {
		super(tool, name, name);
		/*
		 * Default RPC server info
		 */
		serverName = "127.0.0.1";
		serverPort = 11300;
		jsonFileName = null;
		this.eventJsonArray = eventJsonArray;
		
		//serverInfo = new ServerInfo(serverName, serverPort);
		eventList = new ArrayList<>();
		
		textArea = new JTextArea(10, 80);
		textArea.setEditable(false);
		scrollPane = new JScrollPane(textArea);

		clearWindowContentAction();
		saveEventsToJsonFile();
		obtainRPCServerIPandPort();
		
		setIcon(PLUGIN_ICON);
		setDefaultWindowPosition(WindowPosition.BOTTOM);
		setTitle("SensorRE Event Collector PlugIn");
		setVisible(true);
	}
	
	/*
	 * ****************************************************************************
	 * This section handles SensorRE "Clear console" action.
	 * Note, this action only clear what user see on the console and
	 * not the actual event list
	 * ****************************************************************************
	 */
	private void clearWindowContentAction() {
		clearAction = new DockingAction("Clear events in Console", getName()) {
			@Override
			public void actionPerformed(ActionContext context) {
				clear();
			}
		};

		clearAction.markHelpUnnecessary();
		clearAction.setEnabled(true);
		ImageIcon icon = ERASE_ICON;
		clearAction.setToolBarData(new ToolBarData(icon));
		addLocalAction(clearAction);
	}

	/*
	 * Supported method of clearWindowContentAction()
	 */
	private void clear() {
		textArea.setText("");
		eventList.clear();
	}
	/*
	 * ********************************************************************************
	 * End of "Clear console" action
	 * ********************************************************************************
	 */
	

	/*
	 * ********************************************************************************
	 * This section handles "Save to file" icon which is located on the top right 
	 * corner of the Sensor RE docker
	 * 
	 * When clicked by user, all currently saved changed events will be written
	 * to user selected file  
	 * ********************************************************************************
	 */
	private void saveEventsToJsonFile() {
		saveEventToFile = new DockingAction("Save Events to JSON file", getName()) {
			@Override
			public void actionPerformed(ActionContext context) {
				try {
					saveEventsToFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		};
		saveEventToFile.markHelpUnnecessary();
		saveEventToFile.setEnabled(true);
		ImageIcon icon = SAVE_ICON;
		saveEventToFile.setToolBarData(new ToolBarData(icon));
		addLocalAction(saveEventToFile);
	}
	
	/*
	 * Internal method to save events to user selected file
	 */
	private void saveEventsToFile() throws IOException {
		String outFile = null;
		jsonFileName = askFile("Select a file to save events to", "SELECT");
		if (jsonFileName != null) {
			outFile = jsonFileName.getAbsolutePath();
			writer = new FileWriter(outFile, true);
			/*
			 * For debug purpose only if needed
			 */
			Msg.debug(this, "Selected json file is: " + outFile);
			/*
			 * Write all changed events to user selected file
			 */
			for (String s : eventJsonArray) {
				writer.write(s);
			}
			/*
			 * Notify user
			 */
			displayEvent("Wrote " + eventJsonArray.size() + " events to file: " + "\"" + outFile + "\"\n");
		}
		if (null != writer) {
			writer.close();
		}
		
	}

	/**
	 * Method to ask for a file (copied from GhidraScript).
	 * @param title popup window title
	 * @param approveButtonText text for the "yes" button
	 * @return the file chosen, or null
	 */
	private File askFile(final String title, final String approveButtonText) {
		final GhidraFileChooser chooser = new GhidraFileChooser(tool.getActiveWindow());
		chooser.setApproveButtonText(approveButtonText);
		chooser.setTitle(title);
		//chooser.setFileSelectionMode(GhidraFileChooser.FILES_ONLY);
		return chooser.getSelectedFile();
	}
	/*
	 * ********************************************************************************
	 * END of "Save to file" handler section
	 * ********************************************************************************
	 */
	
	private void obtainRPCServerIPandPort() {
		
				
		
		RPCServer = new DockingAction("Upload events to RPC server", getName()) {
			@Override
			public void actionPerformed(ActionContext context) {
				//Msg.info(this, "Before call showEventDemo()");
				buildServerInfoPanel();
				//Msg.info(this, "After call showEventDemo()");
			}
		};

		RPCServer.markHelpUnnecessary();
		RPCServer.setEnabled(true);
		//ImageIcon icon = ResourceManager.loadImage("images/network-receive.png");
		ImageIcon icon = UPLOAD_ICON;
		RPCServer.setToolBarData(new ToolBarData(icon));
		addLocalAction(RPCServer);
	}


		   
	private void buildServerInfoPanel() {
		
	    mainFrame = new JFrame("Specify SensorRE RPC Server");
	    mainFrame.setSize(400,300);
	    mainFrame.setLayout(new GridLayout(3, 2));
	    
		JPanel serverInfoPanel = new JPanel(new BorderLayout(10, 10));
		JPanel topPanel = new JPanel(new BorderLayout(10, 10));
		
		JLabel msgBox = new JLabel("Enter RPC server IP/FQDN and port", JLabel.CENTER);
				

		serverInfoComponent = new ServerInfoComponent();
		serverInfoComponent.setServerInfo(serverInfo);
		//serverInfoComponent.setStatusListener();
		serverInfoComponent.setChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				serverInfoChanged();
			}
		});
		
		topPanel.add(serverInfoComponent, BorderLayout.CENTER);

		clearButton = new JButton("CLEAR");
		clearButton.setToolTipText("Clear address & port");
		clearButton.setEnabled(false);
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				serverInfoComponent.clearServerNameField();
				serverInfoComponent.clearPortNumber();
			}
		});
		
		connectButton = new JButton("CONNECT");
		connectButton.setToolTipText("Connect to RPC server");
		//setDefaultButton(saveButton);
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rpcServer = null;
				if (serverInfoComponent.isValidInformation()) {
					Msg.debug(this, "Connect Button clicked, server Address: " + serverName + " and port: " + serverPort);
					serverName = serverInfoComponent.getServerName();
					serverPort = serverInfoComponent.getPortNumber();
					serverInfo = new ServerInfo(serverName, serverPort); 
					rpcServer = ClientUtil.getRepositoryServer(
							serverInfoComponent.getServerName(), serverInfoComponent.getPortNumber(), true);
					
					if (rpcServer != null) {
						Msg.debug(this, "Connected to RPC server at " + serverInfoComponent.getServerName() + ":" + serverInfoComponent.getPortNumber());
					}else {
						Msg.debug(this, "Failed to connect to RPC server: " + serverInfoComponent.getServerName() + ":" + serverInfoComponent.getPortNumber());
						return;
					}
				}else {
					msgBox.setForeground(Color.RED); 
					msgBox.setText("Invalid IP and/or port!");
					Msg.debug(this, "Connect Button clicked with invalid server name and/or port number, server Address: " + serverName + " and port: " + serverPort);
				}

			}
		});
		
		
		cancelButton = new JButton("CANCEL");
		cancelButton.setToolTipText("Cancel and close this window");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainFrame.dispose();
				mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				//mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}
		});
		
		JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonPanel.add(clearButton);
		buttonPanel.add(connectButton);
		buttonPanel.add(cancelButton);

		serverInfoPanel.add(topPanel, BorderLayout.NORTH);

		mainFrame.add(topPanel);
		mainFrame.add(buttonPanel);
		mainFrame.add(msgBox);
		mainFrame.pack();
		mainFrame.setVisible(true);
		
	}
	
	private void serverInfoChanged() {
//		setStatusText("");
//		setOkEnabled(false);
//		listModel.clear();
		//boolean isValidInfo = serverInfoComponent.isValidInformation();
		clearButton.setEnabled(true);
		//clearButton.setEnabled(isValidInfo);
		//Msg.debug(this, "Server address: " + serverInfoComponent.getServerName() + " and port: " + serverInfoComponent.getPortNumber());
	}

	
	void displayEvent(String eventStr) {

		eventList.add(eventStr);
		if (eventList.size() < LIMIT) {
			int caretPos = textArea.getCaretPosition();
			textArea.append(eventStr);
			textArea.setCaretPosition(caretPos + eventStr.length());
		}
		else {
			if (eventList.size() > LIMIT) {
				List<String> list = eventList.subList(100, eventList.size() - 1);
				eventList = new ArrayList<>(list);
			}
			textArea.setText("");
			int length = 0;
			for (int i = 0; i < eventList.size(); i++) {
				String str = eventList.get(i);
				textArea.append(str);
				length += str.length();
			}
			textArea.setCaretPosition(length);
		}
	}
	
	@Override
	public JComponent getComponent() {
		return scrollPane;
	}

	/**
	 * @see docking.ComponentProvider#componentHidden()
	 */
	@Override
	public void componentHidden() {
		clear();
	}

	public Font getFont() {
		return textArea.getFont();
	}

	public void setFont(Font font) {
		textArea.setFont(font);
	}

}