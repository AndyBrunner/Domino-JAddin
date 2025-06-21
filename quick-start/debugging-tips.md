---
description: Hints and tips on the usage of the JAddin framework
layout:
  title:
    visible: true
  description:
    visible: true
  tableOfContents:
    visible: true
  outline:
    visible: false
  pagination:
    visible: true
---

# 🐞 Debugging Tips

## Common Error Messages

| Error Message                                                                                                               | Possible Reason                                                                                                                                                             |
| --------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `RunJava: Can't find class AddinName lotus/notes/addins/jaddin/AddinName in the classpath. Class names are case-sensitive.` | The RunJava task was unable to load the class. Make sure that it is written with exact upper and lower case characters and it can be found by the RunJava class loader      |
| `JAddin: Unable to load Java class AddinName`                                                                               | The JAddin framework was unable to load the application class. Make sure that it is written with exact upper and lower case characters.                                     |
| `RunJava: Can't find stopAddin method for class AddinName.`                                                                 | The application class must be loaded thru the JAddin framework and not directly from RunJava. Use the command `Load RunJava JAddin AddinName` to start your application.    |
| `RunJava JVM: java.lang.NoClassDefFoundError: AddinName (wrong name: addinname)`                                            | The user class name in the command and the internal name do not match. Most likely you have not typed the name with correct upper and lower case characters.                |
| `Out of memory`                                                                                                             | All Java add-ins execute in a single Java Virtual Machine (JVM) in RunJava. The Domino Notes.Ini parameter `JavaMaxHeapSize=xxxxMB` may be used to increase the heap space. |

## HCL Domino Statistics <a href="#id-3-domino-statistics" id="id-3-domino-statistics"></a>

During execution, the JAddin maintains statistics and status information. They can be displayed with the `Show Stat AddinName` command:

```
> Show Stat HelloWorld
  HelloWorld.Domino.Platform = 6.2 (Windows 8)
  HelloWorld.Domino.Version = Release 14.5|June 06, 2025 (Windows/64)
  HelloWorld.JAddin.StartedTime = 2025-06-21T12:03:39Z
  HelloWorld.JAddin.VersionDate = 2025-06-21
  HelloWorld.JAddin.VersionNumber = 2.2.1
  HelloWorld.JVM.HeapLimitKB = 262'144
  HelloWorld.JVM.HeapUsedKB = 20'775
  HelloWorld.JVM.Version = 21.0.6 (IBM Corporation)
  8 statistics found
```

## Console Help Command

The framework supports a number of special commands:

```
> Tell HelloWorld Help!
21.06.2025 14:04:17   JAddin: Quit!       Terminate the add-in thru the framework
21.06.2025 14:04:17   JAddin: Debug!      Enable the debug logging to the console
21.06.2025 14:04:17   JAddin: NoDebug!    Disable the debug logging to the console
21.06.2025 14:04:17   JAddin: Heartbeat!  Manually start heartbeat processing (automatically done every 15 seconds)
21.06.2025 14:04:17   JAddin: Help!       Displays this help text
```

## Debugging

For a detailed problem determination, you may use the built-in debugging features.

### Enable/Disable Debug

| Domino Console Command                 | Description                                      |
| -------------------------------------- | ------------------------------------------------ |
| `Load RunJava JAddin AddinName Debug!` | Start add-in in debug mode                       |
| `Tell AddinName Debug!`                | Start the debug mode while the add-in is running |
| `Tell AddinName NoDebug!`              | Stop the debug mode while the add-in is running  |

{% hint style="warning" %}
While active debugging adds a significant amount of data to the console log and to the log.nsf database, it can be helpful in finding the root of a problem.&#x20;
{% endhint %}

### Debug Output

The debug output is written to the HCL Domino console and includes the name of the Java method with the source line number issuing the message.

