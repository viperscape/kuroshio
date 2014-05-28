(ns kuroshio.bichan
  (:require [kuroshio.core :as k]
            [kuroshio.chan :as c]))



(defprotocol BI-C*
  (send! [this v])
  (from [this])
  (from! [this])
  (take! [this]))

(deftype bi-c* [#^kuroshio.chan.c* from #^kuroshio.chan.c* to]
  BI-C*
  (send! [this v] (c/send! to v))
  (from! [this] (c/from! from))
  (from [this] (c/from from))
  (take! [this] (c/take! from)))

(defn new-bi-c* []
  (let [s (k/new-s*)
        ch1 (c/new-c* s)
        ch2 (c/new-c* s)]
    [(bi-c*. ch1 ch2)
     (bi-c*. ch2 ch1)]))

(let [bi-ch (new-bi-c*)]
  (send! (first bi-ch) :hi)
  (send! (second bi-ch) :hello)
  (take! (first bi-ch))) ;; :hello
