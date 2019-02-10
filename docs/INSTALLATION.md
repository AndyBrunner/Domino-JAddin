[Homepage](README.md) | [Installation](INSTALLATION.md) | [Hints and Tips](HINTS-AND-TIPS.md) | [API Documentation](api/index.html) | [Open Source Code](https://github.com/AndyBrunner/Domino-JAddin) | [Download](DOWNLOAD.md)

### 1. Prerequisites

- IBM Domino 9.0.1 FP8 or higher (Java Virtual Machine 1.8+ requirement)

### 2. Installation

- [Download](DOWNLOAD.md) and unzip the installation package.
- Copy the `JAddin.class` and `JAddinThread.class` from the installation package to your development environment.
- Copy the `notes.jar` file from your IBM Notes or IBM Domino installation to your development environment.

### 3. Application Distribution 

To distribute and install your add-in, you must create a JAR container which includes a `MANIFEST.MF` file and the framework files `JAddin.class` and `JAddinThread.class`.

**Create MANIFEST.MF file**

Make sure that the last line is terminated with a newline character.

```text
Manifest-Version: 1.0
Class-Path: .
Main-Class: AddinName
```

**Create JAR container**

There are many tools available to create JAR containers. The easiest way is to use the command line.

`jar cvmf MANIFEST.MF AddinName.jar AddinName.class JAddin.class JAddinThread.class`

**Install Application**

Copy the JAR container to the `domino/ndext` directory. This directory is automatically searched by the RunJava task for any Java classes to load.

### 4. Run Application

There are several ways to start the application:

**Console Command**

Enter the command `Load RunJava JAddin AddinName` in the IBM Domino console.

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

**Notes.ini**

You may change the line starting with `ServerTask=` to include the task to be started, e.g.

`ServerTasks=Replica,Router,Update,RunJava JAddin AddinName,AMgr,...`

**Program Document**

The easiest and recommended way is to add a program document in the IBM Domino directory.

![Program Document](JAddin-Program-Document.png)

