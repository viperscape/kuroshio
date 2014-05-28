(ns kuroshio.bichan
  (:require [kuroshio.chan :as c]))



(defprotocol Bi-C*
  (send! [this v])
  (from [this])
  (from! [this])
  (take! [this]))

(deftype bi-c* [#^kuroshio.chan.c* from #^kuroshio.chan.c* to]
  Bi-C*
  (send! [this v] (c/send! to v))
  (from! [this] (c/from! from))
  (from [this] (c/from from))
  (take! [this] (c/take! from)))

(defn new-bi-c* []
  (let [s (c/new-s*)
        ch1 (c/new-c* s)
        ch2 (c/new-c* s)]
    [(bi-c*. ch1 ch2)
     (bi-c*. ch2 ch1)]))
