package ws.leap.kert.http

import ws.leap.kert.core.Filter
import ws.leap.kert.core.Handler

typealias HttpClientHandler = Handler<HttpClientRequest, HttpClientResponse>
typealias HttpClientFilter = Filter<HttpClientRequest, HttpClientResponse>
