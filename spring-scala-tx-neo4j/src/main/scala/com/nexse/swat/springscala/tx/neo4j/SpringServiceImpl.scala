package com.nexse.swat.springscala.tx.neo4j

import org.springframework.beans.factory.annotation.{Autowired, Configurable}
import org.neo4j.kernel.impl.transaction.AbstractTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import javax.transaction.{Transaction, TransactionManager}
import org.springframework.transaction.jta.JtaTransactionManager

@Configurable
class SpringServiceImpl extends AbstractTransactionManager {

  @Autowired
  private var transactionManager: PlatformTransactionManager = _

  private var delegate: TransactionManager = _

  def init() {
    delegate = transactionManager match {
      case jtaTM: JtaTransactionManager => jtaTM.getTransactionManager
      case _ => throw new IllegalStateException(
        s"Injected transaction manager is not of type JtaTransactionManager but ${transactionManager.getClass.getName}"
      )
    }
  }

  def start() {}

  def shutdown() {}

  def begin() {
    delegate.begin()
  }

  def commit() {
    delegate.commit()
  }

  def getStatus() = delegate.getStatus()

  def getTransaction() = delegate.getTransaction()

  def resume(tobj: Transaction) {
    delegate.resume(tobj)
  }

  def rollback() {
    delegate.rollback()
  }

  def setRollbackOnly() {
    delegate.setRollbackOnly()
  }

  def setTransactionTimeout(seconds: Int) {
    delegate.setTransactionTimeout(seconds)
  }

  def suspend() = delegate.suspend()

  def stop() {
    // Currently a no-op
  }

  def getTransactionManager() = transactionManager

}
