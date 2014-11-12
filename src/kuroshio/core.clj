(ns kuroshio.core
  (:gen-class))

(defn- push
  "pushes new value onto tail"
  [t v & op]
  (let [p (or (first op) (promise))]
    (if (deliver t (cons 
                    (if-not v ::nil v) ;;note: I may remove this nil transform
                    (list p)))
      p)))

(defn- pull 
  "pulls values from stream on demand, builds lazy seq"
  [s w]
  (if (or (realized? s) w) ;; "realized? or wait"
    (cons (first @s)
          (lazy-seq (pull (second @s) w)))))

;;useful for merge finding new tail
(defn- find-tail
  "recursively finds tail, returns it"
  [s]
  (if-not (realized? s)
    s
    (recur (second @s))))

(defn- move-stream-head
  "recursively moves up stream n times and return that position, or if finds tail returns it"
  [h n]
  (if-not (and (> n 0) (realized? h))
    h
    (recur (second @h) (dec n))))

(declare force-push) ;;see below protocol

(defn- force-pull 
  "pulls value from stream head, waits if necessary. safely resets head of stream to next position, only then does it return pulled value"
  [head wait]
  ((fn [head wait]
     (let [h @head]
       (if-let [r (first (pull h wait))]
         (if (compare-and-set! head 
                               h
                               (move-stream-head h 1))
           (cons r
                 (lazy-seq (force-pull head wait)))
           (recur head wait)))))
   head wait))

(defprotocol S*
  (put! [this v] [this v op] "inserts value onto tail of stream & extends it")
  (shift! [this n] "shift head towards tail n times, returns shift count")
  (from [this] [this w] "returns lazy-seq of stream")
  (from! [this] [this w] "returns lazy-seq & moves head")
  (take! [this] "short hand for taking first with from!")
  (get-tail [this] "gets tail member")
  (update-tail [this t] "resets tail, useful for merge"))

(defn- revert-nil [s]
  (map #(if (= ::nil %) nil %) s))

(deftype s* [#^clojure.lang.Atom head 
             ^{:volatile-mutable true} tail
             #^clojure.lang.Atom size] ;;track size, for buffering
  S*
  (put! [this v] (put! this v nil))
  (put! [this v op] 
    (let [t (force-push this v op)]
      (set! tail t)
      (swap! size inc)
      t))
  (shift! [this n] (count (take n (from! this))))

  (from [this] (from this nil))
  (from [this w] (revert-nil (lazy-seq (pull @head w))))

  (from! [this] (from! this nil))
  (from! [this w] (revert-nil (lazy-seq (force-pull head w))))

  (take! [this] (first (from! this :force)))
  (get-tail [this] tail)
  (update-tail [this t] (set! tail t)))

(defn- force-push
  "attempts to push new value onto tail, if not then recursively finds tail and tries again"
  [s v op]
  (loop []
    (if-let [t (push (find-tail (get-tail s)) v op)] ;;note:we must actively find tail
      t
      (recur))))

(defn new-stream
  "creates a new stream, stream argument specifies another stream which creates a new head"
  ([] (let [p (promise)] (s*. (atom p) p (atom 0))))
  ([^s* s] (s*. (atom @(.head s)) (get-tail s) (atom @(.size s))))
  ([^s* s head] (let [h (if (= head :tail) ;;fresh copy; todo: pull into its own fn?
                          (get-tail s)
                          head)] ;;@(.head)
                  (s*. (atom h) (get-tail s) (atom @(.size s))))))

(defn stream? [s]
  (= s* (type s)))

(defn merge! 
  "permanently merges two active streams, duplicates first val on s2 to s1; s2 must not be empty"
  [s1 s2] 
  (if-let [v (first (from s2))]
    (put! s1 v @(.head s2))
    (update-tail s1 (get-tail s2)))) ;;save some time later and set tail now directly to new tail
