(ns kuroshio.chan
  (require [kuroshio.core :as k]))

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
  ([^c* c v] (put! c c v))
  ([^c* tc v #^c* fc] (put! fc tc v)))

(defn broadcast! 
  "broadcasts to all other channels in the stream"
  [^c* c v]
  (put! c :all v))

(defn new-stream
  "convenience fn"
  ([] (k/new-stream))
  ([^kuroshio.core.s* s] (k/new-stream s))
  ([^kuroshio.core.s* s head] (k/new-stream s head)))

(defn new-chan
  ([^kuroshio.core.s* s] (c*. (k/new-stream s :tail))))

