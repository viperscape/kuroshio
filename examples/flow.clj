(ns examples.flow
  (:require [kuroshio.core :as k]
            [kuroshio.chan :as c]))

(defn lock-step 
  "lock-step example; producer waits on consumer chans to generate data"
  []
  (let [s (k/new-stream) ;;new stream for chans
        p1 (c/new-chan s) ;;producer
        c1 (c/new-chan s) ;;consumer1
        c2 (c/new-chan s) ;;consumer2
        await! (fn [gen con] ;;await new values from producer/generator
                 (c/send! gen con) ;;let generator know we're ready
                 (c/take! con))] ;;wait on generator

    ;;producer will wait for consumers
    (future (loop [n (range)] ;;loop infinitely
              (let [chan (c/take! p1)] ;;v will be the chan to send data to
                (when-not (= :quit chan) ;;quit loop when told to
                  (c/send! chan (first n))
                  (recur (rest n))))))

    (dotimes [_ 5] (c/send! p1 c1)) ;;buffer 5

    (Thread/sleep 10) ;;wait for producer future
    (prn(c/from! c1)) ;; (0 1 2 3 4)

    (future (dotimes [_ 5] (prn(await! p1 c1))))
    (dotimes [_ 5] (prn(await! p1 c2)))
    
    (Thread/sleep 10) ;;wait for consumer future
    (c/send! p1 :quit)))
