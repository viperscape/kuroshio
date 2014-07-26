(ns kuroshio.multi
  (:require [kuroshio.core :as k])
  (:import [java.net InetAddress DatagramPacket MulticastSocket]
           [java.io ObjectOutputStream ObjectInputStream ByteArrayInputStream ByteArrayOutputStream]))

(defn init-sock [g p]
  (let [s (MulticastSocket. p)]
    (.joinGroup s g)
    s))

(defn init-rcv [s]
  (let [st (k/new-stream)]
    (future (loop []
              (let [p (DatagramPacket. (byte-array 1024) 1024)
                    _ (.receive s p)
                    bis (ByteArrayInputStream. (.getData p))
                    is (ObjectInputStream. bis)
                    msg (.readObject is)]
                (when-not (= msg ::quit)
                  (k/put! st msg)
                  (recur)))))
    st))

(defprotocol M*
  (put! [this v])
  (from [this])
  (from! [this])
  (take! [this]))

(defrecord mso [s g p])

(defn ->bytes [o]
  (let[bos (ByteArrayOutputStream.)
       os (ObjectOutputStream. bos)]
    (.writeObject os o)
    (.flush os)
    (.toByteArray bos)))

(defn new-packet [^bytes b ^mso m]
  (DatagramPacket. b (count b) (:g m) (:p m)))

(deftype m* [^kuroshio.core.s* st ^mso ms]
  M*
  (put! [this v] (.send (:s ms) (new-packet (->bytes v) ms)))
  (from [this] (k/from st))
  (from! [this] (k/from! st))
  (take! [this] (first(k/from! st :force))))

(defn multi? [m]
  (= m* (type m)))

(defn new-multi [g p]
  (let [so (init-sock g p)
        st (init-rcv so)
        ms (mso. so g p)]
    (m*. st ms)))

(defn get-group [a] (InetAddress/getByName a))


;; example
(defn multi-example []
  (let [m (new-multi (get-group "228.5.6.7") 6789)]
    (put! m {:test :passed?})
    (put! m ::quit)
    (Thread/sleep 5)
    (from m)))
