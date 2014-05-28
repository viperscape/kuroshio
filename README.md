# kuroshio

kuroshio streams are built using lazy-seq of nested promises. It provides a way to communicate using stream-like representations. There can be multiple producers and consumer threads working on a stream. There is an option to even read from the stream without modifying/consuming it. Values including nil can be placed onto the stream. Finally, streams can be duplicated, which can keep the head/origin of the stream intact.

> Stream methods
- put! which extends the stream with the new value
- from and from! which return lazy sequences, the latter of which moves the head of the stream (consuming it)
- take! which returns one value, consuming the stream
- and shift! which consumes the stream, returning how far it successfully moved

kuroshio channels are built on top of streams and make use of lazy filtering built in to Clojure. Instead of creating multiple streams dedicated to each communication channel you might need, you can instead use a single stream and multiple channels filtering on that stream. Channels can send to one another, as well as a broadcast to all channels on that stream.

> Channel methods
-  send! sends to a specified channel
-  broadcast! sends to all channels associated with the stream
-  from/from! and take! which are identical to stream methods

## Usage Examples

Please see initial [example](https://github.com/viperscape/kuroshio/tree/master/examples), more to come.


## License

Copyright © 2014 Chris Gill

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
