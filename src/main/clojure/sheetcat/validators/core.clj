(ns sheetcat.validators.core
  (:import [org.apache.poi.ss.usermodel CellType])
  (:require [failjure.core :as f]))

(defn validate-cell [cell]
  (if (nil? cell)
    (f/fail "cell is nil, %s", cell)))

(defn validate-cell-type [cell t]
  (let [xt (case t
             :blank CellType/BLANK
             :string CellType/STRING
             :numeric CellType/NUMERIC)]
    (if-not (-> cell (. getCellTypeEnum) (= xt))
      (f/fail "cell type unexpected, expected %s, %s" t cell))))

(defn validate-cell-streq [cell exp-v]
  (let [actual-v (. cell getStringCellValue)]
    (if-not (= actual-v exp-v)
      (f/fail "cell string value unexpected, got '%s', expected '%s'" actual-v exp-v))))

(defprotocol PValue
  (blank? [self])
  (invalid? [self]))

(defrecord BlankValue []
  PValue
  (blank? [self] true)
  (invalid? [self] false))

(defrecord BlankInvalidValue [reason]
  PValue
  (blank? [self] true)
  (invalid? [self] reason))

(defrecord VerbatimValue [value]
  PValue
  (blank? [self] false)
  (invalid? [self] false))

(defrecord DerivedValue [value]
  PValue
  (blank? [self] false)
  (invalid? [self] false))

(defrecord InvalidValue [value reason]
  PValue
  (blank? [self] false)
  (invalid? [self] reason))

(extend-protocol PValue
  Object
  (blank? [self] (empty? self))
  (invalid? [self] false))

(defn cell-fail
  ([reason] (f/fail reason))
  ([reason & reason-args] (apply f/fail reason reason-args)))

(defn fail-from-invalid [invalid]
  (f/fail (:reason invalid)))

(defprotocol PColDef
  (header-take [this cell])
  (first-state [this cell state])
  ; cell -> PValue
  (cell-default [this cell state])
  ; cell -> PValue
  (cell-take [this cell])
  ; PValue -> int|str|etc.
  (cell-clean [this value state]))

(defn cell-blank? [cell]
  (cond
    (nil? cell) true
    (= (. cell getCellTypeEnum) CellType/BLANK) true
    :else false))

(defn produce-str-value [cell]
  (if (f/ok? (validate-cell-type cell :string))
    (let [str-value (. cell getStringCellValue)]
      (VerbatimValue. str-value))
    ; failed
    (InvalidValue. nil {:reason "type mismatch" :cell cell :type (. cell getCellTypeEnum)})))

(defn produce-numeric-value [cell]
  (if (f/ok? (validate-cell-type cell :numeric))
    (let [num-value (. cell getNumericCellValue)]
      (VerbatimValue. num-value))
    ; failed
    (InvalidValue. nil {:reason "type mismatch" :cell cell :type (. cell getCellTypeEnum)})))

(defn produce-int-value [cell]
  (f/if-let-ok? [v (produce-numeric-value cell)]
    (DerivedValue. (int (:value v)))))

(defn cell-ok? [cell]
  (cond
    (invalid? cell) false
    (f/failed? cell) false
    :else true))
