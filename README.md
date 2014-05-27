# kuroshio

kuroshio streams are built using lazy-seq of nested promises. It provides a way to communicate using stream-like representations. There can be multiple producers and consumer threads working on a stream. There is an option to even read from the stream without modifying/consuming it. Values including nil can be placed onto the stream. Finally, streams can be duplicated, which can keep the head/origin of the stream intact.

> There are 4 main methods: 
- put! which extends the stream with the new value
- from and from! which return lazy sequences, the latter of which moves the head of the stream (consuming it)
- take! which returns one value, consuming the stream
- and shift! which consumes the stream, returning how far it successfully moved


## Usage Examples

Please see initial [example](https://github.com/viperscape/kuroshio/tree/master/examples), more to come.


## License

Copyright © 2014 Chris Gill

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
