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



(declare my-odd?)

(defn my-even? [n]
  (if (zero? n)
    true
    (yield (my-odd? (dec (Math/abs n))))))

(defn my-odd? [n]
  (if (zero? n)
    false
    (yield (my-even? (dec (Math/abs n))))))

;; fyi: this is slower than trampoline
(time
(let [tc (task-chan)
      e? (go-task (my-even? 1e4) tc)]
  (while (go-step tc))
  (take! (:c e?)))) ;;true
