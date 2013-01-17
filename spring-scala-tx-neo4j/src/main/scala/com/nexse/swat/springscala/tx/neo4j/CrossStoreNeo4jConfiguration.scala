package com.nexse.swat.springscala.tx.neo4j

import org.springframework.context.annotation.Bean
import javax.persistence.EntityManagerFactory
import com.nexse.swat.springscala.tx.core.{NullTransactionManager, ChainedTransactionManager}
import org.springframework.orm.jpa.JpaTransactionManager
import org.neo4j.kernel.impl.transaction.{UserTransactionImpl, SpringTransactionManager}
import org.springframework.transaction.jta.{UserTransactionAdapter, JtaTransactionManager}
import org.neo4j.kernel.GraphDatabaseAPI
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.neo4j.graphdb.GraphDatabaseService

@Component
class CrossStoreNeo4jConfiguration @Autowired()(val graphDatabaseService: GraphDatabaseService,
                                                val entityManagerFactory: EntityManagerFactory) {

  @Bean
  def neo4jTransactionManager = ChainedTransactionManager(jpaTXManager, jtaTXManager)

  private val jpaTXManager = new JpaTransactionManager(entityManagerFactory)

  private val jtaTXManager = graphDatabaseService match {
    case gdAPI: GraphDatabaseAPI =>
      new JtaTransactionManager(new UserTransactionImpl(gdAPI), new SpringTransactionManager(gdAPI))
    case _ =>
      new JtaTransactionManager(new UserTransactionAdapter(NullTransactionManager), NullTransactionManager)
  }

}