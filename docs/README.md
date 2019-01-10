[Homepage](README.md) | [API Documentation](api/index.html) | [Download](DOWNLOAD.md) | [Release History](HISTORY.md) | [Open Source Code](https://github.com/AndyBrunner/Domino-JAddin)

![Domino](Domino-Icon.png)

## Java Toolkit for IBM Domino server add-in

Do you need to write an add-in for IBM Domino server in Java?

This JAddin framework is a very thin and convenient layer between the Domino RunJava task and your Java application code. It greatly helps you to create Java server tasks by freeing you to learn all the Domino add-in specifics such as message queue handling, thread creation, communicating with the console, resource cleanup, etc. It is written entirely in Java to support all Domino versions and platforms.

### Installation

- The framework consists of the two Java class files JAddin.class and JAddinThread.class. To install the framework (and your application) you need to copy all necessary Java class files to the pro-gram directory (Windows) or to the data directory (Linux) on your Domino server.
- Alternatively you can combine all class files in one JAR container and add this JAR file name to the Domino Notes.Ini parameter, e.g. `JavaUserClasses=C:\Lotus\Domino\traveler.jar;C:\Apps\Application.jar`
- If you update any Java class file on the Domino server, you first need to stop the RunJava Domino task to free the class cache used by RunJava.

### Starting the Application

There are several ways to start the application:

- Manually by entering the command in the Domino console, e.g. `Load RunJava JAddin <AddinName>`
- Adding RunJava in the ServerTasks line in the Domino server Notes.Ini, e.g. `ServerTasks=Replica,Router,Update,RunJava JAddin HelloWorld,AMgr`
- Creating a program document in the Domino Directory:

Program name: | RunJava
Command line:  |JAddin <AddinName>
Server to run on: | Server/ACME
Enabled/disabled: | At server startup only

The JAddin may be started with the special parameter «Debug!» to activate the debugging during startup, e.g. `Load RunJava JAddin <AddinName> Debug!`

### Console Commands

There are several Domino coonsole commands available:

**Command** | **Description**
Version!	 | Display the JAddin, Java and OS version numbers
Debug! | Turns the debugging on (see logDebug() method)
NoDebug!	 | Turns the debugging off (see logDebug() method)
Quit! | 	Terminate the addin (a bit faster than the regular "Tell <AddinName> Quit")
HeartBeat! | Execute the heartbeat processing (normally done every minute)
Memory! | Display the Java virtual machine memory usage
GC! | Runs the Java virtual machine garbage collector

### Frequently Asked Questions

- Q: How do I develop my JAddin project in Eclipse?
- A: Just make sure that you include the notes.jar file (installed with Notes and Domino) as an ex-ternal JAR file in your project.

- Q: During startup, I see the error message `RunJava: Can't find class JAddIn or lotus/notes/addins/jaddin/JAddIn in the classpath. Class names are case-sensitive.`
- A: Make sure that the class names in the "Load RunJava" are case-correctly entered.

- Q: During startup, I see the error message `JAddin: Error: Unable to load the Java class xxxxxx.`
- A: The JAddin framework tried to load the Java class xxxxxx but the specified name could not be found. Make sure that the class file xxxxxx.class is found in the Domino program directory (Win-dows) or in the Domino data directory (Linux) and that the upper- and lowercase characters match exactly since the Java class loader is case sensitive.

- Q: During startup, I see the error message `RunJava: Can't find stopAddin method for class xxxxxx.`
- A: Make sure you start your addin thru JAddin with the command Tell RunJava JAddin xxxxxx and not directly thru RunJava.

- Q: I see out-of-memory errors in Java while executing my addin.
- A: Since all Java addins execute in a single JVM under the control of RunJava, the default Java heap space may not be large enough. The Domino Notes.Ini parameter JavaMaxHeapSize=xxxMB (de-fault 64MB) may be used to increase the heap space.

- Q: What is the heartbeart in JAddin?
- A: The main thread in JAddin gets triggered every 15 seconds to perform some internal house-keeping checks. One of these checks is to make sure that the Java heap space does not get filled up to avoid out-of-memory errors in Java. If the free space falls below 10 percent, the Java virtual machine garbage collector is invoked and a message is written to the console.

### Author

This project was created to help building server add-ins. If you encounter any issues or if you have a suggestion, please let me know. If you want to share your projects based on JAddin, I will be glad to publish them here. You may contact me thru my [email address](mailto:andy.brunner@k43.ch).

### Freeware License

"This software shall be used for Good, not Evil."

*Created with love and passion in the beautiful country of Switzerland. As far as I know, no animal was harmed in the making of this program. This software shall be used for Good, not Evil.*

---
