package com.nexse.swat.springscala.tx.core

import org.springframework.transaction._
import org.springframework.transaction.HeuristicCompletionException._
import com.typesafe.scalalogging.slf4j.Logging

object ChainedTransactionManager {

  def apply(transactionManagers: PlatformTransactionManager*) =
    new ChainedTransactionManager(DefaultSynchronizationManager(), transactionManagers: _*)

  def apply() = new ChainedTransactionManager(DefaultSynchronizationManager())

}

class ChainedTransactionManager(val synchronizationManager: SynchronizationManager,
                                val transactionManagers: PlatformTransactionManager*)
  extends PlatformTransactionManager
  with Logging {

  def getTransaction(definition: TransactionDefinition) = {
    val mts = MultiTransactionStatus(transactionManagers(0) /*First TM is main TM*/)

    if (!synchronizationManager.isSynchronizationActive) {
      synchronizationManager.initSynchronization()
      mts.setNewSynchonization()
    }
    for (transactionManager <- transactionManagers) {
      mts.registerTransactionManager(definition, transactionManager)
    }
    mts
  }

  def commit(status: TransactionStatus) {
    val multiTransactionStatus = status.asInstanceOf[MultiTransactionStatus]
    var commit = true
    var commitException: Exception = null
    var commitExceptionTransactionManager: PlatformTransactionManager = null

    for (transactionManager <- transactionManagers.reverse) {
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
            logger.warn("Rollback exception (after commit) (" + transactionManager + ") " + ex.getMessage, ex)
        }
      }
    }

    if (multiTransactionStatus.isNewSynchonization()) {
      synchronizationManager.clearSynchronization()
    }

    if (commitException != null) {
      val transactionState = if (commitExceptionTransactionManager == getLastTransactionManager)
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

    for (transactionManager <- transactionManagers.reverse) {
      try {
        multiTransactionStatus.rollback(transactionManager)
      } catch {
        case ex: Exception => {
          if (rollbackException == null) {
            rollbackException = ex
            rollbackExceptionTransactionManager = transactionManager
          } else {
            logger.warn("Rollback exception (" + transactionManager + ") " + ex.getMessage, ex)
          }
        }
      }
    }

    if (multiTransactionStatus.isNewSynchonization()) {
      synchronizationManager.clearSynchronization()
    }

    if (rollbackException != null) {
      throw new UnexpectedRollbackException("Rollback exception, originated at (" + rollbackExceptionTransactionManager + ") " +
        rollbackException.getMessage, rollbackException)
    }
  }

  def getLastTransactionManager = transactionManagers.last

}
