(ns examples.bichan
  (:require [kuroshio.bichan :as bichan :refer [new-bichan send! take!]]))

(let [bi-ch (new-bichan)
      ch (first bi-ch)]

  (future(let [f-ch (second bi-ch)]
           (if (= :start (take! f-ch))
             (send! f-ch :end))))

  (send! ch :start)
  (take! ch)) ;; :end
