package ws.leap.kert.graphql

import com.expediagroup.graphql.generator.execution.FunctionDataFetcher
import com.expediagroup.graphql.generator.execution.SimpleKotlinDataFetcherFactoryProvider
import graphql.schema.DataFetcherFactory
import graphql.schema.DataFetchingEnvironment
import kotlin.reflect.KFunction
import kotlin.reflect.full.instanceParameter

open class ContextDataFetcherFactoryProvider : SimpleKotlinDataFetcherFactoryProvider() {

  override fun functionDataFetcherFactory(target: Any?, kFunction: KFunction<*>) = DataFetcherFactory {
    ContextFunctionDataFetcher(
      target = target,
      fn = kFunction
    )
  }
}

class ContextFunctionDataFetcher(
  private val target: Any?,
  private val fn: KFunction<*>
) : FunctionDataFetcher(target, fn) {
  override fun get(environment: DataFetchingEnvironment): Any? {
    val instance: Any? = target ?: environment.getSource<Any?>()
    val instanceParameter = fn.instanceParameter

    return if (instance != null && instanceParameter != null) {
      val parameterValues = getParameters(fn, environment)
        .plus(instanceParameter to instance)

      if (fn.isSuspend) {
        runSuspendingFunction(environment, parameterValues)
      } else {
        runBlockingFunction(parameterValues)
      }
    } else {
      null
    }
  }
}
