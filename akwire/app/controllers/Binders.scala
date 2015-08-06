package controllers

import models.{Scope, OwningEntity}
import org.bson.types.ObjectId
import play.api.mvc.{QueryStringBindable, PathBindable}

import scala.util.Try

object Binders {
  // For MongoDB ObjectId's
  implicit def pathBinderForObjectId(implicit stringBinder: PathBindable[String]) = new PathBindable[ObjectId] {

    override def bind(key: String, value: String): Either[String, ObjectId] = {
      for {
        idStr <- stringBinder.bind(key, value).right
        id <- Try(new ObjectId(idStr)).toOption.toRight("Unable to parse").right
      } yield id
    }

    override def unbind(key: String, entity: ObjectId): String = {
      stringBinder.unbind(key, entity.toString)
    }
  }

  private def splitSegment(segment:String) : Option[OwningEntity] = {
    segment.split("-").toList match {
      case maybeScope :: maybeId :: Nil =>
        Try {
          val scope = Scope.withName(maybeScope.toUpperCase)
          val id = new ObjectId(maybeId)
          OwningEntity(id, scope)
        }.toOption
      case _ => None
    }
  }

  // Assume that format is: SCOPE-ID, e.g. TEAM-00001234123412341234
  implicit def pathBinderForScope(implicit stringBinder: PathBindable[String]) = new PathBindable[OwningEntity] {

    override def bind(key: String, value: String): Either[String, OwningEntity] = {
      for {
        scopeAndId <- stringBinder.bind(key, value).right
        owningEntity <- splitSegment(scopeAndId).toRight("Unable to parse").right
      } yield owningEntity
    }

    override def unbind(key: String, entity: OwningEntity): String = {
      stringBinder.unbind(key, entity.scope + "-" + entity.id)
    }
  }

  // Assume that format is: SCOPE-ID, e.g. TEAM-00001234123412341234
  implicit def queryBinderForScope(implicit stringBinder: QueryStringBindable[String]) = new QueryStringBindable[OwningEntity] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, OwningEntity]] = {
      for {
        maybeScopeAndId <- stringBinder.bind(key, params)
      } yield {
        maybeScopeAndId match {
          case Right(scopeAndId) => splitSegment(scopeAndId).toRight("Unable to parse")
          case _ => Left("Unable to parse")
        }
      }
    }

    override def unbind(key: String, entity: OwningEntity): String = {
      stringBinder.unbind(key, entity.scope + "-" + entity.id)
    }
  }
}
