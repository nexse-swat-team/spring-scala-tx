package com.nexse.swat.springscala.tx.core

import org.springframework.transaction.support.TransactionSynchronizationManager

trait SynchronizationManager {

  def initSynchronization()

  def isSynchronizationActive: Boolean

  def clearSynchronization()

}

case class DefaultSynchronizationManager() extends SynchronizationManager {

  def initSynchronization() {
    TransactionSynchronizationManager.initSynchronization()
  }

  def isSynchronizationActive = TransactionSynchronizationManager.isSynchronizationActive()

  def clearSynchronization() {
    TransactionSynchronizationManager.clear()
  }

}
