package services

import javax.inject.Named

import akka.actor.{Props, Actor}
import play.api.Play

import org.slf4j.{LoggerFactory, Logger}
import org.springframework.beans.factory.annotation.Autowired
import scala.beans.BeanProperty
import reactivemongo.api.DefaultDB

@Named
class Dao {
  private final val logger: Logger = LoggerFactory.getLogger(classOf[Dao])

  // Handles all the database access for other services
  @Autowired
  @BeanProperty
  var db : DefaultDB = null;

  // users
  // roles
  // agents
  //  - instances of collectors
  // streams

  // collectors


}
