Concatenates sheets of several Excel workbooks.

Usage:

```console
$ mvn compile
$ mvn clojure:run \
-Dclojure.mainClass=sheetcat.main \
-Dclojure.args="file1.xlsx file2.xlsx outputfile"
$ head outputfile
...
```