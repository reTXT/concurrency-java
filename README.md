Advanced Java Concurrency Primitives
---
[![Build Status](https://travis-ci.org/reTXT/concurrency-java.png)](https://travis-ci.org/reTXT/concurrency-java) [![codecov.io](https://codecov.io/github/reTXT/concurrency-java/coverage.svg?branch=master)](https://codecov.io/github/reTXT/concurrency-java?branch=master)
---
## Dispatch Queues
### Features
* Functionally equivalent interface to [GCD](https://en.wikipedia.org/wiki/Grand_Central_Dispatch)
* Global Dispatch Queues (High, Medium, Low, Main & UI)
* Concurrent Dispatch Queues
* Serial Dispatch Queues
* Dispatch Queue Groups
* Dispatch Barriers
* Android Support (currently requires [retrolambda](https://github.com/orfjackal/retrolambda))
### Implementation
All dispatch queues are implemented in 100% pure java using the standard Java 7 concurrency primitives. Global & Concurrent dispatch queues are implemented using a common priority based thread pool. Serial dispatch queues are implemented on top of an existing dispatch queue.

### Availability
Maven (snapshots in oss.jfrog.org & oss.sonatype.org)
```
  <dependency>
      <groupId>io.retxt.concurrency</groupId>
      <artifactId>dispatch</artifactId>
      <version>${version}</version>
  </dependency>
  <!-- Optional Android support -->
  <dependency>
      <groupId>io.retxt.concurrency</groupId>
      <artifactId>dispatch-android</artifactId>
      <version>${version}</version>
  </dependency>
```
Gradle
```
io.retxt.concurrency:dispatch:${version}
// Optional Android support
io.retxt.concurrency:dispatch-android:${version}
```

## Promises
### Features
* Similar interface and implementation to [PromiseKit](https://github.com/mxcl/PromiseKit)
* Defaults to executing on main queue but easily redirectable to a specific or background queue

### Implementation
Promises are implemented via the dispatch queues are there are 100% pure Java and provide tight integration with Operations as well.

### Availability
Maven (snapshots in oss.jfrog.org & oss.sonatype.org)
```
  <dependency>
      <groupId>io.retxt.concurrency</groupId>
      <artifactId>promise</artifactId>
      <version>${version}</version>
  </dependency>
```
Gradle
```
io.retxt.concurrency:dispatch:${version}
```


## Operations & Operation Queues
### Features
* General Operations
  * Same interface as Cocoa [NSOperation](https://developer.apple.com/library/ios/documentation/Cocoa/Reference/NSOperation_class/index.html) & [NSOperationQueue](https://developer.apple.com/library/ios/documentation/Cocoa/Reference/NSOperationQueue_class/index.html#//apple_ref/occ/cl/NSOperationQueue)
  * Dependencies
  * Cancellation
* Advanced Operations
  * Very similar to those described in [WWDC 2015 - Session #226](https://developer.apple.com/videos/play/wwdc2015-226/)
  * Conditions - Required to bet met before operation can be executed
    * Examples
      * Mutually Exclusion - Ensure only one instance of operations with this condition can execute simulataneously
      * No Cancelled Dependencies - Ensure operation executes only if no dependencies were cancelled
      * Delay - Delay start of operation for a specific time period
    * Easy creation of new conditions such as network reachability or UI modal exclusion
  * Finish & cancel with errors

### Implementation
All Operations and AdvancedOperations are implemented via the dispatch queues and therefore are 100% pure Java and provide tight integration with Promises as well.

### Availability
Maven (snapshots in oss.jfrog.org & oss.sonatype.org)
```
  <dependency>
      <groupId>io.retxt.concurrency</groupId>
      <artifactId>operations</artifactId>
      <version>${version}</version>
  </dependency>

```
Gradle
```
io.retxt.concurrency:operations:${version}
```
