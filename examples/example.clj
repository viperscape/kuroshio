(ns kuroshio.example
  (require [kuroshio.core :as k]
           [kuroshio.chan :as c]))

(let [s (k/new-s*)
      s-copy (k/new-s* s) ;;duplicate stream
      s2 (k/new-s*)]
  (doseq [n (range 1e5)] (k/put! s n)) ;;lets put a bunch of numbers in this stream
  (k/put! s2 -1)
  (k/put! s2 nil) ;;this gets transformed to ::nil and then back again once taken back out of the stream

  (.start(Thread. #(dotimes [_ 10] 
                     (Thread/sleep (rand-int 2))
                     (k/put! s2 {:t1 (first (k/from! s))}))))

  (dotimes [_ 10]
    (Thread/sleep (rand-int 2))
    (k/put! s2 {:t2 (k/take! s)}))

  (Thread/sleep 10)

  ;(take 5 (k/from! s2)) ;;would move head of stream once since it's lazy, wrap in doall or do something with results to move head of the stream

  (k/shift! s2 18) ;;shifts the head of the stream n times

  (prn(take 10 (k/from! s2))) ;; ({:t2 16} {:t1 17} {:t1 18} {:t2 19})

  ;; note how s-copy kept its original starting point
  (prn(take 5 (k/from s-copy))) ;; (0 1 2 3 4)


  ;; channels using a single stream
  (let [sc (k/new-s*) ;;new stream for our channels
        ch1 (c/new-c* sc)
        ch2 (c/new-c* sc)]
    (c/send! ch2 :hi) ;; send to just one specific channel
    (c/send! ch1 :yo)
    (c/broadcast! ch1 :hi-yall) ;; sends to all channels on stream
    (prn(c/from! ch2)) ;; (:hi :hi-yall)
    (prn(c/from! ch2)) ;; ()
    (c/broadcast! ch2 :bye-bye) ;; any channel can be specified for broadcast
    (prn(c/from! ch1)))) ;; (:yo :hi-yall :bye-bye)
