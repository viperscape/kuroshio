(ns examples.pubsub
  (:require [kuroshio.core :as k :refer :all]))

(defn sub 
  "returns subscriber function to be run, pass in topic keyword and stream"
  [t s]
  {:pre [(keyword? t)]}
  (let [_s (new-stream s)] ;;build our own stream copy to work on
    (fn [] 
      (->> (from! _s)
           (map #(t %))
           (remove nil?)))))

(let [s (new-stream)
      ui-sub (sub :ui s)]

  (put! s {:something :updates})
  (put! s {:input :space-bar
           :ui :clear})
  (put! s {:submit :click
           :ui :draw})

  (prn (take 5 (ui-sub))) ;; (:clear :draw)
  (prn (take 5 (ui-sub))) ;; ()
  (from s)) ;;({:something :updates} {:ui :clear, :input :space-bar} {:submit :click, :ui :draw})
