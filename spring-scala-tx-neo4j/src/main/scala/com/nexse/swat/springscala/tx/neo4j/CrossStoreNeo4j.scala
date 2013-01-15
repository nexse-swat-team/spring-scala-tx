package com.nexse.swat.springscala.tx.neo4j

import org.springframework.context.annotation.{Bean, Configuration}
import javax.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import beans.BeanProperty
import com.nexse.swat.springscala.tx.core.ChainedTransactionManager
import org.springframework.orm.jpa.JpaTransactionManager
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.impl.transaction.{UserTransactionImpl, SpringTransactionManager}
import org.springframework.transaction.jta.{UserTransactionAdapter, JtaTransactionManager}
import org.neo4j.kernel.GraphDatabaseAPI
import javax.transaction.{Synchronization, Status, Transaction, TransactionManager}
import javax.transaction.xa.XAResource

abstract class CrossStoreNeo4j {

  def ds: GraphDatabaseService

  @Qualifier("&entityManagerFactory")
  @Autowired(required = false)
  @BeanProperty
  var entityManagerFactory: EntityManagerFactory = _

  @Bean
  def neo4jTransactionManager =
    ChainedTransactionManager(new JpaTransactionManager(getEntityManagerFactory()), jtaTXManager)

  private val jtaTXManager = ds match {
    case gdapi: GraphDatabaseAPI => {
      val transactionManager = new SpringTransactionManager(gdapi)
      val userTransaction = new UserTransactionImpl(gdapi)
      new JtaTransactionManager(userTransaction, transactionManager)
    }
    case _ =>
      new JtaTransactionManager(new UserTransactionAdapter(NullTransactionManager), NullTransactionManager)
  }

}

object NullTransactionManager extends TransactionManager {

  import Status._

  val NULL_JAVA_TRANSACTION = new Transaction() {

    def commit() {}

    def delistResource(xaResource: XAResource, i: Int) = false

    def enlistResource(xaResource: XAResource) = false

    val getStatus = STATUS_NO_TRANSACTION

    def registerSynchronization(synchronization: Synchronization) {}

    def rollback() {}

    def setRollbackOnly() {}
  }

  def begin() {}

  def commit() {}

  val getStatus = STATUS_ACTIVE

  val getTransaction = NULL_JAVA_TRANSACTION

  def resume(transaction: Transaction) {}

  def rollback() {}

  def setRollbackOnly() {}

  def setTransactionTimeout(i: Int) {}

  val suspend = NULL_JAVA_TRANSACTION

}