package com.nexse.swat.springscala.tx.core

trait SynchronizationManager {

  def initSynchronization()

  def isSynchronizationActive: Boolean

  def clearSynchronization()

}

case class DefaultSynchronizationManager() extends SynchronizationManager {

  import org.springframework.transaction.support.{TransactionSynchronizationManager => TSM}
  import TSM.{initSynchronization => init, isSynchronizationActive => isActive, clear}

  def initSynchronization() {
    init
  }

  def isSynchronizationActive = isActive

  def clearSynchronization() {
    clear
  }

}