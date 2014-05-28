(ns kuroshio.chan
  (require [kuroshio.core :as k]))

(defprotocol C*
  (put! [this ch v])
  (from [this])
  (from! [this])
  (take! [this]))

(defn filter-chan [c s]
  (map #(:v %) 
       (remove #(not (or (= c (:c %))
                         (= :all (:c %)))) s)))

(defrecord data [#^c* c v])

(deftype c* [#^kuroshio.core.s* s]
  C*
  (put! [this ch v] (k/put! s (data. ch v)))
  (from! [this] (filter-chan this (k/from! s)))
  (from [this] (filter-chan this (k/from s)))
  (take! [this] (first(from! this))))

(defn send! [c v]
  (put! c c v))
(defn broadcast! [c v]
  (put! c :all v))

(defn new-s*
  ([] (k/new-s*))
  ([#^kuroshio.core.s* s] (k/new-s* s)))

(defn new-c*
  ([#^kuroshio.core.s* s] (c*. (new-s* s))))

