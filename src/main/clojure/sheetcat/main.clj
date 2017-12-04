(ns sheetcat.main
  (:gen-class)
  (:import [java.io FileInputStream])
  (:import [org.apache.poi.xssf.usermodel XSSFWorkbook])
  (:require [clojure.string :as str])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.java.io :as io])
  (:require [failjure.core :as f])
  (:require [sheetcat.core :refer [smooth-cells-seq]])
  (:require [sheetcat.validators.core :as shc-val])
  (:require [sheetcat.validators sn identifier date])
  (:import [sheetcat.validators.sn ColSN]
           [sheetcat.validators.identifier ColIdentifier]
           [sheetcat.validators.date ColDate]))

(def HEADER-COUNT 3)
(defn make-header-exp []
  [
    (ColSN. )
    (ColIdentifier. )
    (ColDate. )
  ])

(defn reader-row-header [cells header-exp]
  (f/attempt-all
    [cells (take HEADER-COUNT cells)
     all-strings (map #(-> % (shc-val/validate-cell-type :string)) cells)
     _ (if (some f/failed? all-strings)
         (f/fail "not all strings, %s" {:row_e cells}))
     header-check (map #(shc-val/header-take %1 %2) header-exp cells)
     _ (if (some f/failed? header-check)
         (f/fail "header not as expected, %s" {:header header-check}))]
    nil))

(defn reader-row [header-exp [state result1] row]
  (let [cells (smooth-cells-seq row)]
    (loop [header-exp header-exp
           cells cells
           state state
           result []
           should-skip false]
      (if (or (contains? state :last-row)
              (empty? header-exp)
              (empty? cells))
          [state (conj result1 result)]
          (let [v (f/attempt-all
                    [header-exp header-exp
                     cells cells
                     coldef (first header-exp)
                     cell (first cells)
                     v0 (if (shc-val/cell-blank? cell)
                            (shc-val/cell-default coldef cell state)
                            (f/if-let-ok? [v (shc-val/cell-take coldef cell)]
                              [v state]
                              ; else, returns the monadic failure
                              #_()))
                     v1 (let [[value state] v0]
                          (shc-val/cell-clean coldef value state))]
                    v1)]
            (if (f/failed? v)
                (recur (rest header-exp) (rest cells) state (conj result v) v)
                (let [[value state-post] v
                      state-post
                        (if (and (not (contains? state :identifier-seen-blank))
                                 (contains? state-post :identifier-seen-blank))
                            (do
                              (log/info "seen blank" (. row getRowNum))
                              (assoc state-post :identifier-seen-blank (. row getRowNum)))
                            state-post)]
                  (recur (rest header-exp) (rest cells) state-post (conj result value) should-skip))))))))

(defn reader-sheet [sheet]
  (let [rows (iterator-seq (. sheet rowIterator))
        [header & rows] rows
        header-exp (make-header-exp)
        row-check (-> header (smooth-cells-seq) (reader-row-header header-exp))
        _ (if (f/failed? row-check)
              (throw (ex-info "Unexpected header" {:context row-check})))
        [row1 & _] rows
        state (let [cells (-> row1 (smooth-cells-seq))
                    args (map list header-exp cells)]
                (reduce (fn [m [a b]]
                          (shc-val/first-state a b m))
                        {} args))
        [state rows] (reduce (partial reader-row header-exp)
                             [state []]
                             rows)
        rows (if-let [last-row (state :identifier-seen-blank)]
               (take (dec last-row) rows)
               rows)
        groups (group-by (fn [row]
                           (every? shc-val/cell-ok? row))
                         rows)]
    (let [rows-ok (groups true)
          rows-failed (groups false)]
      (if-not (empty? rows-failed)
        (log/warn "there were invalid entries" rows-failed))
      rows-ok)))

(def DAYS
  [" (Mon)"
   " (Tues)"
   ;;" (Wed)"
   " (Thurs)"
   " (Fri)"
   ])

(defn reader [path]
  (let [wb (->> path (new FileInputStream) (new XSSFWorkbook))
        sheets (filter (fn [i]
                         (let [sheet-name (. wb getSheetName i)]
                           (some (partial str/ends-with? sheet-name) DAYS)))
                       (range (. wb getNumberOfSheets)))
        _ (if-not (= (count DAYS) (count sheets))
            (log/warn "fewer sheets than expected" sheets))
        sheets (map #(->> % (log/spyf :info "getting sheet %d") (. wb getSheetAt)) sheets)]
    (reduce (fn [m sheet]
              (log/info "got sheet" (. sheet getSheetName) sheet)
              (let [result (reader-sheet sheet)]
                (concat m result)))
            []
            sheets)))

(defn format-date [date]
  (let [parts [(+ 1900 (. date getYear))
               (format "%02d" (inc (. date getMonth)))
               (format "%02d" (. date getDate))]]
    (->> parts (map str) (str/join "-"))))

(defn row-str [row]
  (let [[sn id date] row
        row [(str sn) (str id) (format-date date)]]
    (str/join "\t" row)))

(defn format1 [mn]
  (->> mn
       (map row-str)
       (str/join "\n")))

(defn sort-cat [rows]
  (let [third #(-> % (next) (next) (first))
        get-date third
        get-id second]
    (sort-by (juxt get-id get-date) rows)))

(defn write-to-file [filename contents]
  (if (or (str/ends-with? filename ".xlsx")
          (str/ends-with? filename ".xlx"))
      (log/warn "writing to Excel file?" filename))
  (if (.exists (io/file filename))
      (throw (ex-info "Output file exists" {:filename filename})))
  (spit filename contents))

(defn -main [& args]
  (log/debug "hello")
  (->> args
      (take (dec (count args)))
      (reduce #(concat %1 (reader %2)) [])
      (sort-cat)
      (format1)
      (write-to-file (last args)))
  0)
