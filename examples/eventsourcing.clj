;;trivial event sourcing example
(ns examples.eventsourcing
  (:require [kuroshio.core :as k :refer :all]))

(defn sub 
  "returns subscriber function to be run, pass in topic keyword and stream"
  [t s]
  {:pre [(keyword? t)]}
  (let [_s (new-stream s)] ;; build our own stream copy to work on
    (fn [] (filter t (from! _s))))) ;;filter topic from stream
    
(let [ev (new-stream) ;;event stream
      err (sub :err ev) ;;error events in ev
      ev-s #(remove :err (from ev)) ;;event-store: events without errors
      ui-sub (sub :ui ev)
      ui #(remove :err (ui-sub))
      ss #(reductions merge {} (ev-s))]
  (put! ev {:ui :fill-red})
  (put! ev {:animate :thing
            :ui :clear})
  (put! ev {:ui :fill-blue
            :err :cannot-fill-blue})
  (put! ev {:ui :fill-green})

  (let [snaps (ss)]
    {:effects snaps
     :snapshot (last snaps)}))

;; {:effects ({} 
;;            {:ui :fill-red} 
;;            {:animate :thing, :ui :clear} 
;;            {:animate :thing, :ui :fill-green}), 
;;  :snapshot {:animate :thing, :ui :fill-green}}
