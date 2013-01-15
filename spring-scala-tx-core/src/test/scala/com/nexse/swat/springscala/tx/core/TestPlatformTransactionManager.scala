package com.nexse.swat.springscala.tx.core

import org.springframework.transaction.{TransactionDefinition, TransactionStatus, PlatformTransactionManager}

object TestPlatformTransactionManager {

  def createFailingTransactionManager(name: String) = new TestPlatformTransactionManager(name + "-failing") {
    override def commit(status: TransactionStatus) {
      throw new RuntimeException
    }

    override def rollback(status: TransactionStatus) {
      throw new RuntimeException
    }
  }

  def createNonFailingTransactionManager(name: String) = new TestPlatformTransactionManager(name + "-non-failing")

}

class TestPlatformTransactionManager(name: String) extends PlatformTransactionManager {

  var commitTime: Option[Long] = None
  var rollbackTime: Option[Long] = None

  override def toString = if (isCommitted) s"$name (committed) " else s"$name (not committed)"

  def getTransaction(definition: TransactionDefinition) = new TestTransactionStatus(definition)

  def commit(status: TransactionStatus) {
    commitTime = Some(System.currentTimeMillis())
  }

  def rollback(status: TransactionStatus) {
    rollbackTime = Some(System.currentTimeMillis())
  }

  def isCommitted = commitTime.isDefined

  def wasRolledBack = rollbackTime.isDefined

  case class TestTransactionStatus(definition: TransactionDefinition) extends TransactionStatus {

    val isNewTransaction = false

    val hasSavepoint = false

    def setRollbackOnly() {}

    val isRollbackOnly = false

    def flush() {}

    val isCompleted = false

    def createSavepoint() = null

    def rollbackToSavepoint(savepoint: Object) {}

    def releaseSavepoint(savepoint: Object) {}

  }

}
