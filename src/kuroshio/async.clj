(ns kuroshio.async
  (:require [kuroshio.chan :as c :refer :all]
            [kuroshio.core :as k]))


(defrecord task [f ^kuroshio.chan.c* c]) ;; fn and result chan

(defn task? [t]
  (= task (type t)))
(defn tasks? [tc]
 ; {:pre [(chan? tc)]}
  (= task (type (first (from tc)))))

(defn task-chan [] (new-chan (new-stream)))

(defn go 
  ([f] (let [tc (task-chan)]
         (go f tc)))
  ([f ^kuroshio.chan.c* tc] 
    ; {:pre [(chan? tc)]} 
     (let [t (->task f (new-chan (.s tc)))]
       (send! tc t)
       t)))

(defmacro go-task
  ([f ^kuroshio.chan.c* tc]
   ;  {:pre [(chan? tc)]}
     `(go (fn [tchan#] ~f) ~tc)))

(defmacro yield [f]
  `(go (fn [tc#] (go-task ~f tc#))))

(defn push-result [ch v]
  (loop [_ch ch]
    (if (empty? (from _ch)) ;;otherwise points to the parent chan
      (send! _ch v) ;;store the result in the task's chan
      (recur (take! _ch))))) ;;get parent chan and start over, going backwards

(defn go-step [tc]
  (when-let [t (first (from! tc))] ;;get next task
    (let [v ((:f t) tc)] ;;fire it off, use the result
      (or (when-not (task? v)
            (push-result (:c t) v) ;;search backwards and deliver result
            t)
          (let [nt ((:f v) tc)] ;;or process subtask
            (send! (:c nt) (:c t)) ;;searches backwards when finished
            nt)))))
