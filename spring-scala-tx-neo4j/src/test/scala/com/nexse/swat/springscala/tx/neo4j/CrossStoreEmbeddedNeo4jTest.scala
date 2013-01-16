package com.nexse.swat.springscala.tx.neo4j

import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.ContextConfiguration
import org.junit.Assert._
import org.junit.Test
import javax.persistence.{EntityManager, PersistenceContext}
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.neo4j.graphdb.GraphDatabaseService
import org.springframework.test.annotation.Rollback

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[CrossStoreEmbeddedNeo4jConfiguration]))
class CrossStoreEmbeddedNeo4jTest {

  @PersistenceContext
  var em: EntityManager = _

  @Autowired
  var graphDB: GraphDatabaseService = _

  @Test
  @Transactional
  @Rollback(false)
  def firstTest() {
    assertNotNull(em)
    assertNotNull(graphDB)
    val anEntity = new AnEntity
    anEntity.name = "aName"
    em.persist(anEntity)
    assertNotNull(anEntity.id)
    val foundEntity = em.find(classOf[AnEntity], anEntity.id)
    assertNotNull(foundEntity)
    assertEquals(anEntity.name, foundEntity.name)

    val node = graphDB.createNode()
    assertNotNull(node)
    node.setProperty("publicId", anEntity.id)
  }

}