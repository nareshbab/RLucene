.onLoad <- function(libname, pkgname) {
  .jpackage(pkgname, lib.loc=libname)
   path <- .jarray(c("/vagrant/work/nanda_indexes/","/vagrant/work/try3/"))
  .session$lo <- .jnew("Lucene", path)
}
