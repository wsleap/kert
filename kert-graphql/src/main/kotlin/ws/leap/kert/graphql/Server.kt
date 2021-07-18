package ws.leap.kert.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelNames
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.hooks.NoopSchemaGeneratorHooks
import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import com.expediagroup.graphql.generator.toSchema
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.operations.Subscription
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import graphql.ExecutionInput.newExecutionInput
import graphql.GraphQL
import graphql.schema.GraphQLSchema
import io.vertx.core.http.HttpMethod
import kotlinx.coroutines.future.await
import ws.leap.kert.http.HttpRouterDsl
import ws.leap.kert.http.HttpServerBuilderDsl
import ws.leap.kert.http.response
import kotlin.coroutines.coroutineContext


fun HttpServerBuilderDsl.graphql(configure: GraphQlServerBuilder.() -> Unit) {
  router {
    val builder = GraphQlServerBuilder(this)
    configure(builder)
    builder.build()
  }
}

data class GraphqlRequest(
  val operationName: String? = null,
  val variables: Map<String, Any>? = null,
  val query: String
)

class ConfigBuilder() {
  var supportedPackages: List<String> = emptyList()
  var hooks: SchemaGeneratorHooks = NoopSchemaGeneratorHooks

  fun build(): SchemaGeneratorConfig {
    return SchemaGeneratorConfig(
      supportedPackages = supportedPackages,
      hooks = hooks,
      dataFetcherFactoryProvider = ContextDataFetcherFactoryProvider(),
    )
  }
}

class SchemaBuilder(private val mapper: ObjectMapper) {
  private val queries = mutableListOf<Query>()
  private val mutations = mutableListOf<Mutation>()
  private val subscriptions = mutableListOf<Subscription>()

  private val configBuilder = ConfigBuilder()
  fun config(configure: ConfigBuilder.() -> Unit) {
    configure(configBuilder)
  }

  fun query(query: Query) {
    queries.add(query)
  }

  fun mutation(mutation: Mutation) {
    mutations.add(mutation)
  }

  fun subscription(subscription: Subscription) {
    subscriptions.add(subscription)
  }

  fun build(): GraphQLSchema {
    return toSchema(
      configBuilder.build(),
      queries.map { TopLevelObject(it) },
      mutations.map { TopLevelObject(it) },
      subscriptions.map { TopLevelObject(it) },
    )
  }
}

class GraphQlServerBuilder(private val routerBuilder: HttpRouterDsl) {
  var endpoint: String = "/graphql"
  var playground: Boolean = false

  var mapper: ObjectMapper = jacksonMapperBuilder()
    .addModule(AfterburnerModule())
    .build()

  private var schemaConfigurator: SchemaBuilder.() -> Unit = {}

  fun schema(configure: SchemaBuilder.() -> Unit) {
    schemaConfigurator = configure
  }

  fun build() {
    val schemaBuilder = SchemaBuilder(mapper)
    schemaConfigurator(schemaBuilder)
    val graphql = GraphQL.newGraphQL(schemaBuilder.build()).build()

    routerBuilder.call(HttpMethod.POST, endpoint) { req ->
      val json = req.body().toString(Charsets.UTF_8)
      val request = mapper.readValue(json, GraphqlRequest::class.java)

      val input = newExecutionInput()
        .context(coroutineContext)
        .query(request.query)
        .operationName(request.operationName ?: "")
        .variables(request.variables ?: emptyMap())
        .build()

      val result = graphql.executeAsync(input).await()

      val responseBody = mapper.writeValueAsString(result.toSpecification())
      response(body = responseBody, contentType = "application/json")
    }

    if(playground) {
      routerBuilder.call(HttpMethod.GET, endpoint) {
        val playgroundHtml = GraphQlServerBuilder::class.java.classLoader.getResource("playground.html").readBytes()
        response(body = playgroundHtml, contentType = "text/html")
      }
    }
  }
}
