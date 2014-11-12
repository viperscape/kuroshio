(ns examples.stream
  (:require [kuroshio.core :as k :refer :all]))

(let [s1 (new-stream)
      s-copy (new-stream s1)
      s2 (new-stream)]

  (put! s1 :hi)
  
  (doseq [n (range 10)] (put! s1 n))

  ;; "from!" mutates stream as it takes from it
  (prn(take 3 (from! s1))) ;;(:hi 0 1)

  ;; non-volatile peek using "from"
  (prn(take 3 (from s1))) ;;(2 3 4)
  (prn(from! s1)) ;; (2 3 4)

  ;; putting a stream itself or nil onto a stream are possible
  (let [ts (new-stream)]
    (put! ts s1)
    (put! ts nil)
    (prn (from! ts))) ;;(#<s* kuroshio.core.s*@7157385e> nil)

  (future (do (Thread/sleep 200)
              (doseq [n (range 10)] (put! s1 n))))

  ;; "take!" blocks until next item is available
  (prn (take! s1)) ;;0
  (prn (first (from! s1 :force))) ;; this is identical to take!, thus waits if necessary

  (prn (take 3 (from s1 :force))) ;;(2 3 4)

  (shift! s1 3) ;;shifts head towards tail for the stream
  (prn (from s1)) ;; (5 6 7 8 9)

  ;; s-copy stream is the actual data that is also in s1, but consists of a different head-node and thus keeps its position in the stream seperate from s1 stream
  (prn(first(from! s-copy))) ;; :hi
  (prn(rest (filter even? (from! s-copy)))) ;; (2 4 6 8 0 2 4 6 8)

  (put! s1 :something)
  (put! s1 1)
  (put! s1 2)
  (prn (from s-copy)) ;; (:something 1 2)

  ;; filter out non-numbers first, like any other sequence
  (prn (filter even? (filter number? (from! s1)))) ;;(6 8 2)
  ;; notice how the stream print below is now empty, that's because we traversed the stream filtering for even numbers using "from!"
  (prn (from s1)) ;;()

  (let [watch (fn [f s] (loop []
                          (when-let [v (take! s)]
                            (f v)
                            (recur))))
        ws (new-stream s1)] ;;create a new stream copy, ahead of time
    (future (watch 
             #(if (even? %) (prn %)) 
             ws)) ;; setup basic watch thread
    (put! s1 1)
    (put! s1 2)
    (doseq [n (range 5)] (put! s1 n))
    (put! s1 nil)
    (prn (from! s1))) ;; (1 2 0 1 2 3 4 nil)
    ;; future then prints 2 0 2 4 and then quits
)


(let [s (new-stream)
      s2 (new-stream)]

  (put! s 1)
  (put! s 2)
  (put! s2 3)
  (merge! s s2)
  (put! s2 4)
  (put! s2 5)
  (put! s 6) ;;both streams are still active and now point to the same tip
  (put! s2 7)
  
  (prn (from s)) ;;(1 2 3 4 5 6 7)
  (prn (from s2))) ;;(3 4 5 6 7)
