---
description: Some background information on the framework architecture
---

# ℹ Architecture

## Framework Architecture <a href="#id-1-framework-architecture" id="id-1-framework-architecture"></a>

The JAddin architecture consists of two Java classes which are distributed with your application code. An picture of the architecture can be downloaded here:

{% file src=".gitbook/assets/JAddin Architecture.pdf" %}

### **JAddin.class**

The JAddin class is loaded by the HCL Domino RunJava task as the main Java thread. It executes under the control of RunJava and shares the HCL Domino Java Virtual Machine (JVM) with RunJava.

Main functions of JAddin thread:

* Initialize the JAddin framework
* Dynamically loads and starts the user add-in as a subclass of JAddinThread
* Monitors the Java heap space usage and calls the garbage collector if needed
* Acts on special framework commands (see command `Help!`)
* Supports internal debugging thru `Debug!` command
* Backcalls the user implemented methods `addInXXX()` (see below)

### **JAddinThread.class**

This abstract class must be implemented by the user add-in class. It runs as a separate thread to minimize any delays on the normal processing of the HCL Domino server.

* Initialize the runtime environment
* Calls the user class thru addinStart()
* Includes several methods for accessing HCL Domino objects and the server environment

### **AddinName.class**

The user code runs in this subclass of JAddinThread and does all the processing of the application. Several callback methods are invoked from the framework which can or must be implemented by the user class. When the user class terminates, the framework will perform its cleanup and terminates the JAddin main thread.

| **Method**      | **Required** | **Description**                          |
| --------------- | ------------ | ---------------------------------------- |
| addinStart()    | Yes          | Main entry point of the application code |
| addinCommand()  | No           | Called for any console command entered   |
| addinStop()     | Yes          | Called before termination                |
| addinNextHour() | No           | Called at each new hour                  |
| addinNextDay()  | No           | Called at each new day                   |

