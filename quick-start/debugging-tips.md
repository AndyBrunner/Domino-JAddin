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
  HelloWorld.Domino.Version = Release 10.0.1|November 29, 2018 (Windows/64)
  HelloWorld.Domino.Platform = 6.2 (Windows 8)  
  HelloWorld.JAddin.StartedTime = 2019-02-03T08:31:47Z
  HelloWorld.JAddin.VersionDate = 2019-02-03
  HelloWorld.JAddin.VersionNumber = 2.1.0
  HelloWorld.JVM.HeapLimitKB = 131'072
  HelloWorld.JVM.HeapUsedKB = 20'489
  HelloWorld.JVM.Version = 1.8.0_181 (IBM Corporation)
```

## Console Help Command

The framework supports a number of special commands:

```
> Tell HelloWorld Help!
03.02.2019 09:34:28   JAddin: Quit!       Terminate the add-in thru the framework
03.02.2019 09:34:28   JAddin: Debug!      Enable the debug logging to the console
03.02.2019 09:34:28   JAddin: NoDebug!    Disable the debug logging to the console
03.02.2019 09:34:28   JAddin: Heartbeat!  Manually start heartbeat processing (automatically done every 15 seconds)
03.02.2019 09:34:28   JAddin: Help!       Displays this help text
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

## Frequently Asked Questions <a href="#id-6-frequently-asked-questions" id="id-6-frequently-asked-questions"></a>

**Q: How do I develop my JAddin project in Eclipse?**\
A: Ensure that you include the two JAddin framework class files and the notes.jar file (installed with HCL Notes or HCL Domino) as external libraries in your Eclipse project.

**Q: What is the heartbeat in JAddin?**\
A: The main thread in JAddin is triggered every 15 seconds to perform internal housekeeping tasks. One of these tasks monitors Java heap usage to help prevent out-of-memory errors. It also checks whether the user thread has terminated unexpectedly.

**Q: I have copied a new version of my add-in to the server, but it does not become active during application startup. Why?**\
A: The RunJava task caches Java classes in memory. To reload your updated class file, you must terminate all other RunJava tasks—effectively stopping RunJava itself—before restarting it.
