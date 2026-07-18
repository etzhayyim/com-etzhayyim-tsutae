(ns tsutae.murakumo-test
  (:require [clojure.test :refer [deftest is testing]]
            [tsutae.murakumo :as tsutae]))

(def full-attestations
  (into {}
        (map (fn [gate] [gate (str "attested-" (name gate))]))
        (distinct (mapcat :required-gates (vals tsutae/cell-specs)))))

(deftest maps-all-legacy-tsutae-cells
  (is (= #{"tsutae_chassis_assembly"
           "tsutae_device_attestation"
           "tsutae_display_attachment"
           "tsutae_final_qc"
           "tsutae_firmware_load"
           "tsutae_packaging"
           "tsutae_pcb_smt"
           "tsutae_recycling_intake"}
         (set (map :legacy-cell (vals tsutae/cell-specs))))))

(deftest r0-gates-block-effects
  (let [plan (tsutae/cell-plan :pcb-smt
                               {:lot-id "pcb-lot-001"
                                :computed-at "2026-06-29T00:00:00Z"})]
    (is (= :blocked (:status plan)))
    (is (= [:council-charter-attestation
            :silen-tsutae-baseline-review
            :pcb-engineer-registry
            :rf-engineer-registry
            :os-firmware-engineer-registry
            :r1-activation-adr
            :robot-witness-quorum-baseline
            :open-pcb-gerbers-baseline
            :open-soc-baseline
            :binary-blob-ratio-under-5pct-baseline
            :aoi-xray-solder-qc-baseline]
           (:missing-gates plan)))
    (is (empty? (:effects plan)))))

(deftest attested-device-emits-mst-effect
  (let [plan (tsutae/cell-plan :device-attestation
                               {:attestations full-attestations
                                :serial "TSUTAE-0001"
                                :device-id "device-0001"
                                :computed-at "2026-06-29T00:00:00Z"
                                :record {:tid "device-0001"
                                         :repairEventReady true
                                         :bomCid "bafkreitsutaebom"}})
        effect (first (:effects plan))]
    (is (= :ready (:status plan)))
    (is (= :mst/put-record (:op effect)))
    (is (= tsutae/actor-did (:actor effect)))
    (is (= "com.etzhayyim.tsutae.deviceAttestation" (:collection effect)))
    (is (= "device-0001" (:rkey effect)))
    (is (= true (get-in effect [:record :repairEventReady])))
    (is (= "did:web:etzhayyim.com:tsutae:device:TSUTAE-0001"
           (get-in effect [:record :deviceDid])))))

(deftest special-gates-remain-cell-specific
  (testing "firmware load keeps bootloader unlock default gate"
    (let [attestations (dissoc full-attestations :bootloader-unlock-default-baseline)
          plan (tsutae/cell-plan :firmware-load {:attestations attestations})]
      (is (= [:bootloader-unlock-default-baseline] (:missing-gates plan)))
      (is (empty? (:effects plan)))))
  (testing "recycling keeps Li-ion operator gate"
    (let [attestations (dissoc full-attestations :liion-recycling-operator-registry)
          plan (tsutae/cell-plan :recycling-intake {:attestations attestations})]
      (is (= [:liion-recycling-operator-registry] (:missing-gates plan)))
      (is (empty? (:effects plan))))))

(deftest all-cell-plans-ready-when-attested
  (let [plans (tsutae/all-cell-plans {:attestations full-attestations
                                      :serial "TSUTAE-0001"
                                      :device-id "device-0001"
                                      :board-id "board-0001"
                                      :lot-id "lot-0001"
                                      :computed-at "2026-06-29T00:00:00Z"})]
    (is (= (set (keys tsutae/cell-specs)) (set (keys plans))))
    (is (every? #(= :ready (:status %)) (vals plans)))
    (is (= (count tsutae/cell-specs)
           (count (mapcat :effects (vals plans)))))))
