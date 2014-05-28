(require '[kuroshio.bichan :as bichan :refer [new-bi-c* send! take!]])

(let [bi-ch (new-bi-c*)
      ch (first bi-ch)]

  (future(let [f-ch (second bi-ch)]
           (if (= :start (take! f-ch))
             (send! f-ch :end))))

  (send! ch :start)
  (Thread/sleep 20)
  (take! ch)) ;; :end
