[Homepage](README.md) | [API Documentation](api/index.html) | [Download](DOWNLOAD.md) | [Release History](HISTORY.md) | [Open Source Code](https://github.com/AndyBrunner/Domino-JAddin)

### Release History

**Version 2.0.0 xx-Jan-2019**

- Major rewrite of JAddin.java and JAddinThread
- Now requires JVM 1.8+ (Domino 9.0.1 FP8+)
- Added many new methods (dbxxxx) to support Domino applications.
- Added many new methods to support application development
- Complete rewrite of the documentation and publish it on GitHub
- Create jaddin.abdata.ch homepage on GitHub

**Version 1.3.0 24-Mar-2015 (Beta test user only)**
- Change: Replace the Thread termination sequence by Thread.interrupt()
- Change: Mark addInTerminate() as deprecated
- Change: Small change in sendMessage() for processing sender name
- Add: New method waitSeconds() to delay execution
- Add: New method recycleObjects() to free Domino object resources
- Add: New documentation chapter to explain program flow

**Version 1.2.0 18-Dec-2013**

- Add: New command "Version!" to display JAddin, Java and OS version numbers.
- Change: Send the low heap memory warning message only once for each threshold reached.

**Version 1.1.0 18-Jul-2012**

- Add: New methods addinNextHour() and addinNextDay() to allow for notification of next hour and next day.
- Change: Several runtime optimizations.

**Version 1.0.0 28-Apr-2012**

- Add: First formal version
- Add: New method sendMessage() to create and send a message.

**Version 0.5.0 22-Aug-2010**

- New: First public beta version.
