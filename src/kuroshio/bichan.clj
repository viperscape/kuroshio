(ns kuroshio.bichan
  (:require [kuroshio.chan :as c]))



(defprotocol Bi-C*
  (send! [this v])
  (from [this] [this w])
  (from! [this] [this w])
  (take! [this]))

(deftype bi-c* [#^kuroshio.chan.c* from-ch #^kuroshio.chan.c* to-ch]
  Bi-C*
  (send! [this v] (c/send! to-ch v))
  (from! [this] (from! this nil))
  (from! [this w] (c/from! from-ch w))
  (from [this] (from this nil))
  (from [this w] (c/from from-ch w))
  (take! [this] (c/take! from-ch)))

(defn new-bi-c* []
  (let [s (c/new-s*)
        ch1 (c/new-c* s)
        ch2 (c/new-c* s)]
    [(bi-c*. ch1 ch2)
     (bi-c*. ch2 ch1)]))
