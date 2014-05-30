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

(defn new-bichan []
  (let [s (c/new-stream)
        ch1 (c/new-chan s)
        ch2 (c/new-chan s)]
    [(bi-c*. ch1 ch2)
     (bi-c*. ch2 ch1)]))
