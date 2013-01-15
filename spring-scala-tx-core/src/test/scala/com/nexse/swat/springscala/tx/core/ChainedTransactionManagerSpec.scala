package com.nexse.swat.springscala.tx.core

import com.nexse.swat.springscala.tx.core.TestPlatformTransactionManager._
import org.specs2.mutable.Specification
import org.springframework.transaction.{UnexpectedRollbackException, HeuristicCompletionException, PlatformTransactionManager}
import org.springframework.transaction.support.DefaultTransactionDefinition

class ChainedTransactionManagerSpec extends Specification {

  private def setupTransactionManagers(transactionManagers: PlatformTransactionManager*) =
    new ChainedTransactionManager(new TestSynchronizationManager(), transactionManagers: _*)

  private def createAndCommitTransaction(tm: PlatformTransactionManager) {
    tm.commit(tm.getTransaction(new DefaultTransactionDefinition()))
  }

  private def createAndRollbackTransaction(tm: PlatformTransactionManager) {
    tm.rollback(tm.getTransaction(new DefaultTransactionDefinition()))
  }

  "Non failing TM" should {

    "complete successfully" in {
      val transactionManager = createNonFailingTransactionManager("single")
      createAndCommitTransaction(setupTransactionManagers(transactionManager))
      transactionManager match {
        case tptm: TestPlatformTransactionManager if (tptm.isCommitted) => success
        case _ => failure
      }
    }

    "commit all registered TM" in {
      val first = createNonFailingTransactionManager("first")
      val second = createNonFailingTransactionManager("second")
      createAndCommitTransaction(setupTransactionManagers(first, second))
      (first, second) match {
        case (f: TestPlatformTransactionManager, s: TestPlatformTransactionManager)
          if (f.isCommitted && s.isCommitted) => success
        case _ => failure
      }
    }

    "commit TMs in reverse order" in {
      val first = createNonFailingTransactionManager("first")
      val second = createNonFailingTransactionManager("second")
      createAndCommitTransaction(setupTransactionManagers(first, second))
      (first, second) match {
        case (f: TestPlatformTransactionManager, s: TestPlatformTransactionManager) => (f.commitTime, s.commitTime) match {
          case (Some(fct), Some(sct)) if (fct >= sct) => success
          case _ => failure
        }
        case _ => failure
      }
    }

  }

  "Failing TM" should {

    import util.control.Exception.allCatch
    import HeuristicCompletionException._

    def checkHeuristicState(state: Int)(t: Throwable) = t match {
      case e: HeuristicCompletionException if (e.getOutcomeState == state) => success
      case e: HeuristicCompletionException => {
        println("threw HeuristicCompletionException not in correct state")
        failure
      }
      case _ => {
        println("threw some other exception")
        failure
      }
    }

    "throw rolled back exception for single TM failure" in {
      val checker = checkHeuristicState(STATE_ROLLED_BACK) _
      val tm = setupTransactionManagers(createFailingTransactionManager("single"))
      allCatch.either {
        createAndCommitTransaction(tm)
      }.fold(
        fa => checker(fa),
        fb => {
          println("did not thrown any exception")
          failure
        }
      )
    }

    "throw mixed rolled back exception for non first TM failure" in {
      val checker = checkHeuristicState(STATE_MIXED) _
      val tm = setupTransactionManagers(
        createFailingTransactionManager("first"),
        createNonFailingTransactionManager("second")
      )
      allCatch.either {
        createAndCommitTransaction(tm)
      }.fold(
        fa => checker(fa),
        fb => {
          println("did not thrown any exception")
          failure
        }
      )
    }

    "rollback all TMs" in {
      val first = createNonFailingTransactionManager("first")
      val second = createNonFailingTransactionManager("second")
      createAndRollbackTransaction(setupTransactionManagers(first, second))
      (first, second) match {
        case (f: TestPlatformTransactionManager, s: TestPlatformTransactionManager)
          if (f.wasRolledBack && s.wasRolledBack) => success
        case _ => failure
      }
    }

    "throw unexpected exception on failing rollback" in {
      val ctm = setupTransactionManagers(createFailingTransactionManager("first"))
      allCatch.either {
        createAndRollbackTransaction(ctm)
      }.fold(
        fa => fa match {
          case e: UnexpectedRollbackException => success
          case _ => {
            println("threw some other exception")
            failure
          }
        },
        fb => {
          println("did not thrown any exception")
          failure
        }
      )
    }

  }

}
