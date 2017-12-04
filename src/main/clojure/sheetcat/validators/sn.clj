(ns sheetcat.validators.sn
  (:require [clojure.tools.logging :as log])
  (:require [failjure.core :as f])
  (:require [sheetcat.validators.core :as shc-val])
  (:import [sheetcat.validators.core BlankValue BlankInvalidValue DerivedValue]))

(defn validate-sn-monotone [curr state]
  (if-not (contains? state :last-sn)
    true
    (let [prev (state :last-sn)
          exp (inc prev)]
      (if-not (= exp curr)
        (f/fail "sn invariant violated, got %s, expected %s" curr, exp)
        true))))

(defn get-value [col value state]
  (cond
    (shc-val/blank? value)
      (let [[v _] (shc-val/cell-default col nil state)]
        (if (shc-val/blank? v)
            (BlankInvalidValue. "S/N could not provide a default")
            (get-value col v state)))
    :else value))

(deftype ColSN []
  shc-val/PColDef
  (header-take [this cell]
    (f/attempt-all
      [_ (shc-val/validate-cell-type cell :string)
       _is-sn (shc-val/validate-cell-streq cell "S/N")]
      true))
  (first-state [this cell state]
    (let [value (shc-val/cell-take this cell)]
      (if (or (shc-val/invalid? value)
              (shc-val/blank? value))
        (do
          (log/warn "S/N starts with blank/non-number" {:cell cell})
          state)
        (let [v (:value value)]
          (when-not (= v 1)
            (log/warn "S/N does not start from 1" v))
          (assoc state :last-sn 0)))))
  (cell-default [this cell state]
    (if (contains? state :last-sn)
      [(DerivedValue. (+ 1 (state :last-sn))) (dissoc state :last-sn)]
      [(BlankValue. ) state]))
  (cell-take [this cell]
    (shc-val/produce-int-value cell))
  (cell-clean [this value state]
    ; not needed if caller (require)'s
    ; ; fix to allow the local functions (get-value, validate-sn-monotone) to be resolved
    ; ; source: https://stackoverflow.com/a/10953908
    ; (require 'sheetcat.validators.sn)
    (let [value (get-value this value state)]
      (cond
        (shc-val/invalid? value)
          (shc-val/fail-from-invalid value)
        (shc-val/blank? value)
          (f/fail "Invariant violated: should not produce valid yet blank ValidatableValue" {:value value})
        :else
          (let [int-value (:value value)]
            (f/if-let-failed? [mv (validate-sn-monotone int-value state)]
              (log/warn "S/N is not strictly increasing" {:actual int-value :expected (inc (state :last-sn))}))
            [int-value (assoc state :last-sn int-value)])))))
