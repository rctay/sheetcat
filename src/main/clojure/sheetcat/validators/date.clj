(ns sheetcat.validators.date
  (:import [org.apache.poi.ss.usermodel DateUtil])
  (:require [failjure.core :as f])
  (:require [sheetcat.validators.core :as shc-val])
  (:import [sheetcat.validators.core BlankValue DerivedValue]))

(defn date-cell-take [cell]
  (f/attempt-all
    [_ (shc-val/validate-cell-type cell :numeric)
     num-value (. cell getNumericCellValue)]
    num-value))

(deftype ColDate []
  shc-val/PColDef
  (header-take [this cell]
    (f/attempt-all
      [_ (shc-val/validate-cell-type cell :string)
       _is_date (shc-val/validate-cell-streq cell "Date")]
      true))
  (first-state [this cell state]
    ; not needed if caller (require)'s
    ; ; fix to allow (date-cell-take) to be resolved
    ; ; source: https://stackoverflow.com/a/10953908
    ; (require 'sheetcat.validators.date)
    (f/attempt-all [v (date-cell-take cell)]
      (assoc state :date-raw v :date-inst (DateUtil/getJavaDate v))))
  (cell-default [this cell state]
    (let [value (if (contains? state :date-raw)
                    (DerivedValue. (state :date-raw))
                    (BlankValue. ))]
      [value state]))
  (cell-take [this cell]
    (shc-val/produce-numeric-value cell))
  (cell-clean [this value state]
    (cond
      (shc-val/invalid? value)
        (shc-val/fail-from-invalid value)
      :else
        (f/attempt-all
          [v (:value value)
           v0 (if (contains? state :date-raw) (state :date-raw) (f/fail "no first date?"))
           _ (when-not (= v v0)
               (f/fail "date invariant failed, %s" {:expected v0 :actual v :actual-inst (state :date-inst)}))]
          [(state :date-inst) state]))))
