(ns kuroshio.chan
  (require [kuroshio.core :as k]))

(set! *warn-on-reflection* true)

(defprotocol C*
  (put! [this ch v])
  (from [this] [this w])
  (from! [this] [this w])
  (take! [this])
  (take-data! [this]))

(defn filter-chan [c s as-data?]
  (let [data (remove #(not (or (= c (:to-chan %))
                               (and (= :all (:to-chan %))
                                    (not(= c (:from-chan %)))))) s)]
    (if as-data?
      data
      (map #(:v %) data) )))

(defrecord chan-data [to-chan v from-chan])

(defn chan-data? [v]
  (= chan-data (type v)))

(defn broadcast? 
  "determines if chan-data is a broadcast, and if so what kind (to the stream as a whole or from a specific channel)"
  [d]
  {:pre [(chan-data? d)]}
  (if (= (:to-chan d) :all)
    (if (= (:to-chan d) nil)
      :stream
      :chan)))

(deftype c* [^kuroshio.core.s* s]
  C*
  (put! [this ch v] (k/put! s (chan-data. ch v this)))
  (from! [this] (from! this nil))
  (from! [this w] (filter-chan this (k/from! s w) false))
  (from [this] (from this nil))
  (from [this w] (filter-chan this (k/from s w) false))
  (take! [this] (first(from! this :force)))
  (take-data! [this] (first(filter-chan this (k/from! s :force) true))))

(defn chan? [c]
  (= c* (type c)))

(defn send!
  "send to a channel some value, optionally specify a from-channel"
  ([^c* c v] (put! c c v))
  ([^c* tc v #^c* fc] (put! fc tc v)))

(defn broadcast! 
  "broadcasts to all other channels in the stream or to stream itself for all channels depending on type"
  [c v]
  (if (chan? c)
    (put! c :all v) ;; just to other channels
    (if (k/stream? c)
      (k/put! c (chan-data. :all v nil))))) ;; for all channels to see, send direct to the stream itself

(defn new-stream
  "convenience fn"
  ([] (k/new-stream))
  ([^kuroshio.core.s* s] (k/new-stream s))
  ([^kuroshio.core.s* s head] (k/new-stream s head)))

(defn new-chan
  "new channels start at the tail of a stream, so no previous broadcasts show up"
  ([^kuroshio.core.s* s] (c*. (k/new-stream s :tail))))


(defmacro reply! 
  "method for replying to data originator channel"
  [cv & body]
  `(let [data# (c/take-data! ~(first cv))
         ~(second cv) (:v data#)]
     (c/send! (:from-chan data#) ~@body)))
