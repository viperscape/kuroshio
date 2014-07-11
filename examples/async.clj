(ns examples.async
  (:require [kuroshio.async :as a :refer :all]
            [kuroshio.chan :as c :refer :all]))

(defn increment 
  [i n]
  (prn :inc i)
  (if (> n 0)
    (yield (increment (inc i) (dec n)))
    i))

(let [ts (new-tasks)
      tgreet (go-task (do(prn :hi) (yield (prn :hi-yielded-once))) ts)
      tinc-10 (go-task (increment -10 3) ts)
      tinc20 (go-task (increment 20 5) ts)
      tbye (go-task (prn :bye) ts)]

  (while (go-step ts))

  (from (:c tinc20))) ;; (25)

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

(let [ts (new-tasks)
      e? (go-task (my-even? 1e4) ts)]
  (while (go-step ts))
  (from (:c e?))) ;;(true)


;;

(defn getresult [ch] 
  (prn :ch (from ch))
  (or (first (from! ch))
      (yield (getresult ch))))

(let [ts (new-tasks)
      work (new-chan (new-stream)) 
      result (go-task (getresult work) ts)
      sometask (go-task (increment 50 5) ts)]

  (future (do (Thread/sleep 1)
              (send! work :result)
              (go-task (increment 1 5) ts)))

  (while (go-step ts))

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
  
(let [ts (new-tasks)
      data (range 10)
      results (go-task 
               (asmap #(inc %) data)
               ts)
      i (go-task (increment 10 10) ts)]
  (while (go-step ts))
  (take! (:c results))) ;; [1 2 3 4 5 6 7 8 9 10]

;;


;; select the task that finishes first
(let [ts (new-tasks)
      p (go-repeat #(prn (System/currentTimeMillis)) ts) ;;never finishes
      to (go-sleep 10 ts) ;;timeout task of 10 ms
      sel (:c (go-select to p ts))] ;;select the completing channel

  (while (go-step ts)) ;;would loop forever b/c of go-repeat, except select cancels the unending task
 (from sel)) ;;(:kuroshio.async/timeout)


;; go-wait with timeouts (shorthand for go-select with a paired timeout task)
(let [ts (new-tasks)
      p (go-repeat #(prn (System/currentTimeMillis)) ts)
      i (go-task (increment 10 3) ts)
      selp (go-wait p ts 2)
      seli (go-wait i ts)]
      
  (while (go-step ts)) 
  (prn(timeout? seli)) ;;false
  (prn(timeout? selp)) ;;true
  (prn (from (:c selp))) ;;(:kuroshio.async/timeout)
  (from (:c seli))) ;; (13)

;; 1403199017499
;; :inc 10
;; 1403199017500
;; :inc 11
;; 1403199017500
;; :inc 12
;; 1403199017500
;; :inc 13
;; 1403199017501
;; 1403199017501
;; 1403199017501
;; 1403199017501
;; 1403199017501
;; 1403199017501
;; false
;; true

;; async pipeline
(let [ts (new-tasks)
      pl (go-task (as->
                   #(inc 2)
                   #(+ % 3))
                  ts)
      pl2 (go-task (as->
                   #(dec 2)
                   #(- % 3))
                  ts)]
  (while (go-step ts))
  (let [r1 (take! (:c pl)) ;; 6
        r2 (take! (:c pl2))] ;; -2
    (- r1 r2))) ;; 8
