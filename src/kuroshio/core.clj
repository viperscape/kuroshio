(ns kuroshio.core
  (:gen-class))

(defn v-> 
  "pushes new value onto tail"
  [t v & op]
  (let [p (or (first op) (promise))]
    (if (deliver t (cons 
                    (if-not v ::nil v) ;;note: I may remove this nil transform
                    (list p)))
      p)))

(defn v<- 
  "pulls values from stream on demand, builds lazy seq"
  [s w]
  (if (or (realized? s) w) ;; "realized? or wait"
    (cons (first @s)
          (lazy-seq (v<- (second @s) w)))))

;;note:depricated?
(def >*
  "recursively finds tail, returns it"
  (fn 
    [s]
     (if-not (realized? s)
       s
       (recur (second @s)))))

(def >n*
  "recursively moves up stream n times and return that position, or if finds tail returns it"
  (fn 
    [s n]
    (if-not (and (> n 0) (realized? s))
      s
      (recur (second @s) (dec n)))))

(defn v->* 
  "attempts to push new value onto tail, if not then recursively finds tail and tries again"
  [t v op]
  (loop [_t t]
    (if-let [_ (v-> _t v op)]
      _
      (recur (>* _t)))))

(defn v<-! 
  "pulls value from stream head, waits if necessary. safely resets head of stream to next position, only then does it return pulled value"
  [head wait]
  ((fn [head wait]
     (let [h @head]
       (if-let [r (first (v<- h wait))]
         (if (compare-and-set! head 
                               h
                               (>n* h 1))
           (cons r
                 (lazy-seq (v<-! head wait)))
           (recur head wait)))))
   head wait))

(defprotocol S*
  (put! [this v] [this v op] "inserts value onto tail of stream & extends it")
  (shift! [this n] "shift head towards tail n times, returns shift count")
  (from [this] [this w] "returns lazy-seq of stream")
  (from! [this] [this w] "returns lazy-seq & moves head")
  (take! [this] "short hand for taking first with from!")
  (get-tail [this] "aggresively finds tail.. use .tail member instead"))

(defn- revert-nil [s]
  (map #(if (= ::nil %) nil %) s))

(deftype s* [#^clojure.lang.Atom head ^{:volatile-mutable true} tail]
  S*
  (put! [this v] (put! this v nil))
  (put! [this v op] 
    (set! tail (v->* tail v op))
    tail)
  (shift! [this n] (count (take n (from! this))))

  (from [this] (from this nil))
  (from [this w] (revert-nil (lazy-seq (v<- @head w))))

  (from! [this] (from! this nil))
  (from! [this w] (revert-nil (lazy-seq (v<-! head w))))

  (take! [this] (first (from! this :force)))
  (get-tail [this] (>* @head)))

(defn new-stream
  "creates a new stream, stream argument specifies another stream which creates a new head"
  ([] (let [p (promise)] (s*. (atom p) p)))
  ([^s* s] (s*. (atom @(.head s)) (get-tail s)))
  ([^s* s head] (let [h (if (= head :tail) 
                                   (get-tail s)
                                   head)]
                        (s*. (atom h) (get-tail s)))))

(defn stream? [s]
  (= s* (type s)))

(defn merge! 
  "permanently merges two active streams, duplicates first val on s2 to s1; s2 must not be empty"
  [s1 s2] 
  (if-let [v (first (from s2))]
    (put! s1 v @(.head s2))))
