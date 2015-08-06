package models

import org.bson.types.ObjectId
import play.api.mvc.{QueryStringBindable, PathBindable}

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
