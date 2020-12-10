package ws.leap.kert.http

typealias Handler<REQ, RESP> = suspend (req: REQ) -> RESP
typealias Filter<REQ, RESP> = suspend (req: REQ, next: Handler<REQ, RESP>) -> RESP

fun <REQ, RESP> withFilters(handler: Handler<REQ, RESP>, vararg filters: Filter<REQ, RESP>): Handler<REQ, RESP> {
  if(filters.isEmpty()) return handler

  val combinedFilter = combineFilters(*filters)!!
  return { req: REQ ->
    combinedFilter(req, handler)
  }
}

fun <REQ, RESP> Handler<REQ, RESP>.filters(vararg filters: Filter<REQ, RESP>): Handler<REQ, RESP> {
  return withFilters(this, *filters)
}

fun <REQ, RESP> filtered(handler: Handler<REQ, RESP>, filter: Filter<REQ, RESP>): Handler<REQ, RESP> {
  return { req: REQ ->
    filter(req, handler)
  }
}

fun <REQ, RESP> Handler<REQ, RESP>.filter(filter: Filter<REQ, RESP>): Handler<REQ, RESP> {
  return filtered(this, filter)
}

/**
 * Combine the [filters] into one filter, with the order of inner to outer (last filter get called first).
 */
fun <REQ, RESP> combineFilters(vararg filters: Filter<REQ, RESP>): Filter<REQ, RESP>? {
  if (filters.isEmpty()) return null

  return filters.reduce { left, right ->
    { req, next ->
      right(req) { left(it, next) }
    }
  }
}

/**
 * Combine filters by wrapping the [filter] on the [current] filter, which means the [filter] get called first, then the [current] filter.
 */
fun <REQ, RESP> combineFilters(current: Filter<REQ, RESP>?, filter: Filter<REQ, RESP>?): Filter<REQ, RESP>? {
  return current?.let { cur ->
    if (filter != null) {
      { req, next -> filter(req) { cur(it, next) } }
    } else {
      cur
    }
  } ?: filter
}

internal suspend fun <REQ, RESP> handle(req: REQ, handler: Handler<REQ, RESP>, filter: Filter<REQ, RESP>?): RESP {
  return filter?.let { it(req, handler) } ?: handler(req)
}

typealias HttpClientHandler = Handler<HttpClientRequest, HttpClientResponse>
typealias HttpClientFilter = Filter<HttpClientRequest, HttpClientResponse>

typealias HttpServerHandler = Handler<HttpServerRequest, HttpServerResponse>
typealias HttpServerFilter = Filter<HttpServerRequest, HttpServerResponse>
