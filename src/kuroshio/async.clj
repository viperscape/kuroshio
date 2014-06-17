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

(defn asmap 
  "eager, applies f to each item in coll; use in go-task"
  [f coll & results]
  (if (empty? coll) 
    (first results)
    (yield (asmap f (rest coll) (conj 
                                 (into [] (first results))
                                 (f (first coll)))))))

(defn asfilter
  "eager, filters out what doesn't match true with predicate; use in go-task"
  [f coll & results]
  (if (empty? coll) 
    (first results)
    (yield (asfilter f 
                     (rest coll)
                     (if (f (first coll))
                       (conj (into [] (first results))
                             (first coll))
                       (first results))))))

(defn asreduce
  "similar to reduce, folds left; use in go-task"
  ([f coll]
     (yield (asreduce f 
                      (f (first coll))
                      (rest coll))))
  ([f v coll]
     (if (empty? coll) 
       v
       (yield (asreduce f 
                        (f v (first coll))
                        (rest coll))))))
