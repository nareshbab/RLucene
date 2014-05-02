.session <- new.env(parent=emptyenv())

create.lucene.index <- function(path) {
  .session$lo <- .jnew("Lucene", path)
}

load.lucene.index <- function(path) {
  .session$lo <- .jnew("Lucene", path)
}

index.search <- function(query){
  res <- .jcall(.session$lo, "S", "getResults", query)
  return(res)
}

index.document <- function(path, data) {
  metadata <- .jarray(c(data$notebook_id, data$description, data$created_at, data$updated_at, data$content, data$starcount, data$avatar_url, data$user_url, data$commited_at, data$user))
  .jcall(.session$lo, "V", "indexing", path, metadata)
}

delete.lucene.index <- function(path, field, value, ...) {
  if (missing(field) && missing(value)) {
    .jcall(.session$lo, "V", "deleteindexes", path)
  }	else {
    data <- .jarray(c(field, value))
	.jcall(.session$lo, "V", "deleteindexes", path, data)
  }
}




