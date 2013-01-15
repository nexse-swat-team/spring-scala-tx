package com.nexse.swat.springscala.tx.neo4j

import org.springframework.context.annotation.{DependsOn, Bean}
import javax.persistence.EntityManagerFactory
import com.nexse.swat.springscala.tx.core.ChainedTransactionManager
import org.springframework.orm.jpa.JpaTransactionManager
import org.neo4j.kernel.impl.transaction.{UserTransactionImpl, SpringTransactionManager}
import org.springframework.transaction.jta.{UserTransactionAdapter, JtaTransactionManager}
import org.neo4j.kernel.{EmbeddedGraphDatabase, GraphDatabaseAPI}
import javax.transaction.{Synchronization, Status, Transaction, TransactionManager}
import javax.transaction.xa.XAResource
import sys.ShutdownHookThread

abstract class CrossStoreNeo4jConfiguration {

  @Bean
  def graphDB = {
    val ds = new EmbeddedGraphDatabase("pippo")
    ShutdownHookThread {
      ds.shutdown
    }
    ds
  }

  def emf: EntityManagerFactory

  @Bean // TODO: fix for no entity manager factory
  @DependsOn(Array("graphDB", "entityManagerFactory"))
  def neo4jTransactionManager =
    ChainedTransactionManager(new JpaTransactionManager(emf), jtaTXManager)

  private def jtaTXManager = graphDB match {
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