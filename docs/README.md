[Homepage](README.md) | [API Documentation](api/index.html) | [Download](DOWNLOAD.md) | [Open Source Code](https://github.com/AndyBrunner/Domino-JAddin)

![Domino](Domino-Icon.png) ![Java](Java-Icon.png)  

_Do you need to write an add-in for IBM Domino server in Java?_

The JAddin framework is a thin and easy to use layer between the Domino RunJava task and your Java application code. It helps you to create Java server tasks by freeing you to learn all the Domino add-in specifics, such as message queue handling, thread creation, communication with the console, resource cleanup, etc. It is written entirely in Java to support all Domino versions and platforms.

**Code Example**

```java
public class HelloWorld extends JAddinThread {

	// This is the main entry point. When this method returns, the add-in terminates.
	public void addinStart() {
		
		logMessage("Started with parameters " + getAddinParameters());
		
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

	// This method is called asynchronously by the JAddin framework when the
	// command 'Quit' or 'Exit' is entered or at Domino server shutdown. Here
	// you may signal the addinStart() method to terminate and to perform any cleanup.
	public void addinStop() {
		logMessage("Termination in progress");
	}
	
	// This method is called asynchronously by the JAddin framework for any
	// console command entered. It should be executed as quickly as possible
	// to avoid any main Domino message queue delays.
	public void addinCommand(String command) {
		logMessage("You have entered the command " + command);
	}
}
```

**Domino Console**

```text
> Load RunJava JAddin HelloWorld
03.02.2019 09:31:47   JVM: Java Virtual Machine initialized.
03.02.2019 09:31:47   RunJava: Started JAddin Java task.
03.02.2019 09:31:47   HelloWorld: Started with parameters null
03.02.2019 09:31:47   HelloWorld: Running on Release 10.0.1 November 29, 2018
03.02.2019 09:31:47   HelloWorld: User code is executing...
03.02.2019 09:32:02   HelloWorld: User code is executing...
03.02.2019 09:32:17   HelloWorld: User code is executing...
03.02.2019 09:32:32   HelloWorld: User code is executing...
```

### 1. Installation

**Prerequisites**

- IBM Domino 9.0.1 FP8 or higher (Java Virtual Machine 1.8+ requirement)

**Installation**

- [Download](DOWNLOAD.md) and unzip the installation package.
- Copy the `JAddin.class` and `JAddinThread.class` from the installation package to your development environment.
- Copy the `notes.jar` file from your IBM Notes or IBM Domino installation to your development environment.

### 2. Application Distribution 

To distribute and install your add-in as a JAR file container, you must include a valid `MANIFEST.MF`, the framework files `JAddin.class` and `JAddinThread.class` together with your application code.

**Create MANIFEST.MF file**

Make sure that the file includes an empty line at the bottom.

```text
Manifest-Version: 1.0
Class-Path: .
Main-Class: AddinName
```

**Create JAR container**

There are many tools available to create JAR containers, but the easiest way is to use the command line.

`jar cvmf MANIFEST.MF AddinName.jar AddinName.class JAddin.class JAddinThread.class`

**Install Application**

Copy this JAR container to the `domino/ndext` directory. This directory is automatically searched by the RunJava task for any Java class to load.

**Run application**

There are several ways to start the application:

- Manually by entering the command in the Domino console, e.g. `Load RunJava JAddin AddinName`
- Adding RunJava in the Domino server Notes.Ini, e.g. `ServerTasks=Replica,Router,Update,RunJava JAddin AddinName,AMgr`
- Creating a program document in the Domino Directory:

![Program Document](JAddin-Program-Document.png)

### 3. Framework Architecture

**JAddin.class**

The JAddin class is loaded by the Domino RunJava task as the main Java thread. It executes under the control of RunJava and
shares the Java Virtual Machine (JVM) with RunJava.

- Initialize the JAddin framework
- Dynamically loads and starts the user add-in as a subclass of JAddinThread
- Monitors the Java heap space usage and calls the garbage collector if needed
- Acts on special framework commands (see command `Help!`)
- Calls user add-in methods, e.g. addinCommand(), addInStop(), addinNextHour(), etc.

**JAddinThread.class**

This is the abstract class which must be implemented by a user add-in class. It runs as a separate thread to minimize any delays on the normal processing of the Domino server.

- Initialize the runtime environment
- Calls the user class thru addinStart()
- Several support methods for accessing Domino objects and the server environment

**AddinName.class**

The user code runs in this class and does all the processing of the application. Several methods are called from the framework and may be implemented by the user class. When the user class terminates, the framework will perform its cleanup and terminates the JAddin main thread.

- addinStart() is called to run the application code
- addinCommand() is called for any console command entered
- addinStop() is called just before termination
- addinNextHour() and addinNextDay() are called at these time intervals
- See the documentation for all other supporting methods

### 4. Console Commands

The framework supports a number of special commands:

```text
> Tell HelloWorld Help!
03.02.2019 09:34:28   JAddin: Quit!       Terminate the add-in thru the framework
03.02.2019 09:34:28   JAddin: GC!         Executes the Java Virtual Machine garbage collector
03.02.2019 09:34:28   JAddin: Debug!      Enable the debug logging to the console
03.02.2019 09:34:28   JAddin: NoDebug!    Disable the debug logging to the console
03.02.2019 09:34:28   JAddin: Heartbeat!  Manually start heartbeat processing (automatically done every 15 seconds)
03.02.2019 09:34:28   JAddin: Help!       Displays this help text
```

### 5. Domino Statistics

The JAddin framework sets and maintains a number of Domino statistic which are shown with the `Show Stat AddinName` command.

```text
> Show Stat HelloWorld
  HelloWorld.Domino.Version = Release 10.0.1|November 29, 2018 (Windows/64)
  HelloWorld.JAddin.StartedTime = 2019-02-03T08:31:47Z
  HelloWorld.JAddin.VersionDate = 2019-02-03
  HelloWorld.JAddin.VersionNumber = 2.1.0
  HelloWorld.JVM.GCCount = 0
  HelloWorld.JVM.HeapLimitKB = 131'072
  HelloWorld.JVM.HeapUsedKB = 20'489
  HelloWorld.JVM.Version = 1.8.0_181 (IBM Corporation)
  HelloWorld.OS.Version = 6.2 (Windows 8)
```

### 6. Debugging

For problem determination, you may enable debugging with the special parameter `Debug!`. While active debugging adds a significant amount of data to the console log and to the log.nsf database, it can be helpful in finding the root of a problem.

`Load RunJava JAddin AddinName Debug!` | Start your add-in in debug mode
`Tell AddinName Debug!` | Enable debug mode while the add-in is running
`Tell AddinName NoDebug!` | Disable the debug mode while the add-in is running 

The debug output is written to the Domino console and includes the name of the Java method with the source line number issuing the message.

```text
> Load RunJava JAddin HelloWorld Debug!
03.02.2019 09:36:12   JVM: Java Virtual Machine initialized.
03.02.2019 09:36:12   RunJava: Started JAddin Java task.
03.02.2019 09:36:12   JAddin: Debug logging enabled - Enter 'Tell HelloWorld NoDebug!' to disable
03.02.2019 09:36:12   DEBUG: JAddin.runNotes(147)                JAddin framework version 2.1.0
03.02.2019 09:36:12   DEBUG: JAddin.runNotes(148)                HelloWorld will be called with parameters null
03.02.2019 09:36:12   DEBUG: JAddin.runNotes(151)                Creating the Domino message queue
03.02.2019 09:36:12   DEBUG: JAddin.runNotes(169)                Opening the Domino message queue
03.02.2019 09:36:12   DEBUG: JAddin.runNotes(187)                Loading the user Java class HelloWorld
03.02.2019 09:36:12   DEBUG: JAddin.runNotes(199)                User Java class HelloWorld successfully loaded
03.02.2019 09:36:12   DEBUG: JAddin.runNotes(211)                => HelloWorld.addinInitialize()
03.02.2019 09:36:12   DEBUG: HelloWorld.addinInitialize(80)      -- addinInitialize()
03.02.2019 09:36:12   DEBUG: HelloWorld.addinInitialize(94)      Creating the Domino session
03.02.2019 09:36:12   DEBUG: JAddin.runNotes(213)                <= HelloWorld.addinInitialize()
03.02.2019 09:36:12   DEBUG: JAddin.runNotes(224)                => HelloWorld.start()
03.02.2019 09:36:12   DEBUG: JAddin.runNotes(226)                <= HelloWorld.start()
03.02.2019 09:36:12   DEBUG: HelloWorld.runNotes(117)            -- runNotes()
03.02.2019 09:36:12   DEBUG: HelloWorld.runNotes(130)            => HelloWorld.addinStart()
03.02.2019 09:36:12   HelloWorld: Started with parameters null
03.02.2019 09:36:12   HelloWorld: Running on Release 10.0.1 November 29, 2018
03.02.2019 09:36:12   HelloWorld: User code is executing...
> Tell HelloWorld Q
03.02.2019 09:36:27   HelloWorld: User code is executing...
03.02.2019 09:36:27   DEBUG: JAddin.getCommand(622)              User entered Quit, Exit or Domino shutdown is in progress
03.02.2019 09:36:27   DEBUG: JAddin.runNotes(273)                JAddin termination in progress
03.02.2019 09:36:27   DEBUG: JAddin.runNotes(277)                => HelloWorld.addinStop()
03.02.2019 09:36:27   HelloWorld: Termination in progress
03.02.2019 09:36:27   DEBUG: JAddin.runNotes(279)                <= HelloWorld.addinStop()
03.02.2019 09:36:27   DEBUG: JAddin.runNotes(290)                => JAddinThread.addinTerminate()
03.02.2019 09:36:27   DEBUG: HelloWorld.addinTerminate(158)      -- addinTerminate()
03.02.2019 09:36:27   DEBUG: HelloWorld.addinCleanup(190)        -- addinCleanup()
03.02.2019 09:36:27   DEBUG: JAddin.sendQuitCommand(649)         Sending Quit command to Domino message queue
03.02.2019 09:36:27   DEBUG: JAddin.runNotes(292)                <= JAddinThread.addinTerminate()
03.02.2019 09:36:27   DEBUG: JAddin.runNotes(303)                Sending interrupt to HelloWorld
03.02.2019 09:36:27   DEBUG: JAddin.runNotes(308)                Waiting for HelloWorld termination
03.02.2019 09:36:27   DEBUG: JAddin.runNotes(313)                HelloWorld has terminated
03.02.2019 09:36:27   DEBUG: JAddin.jAddinCleanup(751)           -- jAddinCleanup()
03.02.2019 09:36:27   DEBUG: JAddin.jAddinCleanup(775)           Freeing the Domino resources
03.02.2019 09:36:28   DEBUG: JAddin.finalize(798)                -- finalize()
03.02.2019 09:36:28   RunJava: Finalized JAddin Java task.
03.02.2019 09:36:29   RunJava shutdown.
```

### 7. Frequently Asked Questions

- Q: How do I develop my JAddin project in Eclipse?
- A: Make sure you include the two JAddin framework class files and the notes.jar file (installed with Notes and Domino) as external files in your project.

- Q: During startup, I see the error message `RunJava: Can't find class JAddIn or lotus/notes/addins/jaddin/AddinName in the classpath.  Class names are case-sensitive.`
- A: The RunJava task was unable to load the class. Make sure that it is written with exact upper and lower case characters.

- Q: During startup, I see the error message `JAddin: Unable to load Java class AddinName`
- A: The JAddin framework was unable to load the user class. Make sure that it is written with exact upper and lower case characters.

- Q: During startup, I see the error message `RunJava: Can't find stopAddin method for class AddinName.`
- A: The user class must be loaded thru the JAddin framework and not directly from RunJava. Use the command `Load RunJava JAddin AddinName` to start the user class.

- Q: During startup, I see the error message `RunJava JVM: java.lang.NoClassDefFoundError: Addinname (wrong name: AddinName)`
- A: The user class name in the command and the internal name do not match. Most likely you have not typed the name with correct upper and lower case characters.

- Q: I see out-of-memory errors in Java while executing my add-in.
- A: All Java add-ins execute in a single Java Virtual Machine (JVM) in RunJava. The Domino Notes.Ini parameter `JavaMaxHeapSize=xxxxMB` may be used to increase the heap space.

- Q: What is the heartbeart in JAddin?
- A: The main thread in JAddin gets triggered every 15 seconds to perform some internal housekeeping tasks. One of these checks makes sure that the Java heap space does not get filled up to avoid out-of-memory errors. If the free space falls below 10 percent, the Java virtual machine garbage collector is invoked and a message is written to the console.

- Q: I have copied a new version of my add-in to the server, but it does not get active during application startup.
- A: The RunJava task caches the Java classes in use. You must terminate all other RunJava tasks - and therefore terminate RunJava itself - to be able to force the reloading of your class file. 

### 8. Author

This framework was created to help implementing projects which required the use of Domino server add-ins. If you encounter any issue or if you have a suggestion, please let me know. If you want to share some details of your projects based on JAddin, I will be glad to publish them here. You may contact me thru my email address [andy.brunner@abdata.ch](mailto:andy.brunner@abdata.ch).

### 9. Unlicense (see [unlicense.org](http://unlicense.org))

_This software shall be used for Good, not Evil._

***

*Created with love and passion in the beautiful country of Switzerland.*

*As far as I know, no animal was harmed in the making of this software :)*