```
> Load RunJava JAddin HelloWorld Debug!
21.06.2025 14:05:01   JVM: Java Virtual Machine initialized.
21.06.2025 14:05:01   RunJava: Started JAddin Java task.
21.06.2025 14:05:01   JAddin: Enter 'Tell HelloWorld NoDebug!' to disable debug logging
21.06.2025 14:05:01   JAddin: DEBUG: JAddin.runNotes(430)                     JAddin framework version 2.2.1 / 2025-06-21
21.06.2025 14:05:01   JAddin: DEBUG: JAddin.runNotes(431)                     OS platform: 6.2 (Windows 8)
21.06.2025 14:05:01   JAddin: DEBUG: JAddin.runNotes(432)                     JVM version: 21.0.6 (IBM Corporation)
21.06.2025 14:05:01   JAddin: DEBUG: JAddin.runNotes(433)                     HelloWorld will be called with parameter: null
21.06.2025 14:05:01   JAddin: DEBUG: JAddin.runNotes(436)                     Creating and opening the Domino message queue
21.06.2025 14:05:01   JAddin: DEBUG: JAddin.runNotes(470)                     Loading Java class HelloWorld
21.06.2025 14:05:01   JAddin: DEBUG: JAddin.runNotes(491)                     Calling HelloWorld.addinInitialize()
21.06.2025 14:05:01   HelloWorld: DEBUG: HelloWorld.addinInitialize(108)      Entered addinInitialize()
21.06.2025 14:05:01   HelloWorld: DEBUG: HelloWorld.addinInitialize(127)      Domino version: Release 14.5 June 06, 2025 (Windows/64)
21.06.2025 14:05:01   JAddin: DEBUG: JAddin.runNotes(501)                     Calling HelloWorld.start()
21.06.2025 14:05:01   HelloWorld: DEBUG: HelloWorld.runNotes(875)             Entered runNotes()
21.06.2025 14:05:01   HelloWorld: DEBUG: HelloWorld.runNotes(888)             Calling HelloWorld.addinStart()
21.06.2025 14:05:01   HelloWorld: Started
21.06.2025 14:05:01   HelloWorld: User code is executing...
21.06.2025 14:05:06   HelloWorld: User code is executing...
21.06.2025 14:05:11   HelloWorld: User code is executing...
> Tell HelloWorld Quit
21.06.2025 14:05:15   JAddin: DEBUG: JAddin.getCommand(236)                   Termination in progress
21.06.2025 14:05:15   JAddin: DEBUG: JAddin.runNotes(539)                     JAddin termination in progress
21.06.2025 14:05:15   JAddin: DEBUG: JAddin.runNotes(544)                     Calling HelloWorld.addinStop()
21.06.2025 14:05:15   HelloWorld: Termination in progress
21.06.2025 14:05:16   HelloWorld: Terminated
21.06.2025 14:05:16   HelloWorld: DEBUG: HelloWorld.addinCleanup(62)          Entered addinCleanup()
21.06.2025 14:05:16   JAddin: DEBUG: JAddin.sendQuitCommand(673)              Sending Quit command to Domino message queue
21.06.2025 14:05:16   JAddin: DEBUG: JAddin.waitForThreadStop(765)            HelloWorld has terminated
21.06.2025 14:05:16   JAddin: DEBUG: JAddin.addinCleanup(123)                 Entered addinCleanup()
21.06.2025 14:05:16   JAddin: DEBUG: JAddin.waitForThreadStop(765)            HelloWorld has terminated
21.06.2025 14:05:16   JAddin: DEBUG: JAddin.addinCleanup(140)                 Freeing the Domino resources
21.06.2025 14:05:17   RunJava: Finalized JAddin Java task.
21.06.2025 14:05:18   RunJava shutdown.
```

## Frequently Asked Questions <a href="#id-6-frequently-asked-questions" id="id-6-frequently-asked-questions"></a>

**Q: How do I develop my JAddin project in Eclipse?**\
A: Ensure that you include the two JAddin framework class files and the notes.jar file (installed with HCL Notes or HCL Domino) as external libraries in your Eclipse project.

**Q: What is the heartbeat in JAddin?**\
A: The main thread in JAddin is triggered every 15 seconds to perform internal housekeeping tasks. One of these tasks monitors Java heap usage to help prevent out-of-memory errors. It also checks whether the user thread has terminated unexpectedly.

**Q: I have copied a new version of my add-in to the server, but it does not become active during application startup. Why?**\
A: The RunJava task caches Java classes in memory. To reload your updated class file, you must terminate all other RunJava tasks—effectively stopping RunJava itself—before restarting it.
