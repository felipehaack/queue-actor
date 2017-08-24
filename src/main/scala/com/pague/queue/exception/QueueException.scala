package com.pague.queue.exception

import scala.reflect.ClassTag
import scala.util.control.NoStackTrace

class QueueException(val message: String) extends RuntimeException(message)

object QueueException {

  def systemError(desc: String) = throw new QueueException(desc)

  def notFound[T: ClassTag] = throw new NotFoundFailure(getSimpleName[T])

  def invalidId[T: ClassTag] = throw new InvalidIdFailure(getSimpleName[T])

  def invalidInput(desc: String) = throw new InvalidInputFailure(desc)

  def invalidInputs(errors: Set[Error]) = throw new InvalidInputsFailure(errors)

  def invalid(name: String) = throw new InvalidFailure(name)

  private def getSimpleName[T](implicit ct: ClassTag[T]): String = {
    ct.runtimeClass.getSimpleName
  }
}

class QueueFailure(val code: String) extends QueueException(code) with NoStackTrace

class NotFoundFailure(name: String) extends QueueFailure(s"not.found.${name}")

class InvalidFailure(name: String) extends QueueFailure(s"invalid.${name}")

class InvalidIdFailure(name: String) extends InvalidFailure(s"id.${name}")

class InvalidInputFailure(name: String) extends InvalidFailure(name)

class InvalidInputsFailure(val errors: Set[Error]) extends InvalidFailure("input")

class UpdateFailure(val name: String) extends InvalidFailure(s"update.$name")

case object NothingToDoException extends QueueException(s"There is nothing to do in current state.")

