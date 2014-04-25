.session <- new.env(parent=emptyenv())

LuceneObjects <- function(query){
  res <- .jcall(.session$lo, "S", "getResults", query)
  return(res)
}

LuceneIndexing <- function(data) {
  metadata <- .jarray(c(data$notebook_id, data$description, data$created_at, data$updated_at, data$content, data$starcount, data$avatar_url, data$user_url, data$commited_at, data$user))
  .jcall(.session$lo, "V", "indexing", metadata)
}


