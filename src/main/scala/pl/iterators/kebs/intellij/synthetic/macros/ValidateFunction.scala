package pl.iterators.kebs.intellij.synthetic.macros

import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.lang.psi.types.{ScParameterizedType, ScType}

case class ValidateFunction(errorType: ScType)

object ValidateFunction {

  def find(scObject: ScObject): Option[ValidateFunction] = {
    scObject.functions.find(_.name == "validate") match {
      case Some(validateFunction) =>
        for {
          scType <- validateFunction.returnType.toOption
          errorType <- leftFromEither(scType)
        } yield ValidateFunction(errorType)
      case None => None
    }
  }

  private def leftFromEither(scType: ScType) = scType match {
    case parameterizedType: ScParameterizedType if parameterizedType.designator.toString == "Either" && parameterizedType.typeArguments.size == 2 =>
      Some(parameterizedType.typeArguments.head)
    case _ => None
  }

}