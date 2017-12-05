Con(cat)enates sheets of several Excel workbooks, at the same time validating
they have a common structure of:
- serial number (increasing integer)
- identifier (eg. SSN/NRIC)
- date (constant throughout)

Internals:
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