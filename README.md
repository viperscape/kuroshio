# kuroshio

[streams](#stream-methods) | [channels](#channel-methods) | [examples](/examples/) | [caveats](#caveats) | [rationale](#rationale) | alpha/experimental | ``` [kuroshio "0.2.3-SNAPSHOT"] ```

#### About
kuroshio is a Clojure library for creating and operating on streams (delayed, lazily-evaluated, endless lists). Streams work similar to lazy sequences in clojure and in fact are accessed by a return of a lazy-seq object to work on (so lazy-seq and seq operations in clojure work with streams in kuroshio). Streams can be operated on by multiple threads and ease communication between threads.

kuroshio streams are built using lazy-seq of nested promises. It provides a way to communicate using stream-like representations. There can be multiple producers and consumer threads working on a stream. There is an option to even read from the stream without modifying/consuming it. Values including nil can be placed onto the stream. Finally, streams can be duplicated, which can keep the head/origin of the stream intact. When basing a stream object off of another stream (duplicating it) the streams can then be operated on simultaneously and read from in a safe manner while gaining the benefits of lazy-seq caching inherent in Clojure.  

Think of streams as a way to simulate data over time, and process these 'events' at later times, repeatedly. Unlike regular queues which typically contain a single type of emphemeral object, streams can be operated on multiple times from multiple threads and thus can house different data with a central theme. The most basic of examples would be a stream of integers where two threads read the same stream (but two stream objects; duplicated) and each thread filters the integers that they need (i.e. evens for one thread and odds for the other). You could simulate this with queues or linkedlists by reading and then splitting the integers to two more queues/lists to be consumed by the two threads, but this quickly shows how inefficient this process could be.

> ##### [Stream](https://github.com/viperscape/kuroshio/blob/master/examples/stream.clj) methods
- put! which extends the stream with the new value
- from and from! which return lazy sequences, the latter of which moves the head of the stream (consuming it); note that these can be overloaded with :force to block during a take
- take! which returns one value, consuming the stream; note that this purposefuly blocks waiting for a result
- and shift! which consumes the stream, returning how far it successfully moved

kuroshio channels are built on top of streams and make use of lazy filtering built in to Clojure. Instead of creating multiple streams dedicated to each communication channel you might need (see [stream pools](https://github.com/viperscape/kuroshio/blob/master/examples/pool.clj)), you can instead use a single stream and multiple channels filtering on that stream. Channels can send to one another, as well as a broadcast to all channels on that stream.

> ##### [Channel](https://github.com/viperscape/kuroshio/blob/master/examples/chan.clj) methods
-  send! sends to a specified channel
-  broadcast! sends to all channels associated with the stream
-  from/from! and take! which are identical to stream methods

There are a few extras thrown in that may or may not get changed in future releases

> Extras
- [stream pools](https://github.com/viperscape/kuroshio/blob/master/examples/pool.clj), which are literally grouped streams; this is good for broadcasting to individual streams
- [bidirectional channels](https://github.com/viperscape/kuroshio/blob/master/examples/bichan-example.clj), paired channels abstracted as a single channel to communicate between two ends
- [multicast streams](https://github.com/viperscape/kuroshio/blob/master/src/kuroshio/multi.clj), a multicast-socket connection to communicate between two processes (mostly experimental)
- [async tasks](https://github.com/viperscape/kuroshio/blob/master/src/kuroshio/async.clj), a very basic implementation of some async tasking/generators using streams (definitely experimental); [take a peek](https://github.com/viperscape/kuroshio/blob/master/examples/async.clj)
- identity checks for types stream?, pool?, chan?, bichan?, chan-data? (should you want to filter channel data from a stream)

There are a few caveats to be aware of in kuroshio, which may or may not stick around as development progesses

> ##### Caveats
- never force a stream without taking a finite amount from it
```clojure
(let [mystream (new-stream)]
     (from mystream :force) ;;bad, will always block
     (take 50 (from mystream :force)) ;; good, waits on 50 elements
     (from mystream)) ;; not necessarily bad, pulls what ever is available, does not force/block
```
- there is currently no way to short circuit a forced realization on a stream
- take! should be preferred over first on an unforced stream, else you cannot determine if nil is present
``` clojure
(let [s (new-stream)]
     (take! s) ;; best, only returns nil if it was actuslly put onto stream but will block waiting for it
     (first (from s)) ;; bad, might return nil because no value was retrieved, not because nil was present
     (let [v (take 1 (from s))] ;; not bad, returns val or ::empty so nil values are evident and doesn't block
     	  (if (empty? v) ::empty (first v))))
```
- [broadcasting to a pool](https://github.com/viperscape/kuroshio/blob/master/examples/pool.clj#L12) will send values to all attached streams while [broadcasting with a channel](https://github.com/viperscape/kuroshio/blob/master/examples/example.clj#L40) is only to all other channels (not the initiating channel); consider broadcasting to stream with associated channels [for all channels to recieve](https://github.com/viperscape/kuroshio/blob/master/examples/chan.clj#L45)
- from and from! are [lazy](https://github.com/viperscape/kuroshio/blob/master/examples/example.clj#L51) and thus need to be [realized in some way] (http://clojuredocs.org/clojure_core/clojure.core/doall)
- [flatten](http://clojuredocs.org/clojure_core/clojure.core/flatten) for some reason [doesn't jive](https://github.com/viperscape/kuroshio/issues/1), so steer clear for now and consider something like [apply concat](https://github.com/viperscape/kuroshio/issues/1#issuecomment-44845506)
- [interleave](http://clojuredocs.org/clojure_core/clojure.core/interleave) is not suggested with mutable methods like "from!", it discards incomplete sets and thus may incidentally shift the stream head without you realizing. Consider this instead, which will be making it into kuroshio soon along with other multiple stream operations:
```clojure
(defn weave! [s]
  (lazy-seq (cons (doall (map #(k/take! %) s))
                  (weave! s))))
(apply concat (take 2 (weave! (my-seq-of-streams))))
;; or perhaps
(first (weave! (my-seq-of-streams)))
```

There are a few reasons why I started developing kuroshio, primarily out of curiosity:

> ##### Rationale
- I was interested in a simplistic way to achieve thread-communication
- wanted to work on a stream of objects without affecting the stream itself; check out kuroshio's stream-copy concept and 'from' method -- these provide ways to look at a stream without affecting other threads looking at the same stream. 
- wanted non-blocking stream polling methods (look at both from and from!)
- because of stream-copy it's possible to filter the same stream multiple times for specific contents (check out pub-sub example, or channels in general)
- wanted a way to add a watcher and logger for data and have it be triggered automatically in another thread (see the watch example in examples/stream)
- wanted simplistic pipelining
- geared it to work with typical core lazyseq methods, for ease of use and compatibility
- I was adamaent about not intertwining threads with stream communication as a dependency, they should stay separate and be predictable
- wanted a seperate async/multitasking portion for kuroshio that isn't tied to threading on the JVM, with 'async' it's currently possible to interleave multiple looping/iterating/generating operations within a single thread (it makes use of channels, though I may change this in the future)

## Future

I would like to abstract some things away and provide a more unified way of using the different types, hopefully preventing multiple dependency require statements

## License

Copyright © 2014 Chris Gill

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
