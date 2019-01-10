> Load RunJava JAddin Helloworld
06.01.2019 10:06:28   JVM: Java Virtual Machine initialized.
06.01.2019 10:06:28   RunJava: Started JAddin Java task.
06.01.2019 10:06:28   RunJava JVM: Exception in thread "JAddin"
06.01.2019 10:06:28   RunJava JVM: java.lang.NoClassDefFoundError: Helloworld (wrong name: HelloWorld)
06.01.2019 10:06:28   RunJava JVM:      at java.lang.ClassLoader.defineClassImpl(Native Method)
06.01.2019 10:06:28   RunJava JVM:      at java.lang.ClassLoader.defineClass(ClassLoader.java:379)
06.01.2019 10:06:28   RunJava JVM:      at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:154)
06.01.2019 10:06:28   RunJava JVM:      at java.net.URLClassLoader.defineClass(URLClassLoader.java:729)
06.01.2019 10:06:28   RunJava JVM:      at java.net.URLClassLoader.access$400(URLClassLoader.java:95)
06.01.2019 10:06:28   RunJava JVM:      at java.net.URLClassLoader$ClassFinder.run(URLClassLoader.java:1184)
06.01.2019 10:06:28   RunJava JVM:      at java.security.AccessController.doPrivileged(AccessController.java:732)
06.01.2019 10:06:28   RunJava JVM:      at java.net.URLClassLoader.findClass(URLClassLoader.java:604)
06.01.2019 10:06:28   RunJava JVM:      at java.lang.ClassLoader.loadClassHelper(ClassLoader.java:925)
06.01.2019 10:06:28   RunJava JVM:      at java.lang.ClassLoader.loadClass(ClassLoader.java:870)
06.01.2019 10:06:28   RunJava JVM:      at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:343)
06.01.2019 10:06:28   RunJava JVM:      at java.lang.ClassLoader.loadClass(ClassLoader.java:853)
06.01.2019 10:06:28   RunJava JVM:      at java.lang.Class.forNameImpl(Native Method)
06.01.2019 10:06:28   RunJava JVM:      at java.lang.Class.forName(Class.java:339)
06.01.2019 10:06:28   RunJava JVM:      at JAddin.runNotes(JAddin.java:144)
06.01.2019 10:06:28   RunJava JVM:      at lotus.domino.NotesThread.run(Unknown Source)
06.01.2019 10:06:30   RunJava: Finalized JAddin Java task.
06.01.2019 10:06:31   RunJava shutdown.



> Load RunJava JAddin HelloWorld Debug!
05.01.2019 17:37:42   JVM: Java Virtual Machine initialized.
05.01.2019 17:37:42   RunJava: Started JAddin Java task.
05.01.2019 17:37:42   JAddin: Debugging is enabled - Enter <Tell HelloWorld NoDebug!> to disable
05.01.2019 17:37:42   DEBUG: JAddin.runNotes(102)           JAddin framework version 2.0.0
05.01.2019 17:37:42   DEBUG: JAddin.runNotes(103)           Addin <HelloWorld> will be called with parameter <null>
05.01.2019 17:37:42   DEBUG: JAddin.runNotes(106)           Creating the Domino message queue
05.01.2019 17:37:42   DEBUG: JAddin.runNotes(124)           Opening the Domino message queue
05.01.2019 17:37:42   DEBUG: JAddin.runNotes(142)           Loading the user Java class <HelloWorld>
05.01.2019 17:37:42   DEBUG: JAddin.runNotes(154)           User Java class <HelloWorld> successfully loaded
05.01.2019 17:37:42   DEBUG: JAddin.runNotes(166)           => HelloWorld.addinInitialize()
05.01.2019 17:37:42   DEBUG: HelloWorld.addinInitialize(44) -- Method addinInitialize()
05.01.2019 17:37:42   DEBUG: HelloWorld.addinInitialize(58) Creating the Domino session
05.01.2019 17:37:42   DEBUG: JAddin.runNotes(168)           <= HelloWorld.addinInitialize()
05.01.2019 17:37:42   DEBUG: JAddin.runNotes(179)           => HelloWorld.start()
05.01.2019 17:37:42   DEBUG: JAddin.runNotes(181)           <= HelloWorld.start()
05.01.2019 17:37:42   DEBUG: JAddin.getCommand(453)         -- Method getCommand()
05.01.2019 17:37:42   DEBUG: HelloWorld.runNotes(84)        -- Method runNotes()
05.01.2019 17:37:42   DEBUG: HelloWorld.runNotes(97)        => HelloWorld.addinStart()

> Load RunJava HelloWorld
06.01.2019 12:13:19   JVM: Java Virtual Machine initialized.
06.01.2019 12:13:19   RunJava: Can't find stopAddin method for class HelloWorld.
06.01.2019 12:13:20   RunJava shutdown.

> Load RunJava Jaddin HelloWorld
06.01.2019 12:37:36   JVM: Java Virtual Machine initialized.
06.01.2019 12:37:36   RunJava: Can't find class Jaddin or lotus/notes/addins/jaddin/Jaddin in the classpath.  Class names are case-sensitive.
06.01.2019 12:37:37   RunJava shutdown.


