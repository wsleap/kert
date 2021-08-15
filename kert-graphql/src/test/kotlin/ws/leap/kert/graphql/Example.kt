package ws.leap.kert.graphql

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import kotlinx.coroutines.runBlocking
import ws.leap.kert.http.httpServer

enum class Gender {
  MALE,
  FEMALE
}

class StudentQuery : Query {
  companion object {
    val students = mapOf(
      "andy" to Student("andy", "Andy", 12, Gender.MALE),
      "john" to Student("john", "John", 13, Gender.MALE),
      "mary" to Student("mary", "Mary", 11, Gender.FEMALE),
      "lucy" to Student("lucy", "Lucy", 14, Gender.FEMALE),
      "mike" to Student("mike", "Mike", 12, Gender.MALE),
    )
    val friendships = mapOf(
      "andy" to setOf("mary", "john"),
      "john" to setOf("mary", "lucy"),
      "mary" to setOf("andy", "john", "lucy"),
      "lucy" to setOf("mike"),
      "mike" to setOf("andy", "john")
    )
  }

  data class AddressFormat(val showZipcode: Boolean? = true, val showState: Boolean? = true)

  data class Student(private val id: String, val name: String, val age: Int, val gender: Gender) {
    @Suppress("unused")
    suspend fun address(format: AddressFormat? = AddressFormat()): String {
      val showZipcode = format?.showZipcode ?: true
      val showState = format?.showState ?: true

      return "$name's address, ${if(showState) "CA" else ""}, ${if(showZipcode) "94555" else ""}"
    }

    fun friends(): List<Student> {
      return friendships[id]!!.map { students[it]!! }.toList()
    }
  }

  @GraphQLDescription("Return students")
  suspend fun students(
    @GraphQLDescription("Limit of the result") limit: Int = 10
  ): List<Student> {
    return students.values.take(limit)
  }
}

fun main() {
  val server = httpServer(8500) {
    graphql {
      playground = true

      schema {
        config {
          supportedPackages = listOf("ws.leap.kert.graphql")
        }
        query(StudentQuery())
      }
    }
  }

  runBlocking {
    server.start()
  }
}
