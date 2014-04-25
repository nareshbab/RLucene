.onLoad <- function(libname, pkgname) {
  .jpackage(pkgname, lib.loc=libname)
   path <- .jarray(c("<path to load indexes to RAM>","<path to create indexes>"))
  .session$lo <- .jnew("Lucene", path)
}
