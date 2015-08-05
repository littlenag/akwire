package models

import org.bson.types.ObjectId
import play.api.mvc.PathBindable

import scala.util.Try

// Things like Rules, Incidents, Policies, etc may be owned by
//   - Enterprise
//   - Team
//   - User
// We keep track of that via an entities "scope" and owner id.

object Scope extends Enumeration {
  type Scope = Value
  val USER, TEAM, SERVICE = Value
}

case class OwningEntity(id: ObjectId, scope: Scope.Value)

sealed trait EntityScoped {
  def id : ObjectId
  def scope : Scope.Value
}

case class UserScoped(id: ObjectId) extends EntityScoped {
  val scope = Scope.USER
}

case class TeamScoped(id: ObjectId) extends EntityScoped {
  val scope = Scope.TEAM
}

case class ServiceScoped(id: ObjectId) extends EntityScoped {
  val scope = Scope.SERVICE
}

object OwningEntity {

  // Assume that format is: SCOPE-ID, e.g. TEAM-00001234123412341234
  implicit def pathBinder(implicit stringBinder: PathBindable[String]) = new PathBindable[OwningEntity] {

    override def bind(key: String, value: String): Either[String, OwningEntity] = {
      for {
        scopeAndId <- stringBinder.bind(key, value).right
        owningEntity <- splitSegment(scopeAndId).toRight("Unable to parse").right
      } yield owningEntity
    }

    override def unbind(key: String, entity: OwningEntity): String = {
      stringBinder.unbind(key, entity.scope + "-" + entity.id)
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
  }
}