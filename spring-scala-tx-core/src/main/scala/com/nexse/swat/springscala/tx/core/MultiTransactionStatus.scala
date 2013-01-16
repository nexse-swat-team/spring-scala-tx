package com.nexse.swat.springscala.tx.core

import org.springframework.transaction.{TransactionDefinition, PlatformTransactionManager, TransactionStatus}

case class MultiTransactionStatus(newSync: Boolean,
                                  definition: TransactionDefinition,
                                  transactionManagers: PlatformTransactionManager*)
  extends TransactionStatus {

  private val transactionStatuses = {
    val txStatuses = for {
      transactionManager <- transactionManagers
    } yield transactionManager -> transactionManager.getTransaction(definition)

    txStatuses.toMap
  }

  /** First TM is main TM */
  val mainTransactionStatus = transactionStatuses(transactionManagers(0))

  def isNewTransaction() = mainTransactionStatus.isNewTransaction()

  def hasSavepoint() = mainTransactionStatus.hasSavepoint()

  def setRollbackOnly() {
    for (ts <- transactionStatuses.values) {
      ts.setRollbackOnly()
    }
  }

  def isRollbackOnly() = mainTransactionStatus.isRollbackOnly()

  def isCompleted() = mainTransactionStatus.isCompleted()

  case class SavePoints() {

    private val savePoints = for {
      transactionStatus <- transactionStatuses.values
    } yield transactionStatus -> transactionStatus.createSavepoint()

    def rollback() {
      savePoints foreach {
        sp => sp._1.rollbackToSavepoint(sp._2)
      }
    }

    def release() {
      savePoints foreach {
        sp => sp._1.releaseSavepoint(sp._2)
      }
    }

  }

  def createSavepoint() = SavePoints()

  private def maybeSavePoints(savepoint: AnyRef) = savepoint match {
    case sp: SavePoints => Some(sp)
    case _ => None
  }

  def rollbackToSavepoint(savepoint: Object) {
    maybeSavePoints(savepoint) foreach (_.rollback())
  }

  def releaseSavepoint(savepoint: Object) {
    maybeSavePoints(savepoint) foreach (_.release())
  }

  def commit(transactionManager: PlatformTransactionManager) {
    transactionManager.commit(transactionStatuses(transactionManager))
  }

  def rollback(transactionManager: PlatformTransactionManager) {
    transactionManager.rollback(transactionStatuses(transactionManager))
  }

  def flush() {
    for (transactionStatus <- transactionStatuses.values) {
      transactionStatus.flush()
    }
  }

}