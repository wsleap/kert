package ws.leap.kert.http

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.net.URL

class HttpClientSpec : FunSpec() {
  private val client = client()
  init {
    test("call with url") {
      val resp = client.get(URL("https://www.google.com"))
      resp.statusCode shouldBe 200
    }
  }
}
