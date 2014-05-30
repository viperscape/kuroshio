(ns kuroshio.pool
  (:require [kuroshio.core :as k]))

(defrecord pool [#^clojure.lang.Atom p])

(defn pool? [p]
  (= pool (type p)))

(defn broadcast! [#^pool p v]
  (doseq [s @(:p p)] (k/put! s v)))

(defn add! 
  ([#^pool p] (add! p (k/new-stream)))
  ([#^pool p #^kuroshio.core.s* s] 
     (swap! (:p p) conj s)
     s))

(defn remove! [#^pool p #^kuroshio.core.s* s]
  (swap! (:p p) disj s))

(defn new-pool []
  (let [p (pool. (atom #{}))]
    p))

(defn merge-pool [p1 p2]
  (doseq [n @(:p p2)]
    (remove! p2 n)
    (add! p1 n))
  p1)

(defn members [#^pool p]
   @(:p p))

(defn member? [#^pool p #^kuroshio.core.s* s]
  (not(nil?(@(:p p) s))))
