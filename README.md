# kuroshio

kuroshio streams are built using lazy-seq of nested promises. It provides a way to communicate using stream-like representations. There can be multiple producers and consumer threads working on a stream. There is an option to even read from the stream without modifying/consuming it. Values including nil can be placed onto the stream. Finally, streams can be duplicated, which can keep the head/origin of the stream intact.

> [Stream](https://github.com/viperscape/kuroshio/blob/master/examples/stream.clj) methods
- put! which extends the stream with the new value
- from and from! which return lazy sequences, the latter of which moves the head of the stream (consuming it); note that these can be overloaded with :force to block during a take
- take! which returns one value, consuming the stream; note that this purposefuly blocks waiting for a result
- and shift! which consumes the stream, returning how far it successfully moved

kuroshio channels are built on top of streams and make use of lazy filtering built in to Clojure. Instead of creating multiple streams dedicated to each communication channel you might need (see [stream pools](https://github.com/viperscape/kuroshio/blob/master/examples/pool.clj)), you can instead use a single stream and multiple channels filtering on that stream. Channels can send to one another, as well as a broadcast to all channels on that stream.

> [Channel](https://github.com/viperscape/kuroshio/blob/master/examples/chan.clj) methods
-  send! sends to a specified channel
-  broadcast! sends to all channels associated with the stream
-  from/from! and take! which are identical to stream methods

> extras
- [stream pools](https://github.com/viperscape/kuroshio/blob/master/examples/pool.clj), which are literally grouped streams; this is good for broadcasting to individual streams
- ["bidirectional" channels](https://github.com/viperscape/kuroshio/blob/master/examples/bichan-example.clj), paired channels abstracted as a single channel to communicate between two ends
- identity checks for types stream?, pool?, chan?, bichan?, chan-data? (should you want to filter channel data from a stream)

## Usage Examples

Please see initial [examples](https://github.com/viperscape/kuroshio/tree/master/examples), more to come.

## Caveats

> There are a few caveats to be aware of in kuroshio, which may or may not stick around as development progesses
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
- [broadcasting to a pool](https://github.com/viperscape/kuroshio/blob/master/examples/pool.clj#L12) will send values to all attached streams while [broadcasting with a channel](https://github.com/viperscape/kuroshio/blob/master/examples/example.clj#L40) is only to all other channels (not the initiating channel)
- from and from! are [lazy](https://github.com/viperscape/kuroshio/blob/master/examples/example.clj#L51) and thus need to be [realized in some way] (http://clojuredocs.org/clojure_core/clojure.core/doall)

## Future

I would like to abstract some things away and provide a more unified way of using the different types, hopefully preventing multiple dependency require statements

## License

Copyright © 2014 Chris Gill

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
