package models

import com.novus.salat.{TypeHintFrequency, StringTypeHintStrategy, Context}
import play.api.Play
import play.api.Play.current

/**
 * Adding a custom Salat context to work with Play's Classloader
 * Using example from: https://github.com/leon/play-salat/blob/master/sample/app/models/mongoContext.scala
*/
package object mongoContext {
  implicit val ctx : Context = {

    val context = new Context {
      val name = "global"
      override val typeHintStrategy = StringTypeHintStrategy(when = TypeHintFrequency.WhenNecessary, typeHint = "_t")
    }

    context.registerGlobalKeyOverride(remapThis = "id", toThisInstead = "_id")
    context.registerClassLoader(Play.classloader)

    // Only works because the registered data types are case classes
    context.registerCustomTransformer(ContextualizedStreamTransformer)
    context.registerCustomTransformer(RuleBuilderClassTransformer)
    context.registerCustomTransformer(StreamExprTransformer)

    context
  }
}