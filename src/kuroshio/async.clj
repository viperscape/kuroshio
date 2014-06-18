(ns kuroshio.async
  (:require [kuroshio.chan :as c :refer :all]
            [kuroshio.core :as k]))


(defrecord task [f ^kuroshio.chan.c* c]) ;; fn and result chan

(defn task? [t]
  (= task (type t)))

(defrecord tasks [^kuroshio.core.s* g ^kuroshio.core.s* r])
(defn new-tasks [] (->tasks (new-stream) (new-stream)))

(defn tasks? [^tasks ts]
 ; {:pre [(chan? tc)]}
  (task? (first (k/from (:g ts)))))



(defn go 
  ([f] (let [tc (new-tasks)] ;;not in use yet
         (go f tc)))
  ([f ^tasks ts] ;;fresh task
   ; {:pre [(chan? tc)]} 
     (go f ts (new-chan (:r ts))))
  ([f ^tasks ts target] ;;target may reference parent task
     (let [t (->task f target)]
       (k/put! (:g ts) t) ;;add task to task-stream
       t)))

(defmacro go-task
  ([f ^tasks ts]
   ;  {:pre [(chan? tc)]}
   `(go (fn [& _#] ~f) ~ts)))

(defmacro yield [f]
  `(fn [^tasks ts# ^task t#] (go (fn [& _#] ~f) ts# (:c t#))))
;  `(fn [ts#] (go-task ~f ts#)))

(defn go-step [^tasks ts]
  (when-let [t (first (k/from! (:g ts)))] ;;get next task
    (let [v ((:f t) ts)] ;;call w/ provided task-stream, use result
      (or (when-not (fn? v)
            (send! ;(or (first (from! (:c t)))
                       (:c t)
                   v)
            t) ;;return task for reference
          (let [nt (v ts t)] ;;unwrap yield, call with task-stream
           ; (send! (:c nt) (or (first (from! (:c t)))
           ;                    (:c t)))
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
