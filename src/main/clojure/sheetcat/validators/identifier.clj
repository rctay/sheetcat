(ns sheetcat.validators.identifier
  (:require [clojure.tools.logging :as log])
  (:require [failjure.core :as f])
  (:require [sheetcat.validators.core :as shc-val])
  (:import [sheetcat.validators.core BlankInvalidValue]))

(deftype ColIdentifier []
  shc-val/PColDef
  (header-take [this cell]
    (f/attempt-all
      [_ (shc-val/validate-cell-type cell :string)
       _is-identifier (shc-val/validate-cell-streq cell "Identifier")]
      true))
  (first-state [this cell state] state)
  (cell-default [this cell state]
    [(BlankInvalidValue. "Identifier blank") state])
  (cell-take [this cell]
    (shc-val/produce-str-value cell))
  (cell-clean [this value state]
    (let [state-blank (contains? state :identifier-seen-blank)]
      (cond
        (shc-val/blank? value)
          (let [state (if state-blank
                          state
                          (assoc state :identifier-seen-blank true))]
            [value state])
        (shc-val/invalid? value)
          (shc-val/fail-from-invalid value)
        :else
          (let [state (if-not state-blank
                              state
                              (do
                                (log/warn "row with blank identifier but was not last")
                                (dissoc state :identifier-seen-blank)))]
            [(:value value) state])))))
