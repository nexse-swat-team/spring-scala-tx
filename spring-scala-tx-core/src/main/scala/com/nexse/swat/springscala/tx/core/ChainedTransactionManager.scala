package com.nexse.swat.springscala.tx.core

import org.springframework.transaction._
import org.springframework.transaction.HeuristicCompletionException._
import com.typesafe.scalalogging.slf4j.Logging

object ChainedTransactionManager {

  def apply(transactionManagers: PlatformTransactionManager*) =
    new ChainedTransactionManager(DefaultSynchronizationManager(), transactionManagers: _*)

  //def apply() = new ChainedTransactionManager(DefaultSynchronizationManager())
}

case class ChainedTransactionManager(synchronizationManager: SynchronizationManager,
                                     transactionManagers: PlatformTransactionManager*)
  extends PlatformTransactionManager
  with Logging {

  private val reversedTM = transactionManagers.reverse

  private val lastTM = reversedTM.head

  def getTransaction(definition: TransactionDefinition) = MultiTransactionStatus(
    if (!synchronizationManager.isSynchronizationActive) {
      synchronizationManager.initSynchronization()
      true
    } else false,
    definition,
    transactionManagers: _*
  )

  def commit(status: TransactionStatus) {
    val multiTransactionStatus = status.asInstanceOf[MultiTransactionStatus]
    var commit = true
    var commitException: Exception = null
    var commitExceptionTransactionManager: PlatformTransactionManager = null

    for (transactionManager <- reversedTM) {
      if (commit) {
        try {
          multiTransactionStatus.commit(transactionManager)
        } catch {
          case ex: Exception => {
            commit = false
            commitException = ex
            commitExceptionTransactionManager = transactionManager
          }
        }
      } else {
        //after unsuccessful commit we must try to rollback remaining transaction managers
        try {
          multiTransactionStatus.rollback(transactionManager)
        } catch {
          case ex: Exception =>
            logger.warn("Rollback exception (after commit) ($transactionManager): ${ex.getMessage}", ex)
        }
      }
    }

    if (multiTransactionStatus.newSync) {
      synchronizationManager.clearSynchronization()
    }

    if (commitException != null) {
      val transactionState = if (commitExceptionTransactionManager == lastTM)
        STATE_ROLLED_BACK
      else
        STATE_MIXED
      throw new HeuristicCompletionException(transactionState, commitException)
    }

  }

  def rollback(status: TransactionStatus) {
    val multiTransactionStatus = status.asInstanceOf[MultiTransactionStatus]
    var rollbackException: Exception = null
    var rollbackExceptionTransactionManager: PlatformTransactionManager = null

    for (transactionManager <- reversedTM) {
      try {
        multiTransactionStatus.rollback(transactionManager)
      } catch {
        case ex: Exception => {
          if (rollbackException == null) {
            rollbackException = ex
            rollbackExceptionTransactionManager = transactionManager
          } else {
            logger.warn(s"Rollback exception ($transactionManager): ${ex.getMessage}", ex)
          }
        }
      }
    }

    if (multiTransactionStatus.newSync) {
      synchronizationManager.clearSynchronization()
    }

    if (rollbackException != null) {
      throw new UnexpectedRollbackException(
        s"Rollback exception, originated at ($rollbackExceptionTransactionManager): ${rollbackException.getMessage}",
        rollbackException
      )
    }
  }

}