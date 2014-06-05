(ns examples.pubsub
  (:require [kuroshio.core :as k :refer :all]))

(defn sub 
  "returns subscriber function to be run, pass in topic keyword and stream"
  [t s]
  {:pre [(keyword? t)]}
  (let [_s (new-stream s)] ;; build our own stream copy to work on
    (fn [] 
      (->> (from! _s) ;; grab all avail, move head of stream copy
           (filter t) ;; filter out anything but what we want
           (map t))))) ;; just get the topic of interest
           

(let [s (new-stream)
      ui-sub (sub :ui s)
      inp-sub (sub :input s)]

  (put! s {:something :updates})
  (put! s {:ui :fade})
  (put! s {:input :space-bar
           :ui :clear})
  (put! s {:input :click
           :ui :draw})

  (prn (first (ui-sub))) ;; :fade
  (future (prn (inp-sub))) ;; (:space-bar :click)
  (ui-sub)) ;; (:clear :draw)
