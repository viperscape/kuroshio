(ns examples.pool
  (:require [kuroshio.core :as k])
  (:require [kuroshio.pool :as p :refer :all]))

(let [p (new-pool)
      s1 (k/new-s*)
      s2 (add! p) ;;generates a stream, returns it
      p2 (new-pool)
      s3 (add! p2)]
  (add! p s1)
  (k/put! s2 s1)
  (broadcast! p :hi)
  (remove! p s2)
  (broadcast! p :hi-again)
  (prn(k/from s2)) ;; (#<s* kuroshio.core.s*@1c4e4efb> :hi)
  (merge-pool p p2)
  (prn (members p) (members p2)) ;; #{#<s* kuroshio.core.s*@2ade3569> #<s* kuroshio.core.s*@6284967b>} #{}
  (prn(member? p s1))) ;; true
