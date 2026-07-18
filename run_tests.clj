(require '[clojure.test :as t])

(def suites
  '[tsutae.cells.tsutae-pcb-smt.test-state-machine
    tsutae.cells.tsutae-chassis-assembly.test-state-machine
    tsutae.cells.tsutae-display-attachment.test-state-machine
    tsutae.cells.tsutae-firmware-load.test-state-machine
    tsutae.cells.tsutae-final-qc.test-state-machine
    tsutae.cells.tsutae-packaging.test-state-machine
    tsutae.cells.tsutae-device-attestation.test-state-machine
    tsutae.cells.tsutae-recycling-intake.test-state-machine
    tsutae.methods.test-agent
    tsutae.repository-contract-test])

(apply require suites)
(let [{:keys [fail error]} (apply t/run-tests suites)]
  (when-not (zero? (+ fail error))
    (System/exit 1)))
