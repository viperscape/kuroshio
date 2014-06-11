(ns examples.chan
  (:require [kuroshio.chan :as chan :refer :all]))

(let [s (new-stream)
      director (new-chan s)
      chans (take 5 (repeatedly #(new-chan s)))]

  (doseq [ch chans] 
    (future (send! director (take! ch))))

  (broadcast! director :hi)
  (prn(take 5 (from! director :force)))  ;; (:hi :hi :hi :hi :hi)




  (doall(map #(send! % %2) chans (range 5)))

  (doseq [ch chans] 
    (future (send! director (take! ch))))

  ;; the futures/threads will finish out of order
  (prn(take 5 (from! director :force))) ;; (1 2 0 4 3)
  (prn (from! director)) ;; ()


  
  (let [err-ch (new-chan s)] ;; this discards previous broadcasts
    (doseq [ch chans] 
      (future (loop []
                (let [v (take! ch)]
                  (when-not (= :quit v)
                    (try  (send! director (/ v (rand-int 2))) ;;eventually throws error
                          (catch Exception e (send! err-ch {ch (.getMessage e)})))
                    (recur))))))

    (doall(map #(send! % %2) chans (range 5)))


    (Thread/sleep 200)
    (prn(from err-ch)) ;; ({#<c* kuroshio.chan.c*@2ec41dd4> "Divide by zero"})
    (prn(from! director)) ;; (4 2 3 1)
    (broadcast! director :quit))

  (broadcast! s "this is for all channels on stream")
  (prn(from director)) ;; ("this is for all channels on stream")
  
  (let [s2 (new-stream)
      c1 (new-chan s2)
      c2 (new-chan s2)]
  (send! c2 1 c1) ;;send to-chan, value, (optional from-chan for reply)
    ;; note that d is a variable that becomes the result of what is sent to the channel c2, what's returned (inc d) in the closure is sent back
  (reply! [c2 d] (inc d)) ;; replies to whatever chan sent value to c2, assumes from-chan specified (don't use otherwise, consumes stream)
  (from! c1))) ;; (2)
