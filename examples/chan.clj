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

  (prn(take 5 (from! director :force))) ;; (1 2 0 4 3)



  
  (let [err-ch (new-chan s)] ;;note that in current version of kuroshio this will pick up the previous broadcast :hi
    (doseq [ch chans] 
      (future (loop []
                (let [v (take! ch)]
                  (when-not (= :quit v)
                    (try  (send! director (/ v (rand-int 2))) ;;eventually throws error
                          (catch Exception e (send! err-ch {ch (.getMessage e)})))
                    (recur))))))

    (doall(map #(send! % %2) chans (range 5)))


    (Thread/sleep 200)
    (prn(from err-ch)) ;; (:hi {#<c* kuroshio.chan.c*@2ec41dd4> "Divide by zero"})
    (prn(from director)) ;; (4 2 3 1)
    (broadcast! director :quit)))
