(ns kuroshio.pool
  (:require [kuroshio.core :as k]))

(defrecord p* [^clojure.lang.Atom pool])

(defn pool? [p]
  (= p* (type p)))

(defn broadcast! 
  "broadcast to all streams in the pool"
  [^p* p v]
  (doseq [s @(:pool p)] (k/put! s v)))

(defn add! 
  ([^p* p] (add! p (k/new-stream)))
  ([^p* p ^kuroshio.core.s* s] 
     (swap! (:pool p) conj s)
     s))

(defn remove! [^p* p ^kuroshio.core.s* s]
  (swap! (:pool p) disj s))

(defn new-pool []
  (p*. (atom #{})))

(defn merge-pool! 
  "merges second pool into first pool, empties second pool"
  [p1 p2]
  {:pre [(pool? p1)(pool? p2)]}
  (doseq [n @(:pool p2)]
    (remove! p2 n)
    (add! p1 n))
  p1)

(defn members 
  "returns the current pool's members"
  [^p* p]
   @(:pool p))

(defn member? [^p* p ^kuroshio.core.s* s]
  (not(nil?(@(:pool p) s))))
