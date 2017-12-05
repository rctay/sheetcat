Concatenates sheets of several Excel workbooks.

- Apache POI: for parsing Excel
- written in Clojure
- ~~[algo.monads](https://github.com/clojure/algo.monads)~~ [![Clojars Project](https://img.shields.io/clojars/v/failjure.svg)](https://clojars.org/failjure)
- [clojure/tools.logging](https://github.com/clojure/tools.logging)
- [talios' Clojure Maven plugin](https://github.com/talios/clojure-maven-plugin)

Usage:

```console
$ mvn compile
$ mvn clojure:run \
-Dclojure.mainClass=sheetcat.main \
-Dclojure.args="file1.xlsx file2.xlsx outputfile"
$ head outputfile
...
```