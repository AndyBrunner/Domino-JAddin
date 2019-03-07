[Homepage](README.md) | [Installation](INSTALLATION.md) | [Hints and Tips](HINTS-AND-TIPS.md) | [API Documentation](api/index.html) | [Open Source Code](https://github.com/AndyBrunner/Domino-JAddin) | [Download](DOWNLOAD.md)
### Downloads

**[Version 2.1.1 2019-03-07](JAddin-2.1.1.zip)**

- Change: dbSendMessage now uses mail1.box if mail.box is not present

**[Version 2.1.0 2019-02-03](JAddin-2.1.0.zip)**

- Added static methods JAddin.fromISODateUTC() and JAddin.toISODateUTC()
- Added method generateHash(), encryptAES(), decryptAES(), fromBase64 and toBase64()
- Changed method dbSendMessage() to always create a MIME message
- Changed method dbRecycleObjects() to better support arrays and vectors
- Changed Domino statistic to show date in UTC ISO 8601 format
- JavaDoc changes

**[Version 2.0.0 2019-01-16](JAddin-2.0.0.zip)**

- Major rewrite of JAddin.java and JAddinThread.java
- Now requires JVM 1.8+ (Domino 9.0.1 FP8+)
- Many new and changed methods to support applications
- Complete rewrite of the documentation and publish it on GitHub
- Create project homepage at https://jaddin.abdata.ch

**Version 1.3.0 2016-03-24 (Beta test user only)**
- Change: Replace the Thread termination sequence by Thread.interrupt()
- Change: Mark addInTerminate() as deprecated
- Change: Small change in sendMessage() for processing sender name
- Add: New method waitSeconds() to delay execution
- Add: New method recycleObjects() to free Domino object resources
- Add: New documentation chapter to explain program flow

**[Version 1.2.0 2013-12-18](JAddin-1.2.0.zip)**

- Add: New command "Version!" to display JAddin, Java and OS version numbers.
- Change: Send the low heap memory warning message only once for each threshold reached.

**Version 1.1.0 2012-07-18**

- Add: New methods addinNextHour() and addinNextDay() to allow for notification of next hour and next day.
- Change: Several runtime optimizations.

**Version 1.0.0 2012-04-28**

- Add: First formal version
- Add: New method sendMessage() to create and send a message.

**Version 0.5.0 2010-08-22**

- New: First public beta version.
