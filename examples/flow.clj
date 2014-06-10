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


(defn trans 
  "signal transform example; transition chan is signaled when to change, and always in a sequential manner because of the chaining in the map"
  []
  (let [s (k/new-stream)
        t1 (c/new-chan s)
        ;; chain together signals to transition between
        trans-light {:red :green, :green :yellow, :yellow :red}
        trans-screen {:menu :game-screen, :game-screen :menu}
        transfn (fn [_sig] 
                  (loop [sig _sig]
                    (when-let [new-sig (sig (c/take! t1))]
                      (prn new-sig)
                      (recur new-sig))))]

    ;; transition between street light signals
    (future (transfn :red))
    (c/send! t1 trans-light) ;; :green
    (c/send! t1 trans-light) ;; :yellow
    (c/send! t1 trans-light) ;; :red
    (c/send! t1 trans-light) ;; :green
    (c/send! t1 :quit)

    (Thread/sleep 20)
    ;; swap between a game-screen and a menu
    (future (transfn :menu)) ;; start on menu
    (c/send! t1 trans-screen) ;; :game-screen
    (c/send! t1 trans-screen) ;; :menu
    (c/send! t1 trans-screen) ;; :game-screen
    (c/send! t1 :quit)))
