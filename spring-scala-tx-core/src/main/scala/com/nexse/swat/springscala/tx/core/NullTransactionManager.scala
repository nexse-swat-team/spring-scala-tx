package com.nexse.swat.springscala.tx.core

import javax.transaction.{Synchronization, Transaction, Status, TransactionManager}
import javax.transaction.xa.XAResource

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