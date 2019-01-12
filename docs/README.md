[Homepage](README.md) | [API Documentation](api/index.html) | [Download](DOWNLOAD.md) | [Release History](HISTORY.md) | [Open Source Code](https://github.com/AndyBrunner/Domino-JAddin)

![Domino](Domino-Icon.png)

## Java Toolkit for IBM Domino Server Add-in

Do you need to write an add-in for IBM Domino server in Java?

The JAddin framework is a very thin and convenient layer between the Domino RunJava task and your Java application code. It greatly helps you to create Java server tasks by freeing you to learn all the Domino add-in specifics such as message queue handling, thread creation, communicating with the console, resource cleanup, etc. It is written entirely in Java to support all Domino versions and platforms.

### Prerequisites

- IBM Domino 9.0.1 FP8 or higher (JVM 1.8 requirement)

### Installation

- The framework consists of the two Java class files JAddin.class and JAddinThread.class. To install the framework (and your application) you need to copy all necessary Java class files to the program directory on Windows or to the data directory on Linux.
- Alternatively you can combine all class files in one JAR container and add this JAR file name to the Domino Notes.Ini parameter, e.g. `JavaUserClasses=C:\Lotus\Domino\traveler.jar;C:\Apps\AddinName.jar`
- If you update any Java class file on the Domino server, you first need to stop the RunJava Domino task to free the class cache used by RunJava.

### Example

```java
public class HelloWorld extends JAddinThread {

	public void addinStart() {
		
		logMessage("Started with parameters <" + getAddinParameters() + '>');
		
		try {
			logMessage("Running on " + dbGetSession().getNotesVersion());
		} catch (Exception e) {
			logMessage("Unable to get Domino version: " + e.getMessage());
		}

		// Main add-in loop ...
		while (true) {
			logMessage("User code is executing ...");
			waitMilliSeconds(15000L);
		}
	}

	// This method is called asynchronously by the JAddin framework when the command 'Quit' or 'Exit' is entered or at Domino
	// server shutdown. Here you may signal the addinStart() method to terminate and to perform any cleanup.
	public void addinStop() {
		logMessage("Termination in progress");
	}
	
	// This method is called asynchronously by the JAddin framework for any console command entered. It should be executed as
	// quickly as possible to avoid any main Domino message queue delays.
	public void addinCommand(String command) {
		logMessage("You have entered the command " + command);
	}
}
```

### Starting the Application

There are several ways to start the application:

- Manually by entering the command in the Domino console, e.g. `Load RunJava JAddin AddinName`
- Adding RunJava in the Domino server Notes.Ini, e.g. `ServerTasks=Replica,Router,Update,RunJava JAddin AddinName,AMgr`
- Creating a program document in the Domino Directory:

Program name: | "RunJava"
Command line:  | "JAddin AddinName"
Server to run on: | Your Server Name, e.g. "Server/ACME"
Enabled/disabled: | At server startup only

The JAddin may be started with the special parameter «Debug!» to activate the debugging during startup, e.g. `Load RunJava JAddin AddinName Debug!`

### Console Commands

Several Domino console commands are supported:

**Command** | **Description**
Version!	 | Display the JAddin, Java and OS version numbers
Quit! | 	Terminate the add-in thru the JAddin framework
Memory! | Display the Java virtual machine memory usage
GC! | Executes the Java virtual machine garbage collector
Debug! | Enable the debug logging to the console
NoDebug!	 | Disable the debug logging to the console
HeartBeat! | Manually start heartbeat processing (automatically done every 15 seconds)
Help! | Displays this help text

### Frequently Asked Questions

- Q: How do I develop my JAddin project in Eclipse?
- A: Make sure that you include the JAddin framework JAR files and the notes.jar file (installed with Notes and Domino) as external files in your project.

- Q: During startup, I see the error message `RunJava: Can't find class JAddIn or lotus/notes/addins/jaddin/JAddIn in the classpath. Class names are case-sensitive.`
- A: Make sure that the class names in the "Load RunJava" are entered with exact upper and lower case.

- Q: During startup, I see the error message `JAddin: Error: Unable to load the Java class AddinName.`
- A: The JAddin framework tried to load the Java class AddinName but the specified name could not be found. Make sure that the class file AddinName.class is found in the Domino program directory (Windows) or in the Domino data directory (Linux).

- Q: During startup, I see the error message `RunJava: Can't find stopAddin method for class AddinName.`
- A: Make sure you start your add-in thru JAddin with the command `Tell RunJava JAddin AddinName` and not directly thru RunJava.

- Q: I see out-of-memory errors in Java while executing my add-in.
- A: All Java add-ins execute in a single JVM under the control of RunJava. The Domino Notes.Ini parameter `JavaMaxHeapSize=xxxxMB` may be used to increase the heap space.

- Q: What is the heartbeart in JAddin?
- A: The main thread in JAddin gets triggered every 15 seconds to perform some internal housekeeping checks. One of these checks makes sure that the Java heap space does not get filled up to avoid out-of-memory errors in Java. If the free space falls below 10 percent, the Java virtual machine garbage collector is invoked and a message is written to the console.

### Author

This framework was created to help implementing projects which required the use of Domino server add-ins. If you encounter any issue or if you have a suggestion, please let me know. If you want to share some details of your projects based on JAddin, I will be glad to publish them here. You may contact me thru my [email address](mailto:andy.brunner@k43.ch).

### Unlicense (see [unlicense.org](http://unlicense.org))

_This software shall be used for Good, not Evil._

***

*Created with love and passion in the beautiful country of Switzerland. As far as I know, no animal was harmed in the making of this program.*

