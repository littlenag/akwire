package services

import javax.inject.{Named, Singleton}
import java.util.UUID

/**
 * A type declaring the interface that will be injectable.
 */
abstract class UUIDGenerator() {
  def generate: UUID
}

/**
 * A simple implementation of UUIDGenerator that we will inject.
 */
//@Singleton
//@org.springframework.stereotype.Service
@Named
class SimpleUUIDGenerator extends UUIDGenerator {
  def generate: UUID = UUID.randomUUID()
}