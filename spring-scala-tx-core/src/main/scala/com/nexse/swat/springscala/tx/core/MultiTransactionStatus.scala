package com.nexse.swat.springscala.tx.core

import org.springframework.transaction.{TransactionDefinition, PlatformTransactionManager, TransactionStatus}
import collection.mutable

case class MultiTransactionStatus(mainTransactionManager: PlatformTransactionManager) extends TransactionStatus {

  private val transactionStatuses = new mutable.HashMap[PlatformTransactionManager, TransactionStatus]
    with mutable.SynchronizedMap[PlatformTransactionManager, TransactionStatus]

  private var newSynchonization: Boolean = _

  def mainTransactionStatus = transactionStatuses(mainTransactionManager)

  def setNewSynchonization() {
    newSynchonization = true
  }

  def isNewSynchonization() = newSynchonization

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
    val savepoints = new mutable.HashMap[TransactionStatus, Object]

    def addSavePoint(status: TransactionStatus, savepoint: Object) {
      savepoints.put(status, savepoint)
    }

    def save(transactionStatus: TransactionStatus) {
      val savepoint = transactionStatus.createSavepoint()
      addSavePoint(transactionStatus, savepoint)
    }

    def rollback() {
      for (transactionStatus <- savepoints.keySet) {
        transactionStatus.rollbackToSavepoint(savepointFor(transactionStatus))
      }
    }

    def savepointFor(transactionStatus: TransactionStatus) = savepoints.get(transactionStatus)

    def release() {
      for (transactionStatus <- savepoints.keySet) {
        transactionStatus.releaseSavepoint(savepointFor(transactionStatus))
      }
    }

  }

  def createSavepoint() = {
    val savePoints = SavePoints()
    for (transactionStatus <- transactionStatuses.values) {
      savePoints.save(transactionStatus)
    }
    savePoints
  }

  def rollbackToSavepoint(savepoint: Object) {
    savepoint.asInstanceOf[SavePoints].rollback()
  }

  def releaseSavepoint(savepoint: Object) {
    savepoint.asInstanceOf[SavePoints].release()
  }

  def registerTransactionManager(definition: TransactionDefinition, transactionManager: PlatformTransactionManager) {
    transactionStatuses.put(transactionManager, transactionManager.getTransaction(definition))
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
