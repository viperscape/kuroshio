(ns kuroshio.core
  (:gen-class))

(defn -main  [& args] )

(defn v-> [h t v]
  (let [p (promise)]
    (if (deliver t (cons 
                    (if-not v ::nil v) ;; may remove this nil transform
                    (list p)))
      p)))
 
(defn v<- [s w]
  (if (or (realized? s) w) ;; or wait
    (cons (first @s)
          (lazy-seq (v<- (second @s) w)))))

(def >*
  (fn [s]
     (if-not (realized? s)
       s
       (recur (second @s)))))

(def >n*
  (fn [s n]
    (if-not (and (> n 0) (realized? s))
      s
      (recur (second @s) (dec n)))))

(defn v->* [h t v]
  (loop [_t t]
    (if-let [_ (v-> h _t v)]
      _
      (recur (>* _t)))))

(defn v<-! [head wait]
  ((fn [head wait]
    (let [h @head
          r (first (v<- h nil))]
      (if r
        (if (compare-and-set! head 
                              h
                              (>n* h 1))
          (cons r
                (lazy-seq (v<-! head wait)))
          (recur head wait)))))
   head wait))

(defprotocol S*
  (put! [this v] "inserts value onto tail of stream & extends it")
  (shift! [this n] "shift head towards tail n times, returns shift count")
  (from [this] "returns lazy-seq of stream")
  (from! [this] "returns lazy-seq & moves head"))


(deftype s* [#^clojure.lang.Atom head ^{:volatile-mutable true} tail]
  S*
  (put! [this v] 
    (set! tail (v->* head tail v))
    tail)
  (shift! [this n] (count (take n (from! this))))
  (from [this] (map #(if (= ::nil %) nil %) (v<- @head nil)))
  (from! [this] (map #(if (= ::nil %) nil %) (v<-! head nil))))

(defn new-s* 
  ([] (let [p (promise)] (s*. (atom p) p)))
  ([^s* s] (s*. (atom @(.head s)) s)))

 
 
