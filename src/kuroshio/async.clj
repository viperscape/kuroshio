(ns kuroshio.async
  (:require [kuroshio.chan :as c :refer :all]
            [kuroshio.core :as k]))


(defrecord task [f ^kuroshio.chan.c* c]) ;; fn and result chan

(defn task? [t]
  (= task (type t)))
(defn tasks? [^kuroshio.chan.c* tc]
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
     `(go (fn [_#] ~f) ~tc)))

(defmacro yield [f]
  `(fn [tc#] (go (fn [_#] ~f) tc#)))
;  `(fn [tc#] (go-task ~f tc#)))

(defn go-step [^kuroshio.chan.c* tc]
  (when-let [t (first (from! tc))] ;;get next task
    (let [v ((:f t) tc)] ;;fire it off, use the result
      (or (when-not (fn? v)
            (send! (or (first (from! (:c t)))
                       (:c t))
                   v)
            t)
          (let [nt (v tc)] ;;unwrap yield and call with task-chan
            (send! (:c nt) (or (first (from! (:c t)))
                               (:c t)))
            nt)))))
