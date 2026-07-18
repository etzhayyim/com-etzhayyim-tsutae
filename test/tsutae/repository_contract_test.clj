(ns tsutae.repository-contract-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]))

(def root (io/file (System/getProperty "user.dir")))

(deftest canonical-edn-contract
  (doseq [path ["manifest.edn" "products.edn" "schema/schema.edn"
                "schema/kotoba.edn" "data/seed.kotoba.edn"]]
    (is (some? (edn/read-string (slurp (io/file root path)))) path)))

(deftest no-legacy-artifacts
  (is (empty? (filter #(re-find #"\\.(?:go|sh|json|jsonld|bpmn)$" (.getName %))
                      (filter #(.isFile %) (file-seq root))))))
