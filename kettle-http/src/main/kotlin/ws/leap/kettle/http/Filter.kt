package ws.leap.kettle.http

interface Filter<REQ, RESP> {
  suspend fun invoke(req: REQ): RESP
}

abstract class FilterImpl<REQ, RESP>(private val next: Filter<REQ, RESP>) : Filter<REQ, RESP> {
}
