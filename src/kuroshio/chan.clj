(ns kuroshio.chan
  (require [kuroshio.core :as k]))

(defprotocol C*
  (put! [this ch v])
  (from [this] [this w])
  (from! [this] [this w])
  (take! [this]))

(defn filter-chan [c s]
  (map #(:v %) 
       (remove #(not (or (= c (:c %))
                         (= :all (:c %)))) s)))

(defrecord chan-data [c v])
(defn chan-data? [v]
  (= chan-data (type v)))

(deftype c* [#^kuroshio.core.s* s]
  C*
  (put! [this ch v] (k/put! s (chan-data. ch v)))
  (from! [this] (from! this nil))
  (from! [this w] (filter-chan this (k/from! s w)))
  (from [this] (from this nil))
  (from [this w] (filter-chan this (k/from s w)))
  (take! [this] (first(from! this :force))))

(defn chan? [c]
  (= c* (type c)))

(defn send! [#^c* c v]
  (put! c c v))
(defn broadcast! [#^c* c v]
  (put! c :all v))

(defn new-stream
  "convenience fn"
  ([] (k/new-stream))
  ([#^kuroshio.core.s* s] (k/new-stream s)))

(defn new-chan
  ([#^kuroshio.core.s* s] (c*. (k/new-stream s))))

