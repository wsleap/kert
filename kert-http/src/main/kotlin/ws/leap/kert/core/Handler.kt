package ws.leap.kert.core

typealias Handler<REQ, RESP> = suspend (req: REQ) -> RESP
typealias Filter<REQ, RESP> = suspend (req: REQ, next: Handler<REQ, RESP>) -> RESP

fun <REQ, RESP> filtered(handler: Handler<REQ, RESP>, vararg filters: Filter<REQ, RESP>): Handler<REQ, RESP> {
  if(filters.isEmpty()) return handler

  val combinedFilter = combine(*filters)!!
  return { req: REQ ->
    combinedFilter(req, handler)
  }
}

fun <REQ, RESP> Handler<REQ, RESP>.filters(vararg filters: Filter<REQ, RESP>): Handler<REQ, RESP> {
  return filtered(this, *filters)
}

fun <REQ, RESP> filtered(handler: Handler<REQ, RESP>, filter: Filter<REQ, RESP>): Handler<REQ, RESP> {
  return { req: REQ ->
    filter(req, handler)
  }
}

fun <REQ, RESP> Handler<REQ, RESP>.filter(filter: Filter<REQ, RESP>): Handler<REQ, RESP> {
  return filtered(this, filter)
}

fun <REQ, RESP> combine(vararg filters: Filter<REQ, RESP>): Filter<REQ, RESP>? {
  var combinedFilter: Filter<REQ, RESP>? = null
  filters.map { filter ->
    combinedFilter = combinedFilter?.let { current ->
      { req, next ->
        current(req) { filter(it, next) }
      }
    } ?: filter
  }
  return combinedFilter
}

fun <REQ, RESP> combine(current: Filter<REQ, RESP>?, filter: Filter<REQ, RESP>): Filter<REQ, RESP>? {
  return current?.let { cur ->
    { req, next -> cur(req) { filter(it, next) } }
  } ?: filter
}

suspend fun <REQ, RESP> handle(req: REQ, handler: Handler<REQ, RESP>, filter: Filter<REQ, RESP>?): RESP {
  return filter?.let { it(req, handler) } ?: handler(req)
}
