## Dispatch Queues (akin to Grand Central Dispatch)
### Features
* Global Dispatch Queues (High, Medium, Low, Main & UI)
* Concurrent Dispatch Queues
* Serial Dispatch Queues
* Dispatch Queue Groups
* Dispatch Barriers

### Implementation
All dispatch queues are implemented in 100% pure java using the standard Java 7 concurrency primitives. Global & Concurrent dispatch queues are implemented using a common priority based thread pool. Serial dispatch queues are implemented on top of an existing dispatch queue.

## Cocoa Advanced Operations
### Features
* Simple Operations
 * Same interface as NSOperation & NSOperationQueue
* Advanced Operations
 * Very similar to those described in [WWDC 2015 - Session #226](https://developer.apple.com/videos/play/wwdc2015-226/)
 * Conditions - Required to bet met before operation can be executed
  * Mutually Exclusion - Ensure only one instance of operations with this condition can execute simulataneously.
 * Finish & Cancel Error support
