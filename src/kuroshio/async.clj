(ns kuroshio.async
  (:require [kuroshio.chan :as c :refer :all]
            [kuroshio.core :as k]))


(defrecord task [f ^kuroshio.chan.c* c ^kuroshio.core.s* cancel]) ;; fn and result chan, cancel stream (private)

(defn task? [t]
  (= task (type t)))

(defrecord tasks [^kuroshio.core.s* g ^kuroshio.core.s* r]) ;; go stream (filled with functions) and result stream (filled with chan data)
(defn new-tasks [] (->tasks (new-stream) (new-stream)))

(defn tasks? [^tasks ts]
  (task? (first (k/from (:g ts)))))



(defn go 
  ([f ^tasks ts] ;;create fresh task with no parent task
     (go f ts (new-chan (:r ts))))
  ([f ^tasks ts target] ;;target may or may not reference parent task
     (let [t (->task f target (new-stream))]
       (k/put! (:g ts) t) ;;add task to task-stream
       t)))

(defmacro go-task
  ([f ^tasks ts]
   `(go (fn [& _#] ~f) ~ts)))

(defmacro yield [f]
  `(fn [^tasks ts# ^task t#] (go (fn [& _#] ~f) ts# (:c t#))))

(defn go-step [^tasks ts]
  (when-let [t (first (k/from! (:g ts)))] ;;get next task
    (if-not (empty? (k/from (:cancel t))) ;;is this task cancelled?
      (go-step ts) ;;go to next task
      (let [v ((:f t) ts)] ;;call w/ provided task-stream, use result
        (or (when-not (fn? v)
              (send! (:c t) v)
              t) ;;return task for reference
            (v ts t)))))) ;;unwrap yield, call with task-stream and reference parent task -- return this new task

(defn stop-task [t] (k/put! (:cancel t) ::cancel))

(defn go-repeat 
  "repeat passed in fn, ex: (go-repeat #(prn :hi) my-tasks)"
  [f ts]
  (go-task (fn [& args] (f) (yield (go-repeat f ts))) ts))

(defn go-sleep 
  "simple sleep task (milliseconds), combine with go-select"
  [t ts]
  (let [f (fn task-sleep [t]
            (Thread/sleep 1)
            (if (> t 0) 
              (yield (task-sleep (dec t)))
              :timeout))]
    (go-task (f t) ts)))

(defn go-select 
  "simple selector between two tasks, chooses task that finished first (combine with go-sleep for a timeout)"
  [t1 t2 ts]
  (let [f (fn task-select [t1 t2]
            (let [r1 (from (:c t1))
                  r2 (from (:c t2))]
              (cond
                (not (empty? r1)) (do (stop-task t2) (first r1))
                (not (empty? r2)) (do (stop-task t1) (first r2))
                :else (yield (task-select t1 t2)))))]
    (go-task (f t1 t2) ts)))
    

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
