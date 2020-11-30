package ws.leap.kert.http

import ws.leap.kert.core.Filter
import ws.leap.kert.core.Handler

typealias HttpServerHandler = Handler<HttpServerRequest, HttpServerResponse>
typealias HttpServerFilter = Filter<HttpServerRequest, HttpServerResponse>
