.onLoad <- function(libname, pkgname) {
  .jpackage(pkgname, lib.loc=libname)
   path <- .jarray(c("/vagrant/work/nanda_indexes","/vagrant/work/new"))
  .session$lo <- .jnew("Lucene", path)
}
