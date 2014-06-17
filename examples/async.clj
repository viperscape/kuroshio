(ns examples.async
  (:require [kuroshio.async :as a :refer :all]
            [kuroshio.chan :as c :refer :all]))

(defn increment 
  [i n]
  (prn :inc i)
  (if (> n 0)
    (yield (increment (inc i) (dec n)))
    i))

(let [tc (task-chan)
      tgreet (go-task (do(prn :hi) (yield (prn :hi-yielded-once))) tc)
      tinc-10 (go-task (increment -10 3) tc)
      tinc20 (go-task (increment 20 5) tc)
      tbye (go-task (prn :bye) tc)]

  (while (go-step tc))

  (from (:c tinc20))) ;; 25

;; :hi
;; :inc -10
;; :inc 20
;; :bye
;; :hi-yielded-once
;; :inc -9
;; :inc 21
;; :inc -8
;; :inc 22
;; :inc -7
;; :inc 23
;; :inc 24
;; :inc 25

;;

(declare my-odd?)

(defn my-even? [n]
  (if (zero? n)
    true
    (yield (my-odd? (dec (Math/abs n))))))

(defn my-odd? [n]
  (if (zero? n)
    false
    (yield (my-even? (dec (Math/abs n))))))

(let [tc (task-chan)
      e? (go-task (my-even? 1e4) tc)]
  (while (go-step tc))
  (take! (:c e?))) ;;true

;;

(defn getresult [ch] 
  (prn :ch (from ch))
  (or (first (from! ch))
      (yield (getresult ch))))

(let [tc (task-chan)
      work (new-chan (new-stream)) 
      result (go-task (getresult work) tc)
      sometask (go-task (increment 50 5) tc)]

  (future (do (Thread/sleep 1)
              (send! work :result)
              (go-task (increment 1 5) tc)))

  (while (go-step tc))

  (from (:c result))) ;; (:result)

;; :ch ()
;; :inc 50
;; :ch ()
;; :inc 51
;; :ch ()
;; :inc 52
;; :ch (:result)
;; :inc 53
;; :inc 1
;; :inc 54
;; :inc 2
;; :inc 55
;; :inc 3
;; :inc 4
;; :inc 5
;; :inc 6
  
