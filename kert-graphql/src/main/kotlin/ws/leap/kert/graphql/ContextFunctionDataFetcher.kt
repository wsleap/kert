package ws.leap.kert.graphql

import com.expediagroup.graphql.generator.execution.FunctionDataFetcher
import com.expediagroup.graphql.generator.execution.SimpleKotlinDataFetcherFactoryProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import graphql.schema.DataFetcherFactory
import graphql.schema.DataFetchingEnvironment
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.full.instanceParameter

open class ContextDataFetcherFactoryProvider(
  private val objectMapper: ObjectMapper = jacksonObjectMapper()
) : SimpleKotlinDataFetcherFactoryProvider(objectMapper) {

  override fun functionDataFetcherFactory(target: Any?, kFunction: KFunction<*>) = DataFetcherFactory {
    ContextFunctionDataFetcher(
      target = target,
      fn = kFunction,
      objectMapper = objectMapper
    )
  }
}

class ContextFunctionDataFetcher(
  private val target: Any?,
  private val fn: KFunction<*>,
  objectMapper: ObjectMapper = jacksonObjectMapper()
) : FunctionDataFetcher(target, fn, objectMapper) {
  override fun get(environment: DataFetchingEnvironment): Any? {
    val instance: Any? = target ?: environment.getSource<Any?>()
    val instanceParameter = fn.instanceParameter

    return if (instance != null && instanceParameter != null) {
      val parameterValues = getParameters(fn, environment)
        .plus(instanceParameter to instance)

      if (fn.isSuspend) {
        val context: CoroutineContext = (environment.getContext<Any?>() as? CoroutineContext) ?: EmptyCoroutineContext
        runSuspendingFunction(parameterValues, context)
      } else {
        runBlockingFunction(parameterValues)
      }
    } else {
      null
    }
  }
}
