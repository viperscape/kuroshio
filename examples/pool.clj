(ns kuroshio.pool
  (:require [kuroshio.core :as k]))

(defn broadcast! [pool v]
  (doseq [s @pool] (k/put! s v)))

(defn add-stream! [pool]
  (let [s (k/new-s*)]
    (swap! pool conj s)
    s))

(let [pool (atom [])
      s1 (add-stream! pool)
      s2 (add-stream! pool)]

  (k/put! s2 s1)
  (broadcast! pool :hi)

  (k/from! s2)) ;; (#<s* kuroshio.core.s*@68f8093f> :hi)
