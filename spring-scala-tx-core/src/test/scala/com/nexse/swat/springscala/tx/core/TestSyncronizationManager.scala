package com.nexse.swat.springscala.tx.core

class TestSynchronizationManager extends SynchronizationManager {

  private var synchronizationActive: Boolean = _

  def initSynchronization() {
    synchronizationActive = true
  }

  def isSynchronizationActive() = synchronizationActive

  def clearSynchronization() {
    synchronizationActive = false
  }

}
