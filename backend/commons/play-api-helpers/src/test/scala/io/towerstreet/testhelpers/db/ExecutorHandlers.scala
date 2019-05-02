package io.towerstreet.testhelpers.db

import acolyte.jdbc.AcolyteDSL.handleStatement
import acolyte.jdbc._

/**
  * Defines set of helper objects to be used in pattern matching to produce acolyte handlers with
  * less boilerplate code.
  *
  * Usage:
  *
  * private lazy val handler = Handler("^select ") {
  *   // Match select query with parameters
  *   case ~(Params("""^select .* from my_table"""), ("param_01", "param_02")) => QueryResult.Nil
  *   // Match select query and ignore parameters
  *   case ~(Query("""^select .* from my_table""")) => results
  *   // Match insert query and obtain single parameter
  *   case ~(Params("""^insert into from my_table"""), p) => ResultHelpers.parameterKeyResult(v)
  * }
  *
  *
  * Previous code is equivalent to this acolyte code:
  *
  * lazy val handler = handleStatement
  *   .withQueryDetection("^select ")
  *   .withQueryHandler {
  *     // Match select query with parameters
  *     case ~(ExecutedStatement("""^select .* from my_table""")
  *     , (_, ExecutedParameter("param_01") :: ExecutedParameter("param_02") :: Nil)) => QueryResult.Nil
  *
  *     // Match select query and ignore parameters
  *     case ~(ExecutedStatement("""^select .* from my_table""")
  *     , (_, _)) => results
  *   }
  *   .withUpdateHandler {
  *     // Match insert query and obtain single parameter
  *     case ~(ExecutedStatement("""^insert into from my_table""")
  *     , (_, ExecutedParameter(v) :: Nil)) => ResultHelpers.parameterKeyResult(v)
  *   }
  *
  **/
object ExecutorHandlers {

  /**
    * Shortcut for acolyte handleStatement object. Allows to use single partial function for both
    * query and update handlers. Method will pick the correct case for proper result.
    */
  object Handler
  {
    def apply(queryDetection: String*)
             (handler: PartialFunction[Execution, Result[_]]): ScalaCompositeHandler =
      handleStatement
        .withQueryDetection(queryDetection:_*)
        .withQueryHandler(handler andThen {
          case r: QueryResult => r
        })
        .withUpdateHandler(handler andThen {
          case r: UpdateResult => r
        })
  }

  /**
    * Shortcut for query mapper ignoring parameters.
    */
  case class Query(matchedString: String) {
    def unapplySeq(arg: Execution): Option[Seq[Any]] = {
      val matcher = ExecutedStatement(matchedString)

      arg match {
        case matcher(_, _) ⇒ Some(Seq())
        case _ => None
      }
    }
  }

  /**
    * Shortcut for query mapper with parameters
    */
  case class Param(matchedString: String) {
    def unapplySeq(arg: Execution): Option[Seq[Any]] = {
      val matcher = ExecutedStatement(matchedString)

      arg match {
        case matcher(_, params) ⇒ Some(params.map(_.value))
        case _ => None
      }
    }
  }

  /**
    * Empty extractor definition to get rid of IDEA compilation error.
    *
    * Symbol ~ is provided by acolyte scalac compilation plugin to add regex support for mocking database
    * queries. However IDEA can't resolve it and keeps produce compilation errors and tries to suggest imports.
    * To get rid of it there is this object which provides empty extractors.
    *
    * This object is actually never used - it is overridden by compiler.
   */
  object ~ {
    def unapplySeq(arg: Execution): Option[Seq[Any]] = None
  }
}
