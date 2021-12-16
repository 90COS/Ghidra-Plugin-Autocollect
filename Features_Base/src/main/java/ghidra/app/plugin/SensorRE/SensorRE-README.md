#### Notes: This ia a minimal push at this time as this branch only contains the based code and not all dependencies. Full Ghidra and **EventCollectorForSensorREPlugin** can be clone from: [github](https://github.com/kmai-github/ghidra)
#### Hence, in order to run **EventCollectorForSensorREPlugin.java**, the following steps need to be carried out.


-In Eclipse, create a new package **ghidra.app.plugin.sensorRE** in **Features Base/src/main/java**
-Add **EventCollectorForSensorREPlugin.java** and **EventCollectorForSensorREPluginDockerProvider.java**
-Using Eclipse smart feature to correct any dependecy errors
-Run Ghidra from Eclipse
-If **SensorRE Event Collector Plugin** docker does not show up at the bottom of Ghidra window, then do the following:
    -Go to File > Configure
    -From **Configure Tool** window, under **Developer** row, click on **Configure** link
    -From **Confiure Developer Plugins** windows, check **EventCollectorForSensorREPlugin**, then click **OK**
    -Close the **Configure Tool**
    
