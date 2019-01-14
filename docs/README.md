[Homepage](README.md) | [API Documentation](api/index.html) | [Download](DOWNLOAD.md) | [Release History](HISTORY.md) | [Open Source Code](https://github.com/AndyBrunner/Domino-JAddin)

![Domino](Domino-Icon.png)

Do you need to write an add-in for IBM Domino server in Java?

The JAddin framework is a very thin and convenient layer between the Domino RunJava task and your Java application code. It greatly helps you to create Java server tasks by freeing you to learn all the Domino add-in specifics such as message queue handling, thread creation, communicating with the console, resource cleanup, etc. It is written entirely in Java to support all Domino versions and platforms.

### Prerequisites

- IBM Domino 9.0.1 FP8 or higher (Java Virtual Machine 1.8+ requirement)

### Example

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

### Framework Architecture

#### JAddin.class

The JAddin class is loaded by the Domino RunJava as the main Java thread. It shares the Java Virtual Machine (JVM) with RunJava.

- Initialize the JAddin framework
- Dynamically loads and starts the user add-in (Subclass of JAddinThread)
- Monitors the Java heap space usage and calls the garbage collector if needed
- Acts on special framework commands (see 'Help!')
- Calls user add-in methods, e.g. addinCommand(), addInStop()

#### JAddinThread.class

This abstract class must be implemented by the user add-in class. It runs as a separate thread to minimize any delays on the normal processing of the Domino server.

- Initialize the runtime environment
- Calls the user class thru addinStart()
- Support methods for accessing Domino objects and the server environment

#### AddinName.class

The user code runs in this class and does all the processing of the application. Several methods are called from the framework and can be implemented by the user class. When the user class terminates, the framework will perform its cleanup and terminates the JAddin main thread.

- addinStart() is called run the application code
- addinCommand() is called for any console command enteredä
- addinStop() is called just before termination
- addinNextHour() and addinNextDay() are called at these time intervals
- See the documentation for all other supporting methods

### Application Development 

To be able to use this framework, you must simply add the `JAddin.class`, `JAddinThread.class` and the `notes.jar` file from your IBM Notes or IBM Domino installation to your development environment.

### Application Testing and Distribution

To distribute and install your add-in, you must include a valid `MANIFEST.MF`, the framework files `JAddin.class` and `JAddinThread.class` together with your application code.

#### Create MANIFEST.MF file

Make sure that the file includes an empty line at the bottom.

```text
Manifest-Version: 1.0
Class-Path: .
Main-Class: AddinName
```

#### Create JAR container

There are many tools available to create JAR containers, but the easiest way is to use the command line.

`jar cvmf MANIFEST.MF AddinName.jar AddinName.class JAddin.class JAddinThread.class`

#### Install Application

Copy this JAR container to the `domino/ndext` directory. This directory is automatically searched by the RunJava task for any Java class to load.

#### Run application

There are several ways to start the application:

- Manually by entering the command in the Domino console, e.g. `Load RunJava JAddin AddinName`
- Adding RunJava in the Domino server Notes.Ini, e.g. `ServerTasks=Replica,Router,Update,RunJava JAddin AddinName,AMgr`
- Creating a program document in the Domino Directory:

![Program Document](JAddin-Program-Document.png)

### Console Commands

The framework supports a number of special commands:

```text
> Tell HelloWorld Help!
12.01.2019 18:18:02   JAddin: The following JAddin commands are available:
12.01.2019 18:18:02   JAddin: Version!    Display JAddin, Java and OS version numbers
12.01.2019 18:18:02   JAddin: Quit!       Terminate the add-in thru the JAddin framework
12.01.2019 18:18:02   JAddin: Memory!     Display the Java virtual machine memory usage
12.01.2019 18:18:02   JAddin: GC!         Executes the Java virtual machine garbage collector
12.01.2019 18:18:02   JAddin: Debug!      Enable the debug logging to the console
12.01.2019 18:18:02   JAddin: NoDebug!    Disable the debug logging to the console
12.01.2019 18:18:02   JAddin: Heartbeat!  Manually start heartbeat processing (automatically done every 15 seconds)
12.01.2019 18:18:02   JAddin: Help!       Displays this help text
```

### Domino Statistics

The JAddin framework periodically sets a number of Domino statistic values which are shown with the `Show Stat AddinName' command.

```text
> Show Stat HelloWorld
  HelloWorld.Domino.Version = Release 10.0.1|November 29, 2018
  HelloWorld.JAddin.Date = 14-Jan-2019
  HelloWorld.JAddin.StartTime = Mon Jan 14 10:22:04 CET 2019
  HelloWorld.JAddin.Version = 2.0.0
  HelloWorld.JVM.GCCount = 1
  HelloWorld.JVM.HeapDefinedKB = 131'072
  HelloWorld.JVM.HeapUsedKB = 20'781
  HelloWorld.JVM.Version = 1.8.0_181 (IBM Corporation)
  HelloWorld.OS.Version = 6.2 (Windows 8)
```

### Debugging

For problem determination, you may enable debugging with the special parameter `Debug!`. To stop the debugging, use the parameter `NoDebug!`. While active debugging adds a significant amount of data to the console log and to the log.nsf database, it can be helpful in finding the root of a problem.

`Load RunJava AddinName Squirrel Debug!` | Start your add-in in debug mode
`Tell AddinName Debug!` | Start the debug mode while the add-in is running
`Tell AddinName NoDebug!` | Stop the debug mode while the add-in is running 

The debug output is written to the Domino console and includes the name of the Java method with the source line number issuing the message.

```text
> Load RunJava JAddin HelloWorld Debug!
12.01.2019 11:27:48   JVM: Java Virtual Machine initialized.
12.01.2019 11:27:48   RunJava: Started JAddin Java task.
12.01.2019 11:27:48   JAddin: Debug logging enabled - Enter 'Tell HelloWorld NoDebug!' to disable
12.01.2019 11:27:48   DEBUG: JAddin.runNotes(111)                JAddin framework version 2.0.0
12.01.2019 11:27:48   DEBUG: JAddin.runNotes(112)                Addin HelloWorld will be called with parameters null
12.01.2019 11:27:48   DEBUG: JAddin.runNotes(115)                Creating the Domino message queue
12.01.2019 11:27:48   DEBUG: JAddin.runNotes(133)                Opening the Domino message queue
12.01.2019 11:27:48   DEBUG: JAddin.runNotes(151)                Loading the user Java class HelloWorld
12.01.2019 11:27:48   DEBUG: JAddin.runNotes(163)                User Java class HelloWorld successfully loaded
12.01.2019 11:27:48   DEBUG: JAddin.runNotes(175)                => HelloWorld.addinInitialize()
12.01.2019 11:27:48   DEBUG: HelloWorld.addinInitialize(75)      -- Method addinInitialize()
12.01.2019 11:27:48   DEBUG: HelloWorld.addinInitialize(89)      Creating the Domino session
12.01.2019 11:27:48   DEBUG: JAddin.runNotes(177)                <= HelloWorld.addinInitialize()
12.01.2019 11:27:48   DEBUG: JAddin.runNotes(188)                => HelloWorld.start()
12.01.2019 11:27:48   DEBUG: JAddin.runNotes(190)                <= HelloWorld.start()
12.01.2019 11:27:48   DEBUG: HelloWorld.runNotes(114)            -- Method runNotes()
12.01.2019 11:27:48   DEBUG: HelloWorld.runNotes(127)            => HelloWorld.addinStart()
12.01.2019 11:27:48   HelloWorld: Started with parameters null
12.01.2019 11:27:48   HelloWorld: Running on Release 10.0.1 November 29, 2018
12.01.2019 11:27:48   HelloWorld: User code is executing ...
> Tell HelloWorld q
```

### Frequently Asked Questions

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

### Author

This framework was created to help implementing projects which required the use of Domino server add-ins. If you encounter any issue or if you have a suggestion, please let me know. If you want to share some details of your projects based on JAddin, I will be glad to publish them here. You may contact me thru my [email address](mailto:andy.brunner@abdata.ch).

### Unlicense (see [unlicense.org](http://unlicense.org))

_This software shall be used for Good, not Evil._

***

*Created with love and passion in the beautiful country of Switzerland. As far as I know, no animal was harmed in the making of this program.*
