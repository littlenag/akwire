package models

import com.mongodb.casbah.Imports._
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

trait EntityScoped {
  def id : ObjectId
  def scope : Scope.Value
  final def asRef = OwningEntityRef(id,scope)
}

case class OwningEntityRef(id: ObjectId, scope: Scope.Value) extends EntityScoped
