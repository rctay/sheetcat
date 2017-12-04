(ns sheetcat.core)

(defn smooth-cells-seq [row]
  (let [cells (-> row (. cellIterator) (iterator-seq))]
    (loop [cells cells i 0 result []]
      (if (empty? cells)
        result
        (let [cell (first cells)
              j (. cell getColumnIndex)
              needs-skip
                (condp #(%1 i %2) j
                  > (throw (ex-info "getting previous columns" {:expected i :actual j :cell cell}))
                  = false
                  < true)]
          (if needs-skip
            (recur cells (inc i) (conj result nil))
            (recur (rest cells) (inc i) (conj result cell))))))))
