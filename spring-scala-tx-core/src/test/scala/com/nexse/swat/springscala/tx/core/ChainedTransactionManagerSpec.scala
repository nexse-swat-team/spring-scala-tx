package com.nexse.swat.springscala.tx.core

import com.nexse.swat.springscala.tx.core.TestPlatformTransactionManager._
import org.specs2.mutable.Specification
import org.springframework.transaction.{UnexpectedRollbackException, HeuristicCompletionException, PlatformTransactionManager}
import org.springframework.transaction.support.DefaultTransactionDefinition

class ChainedTransactionManagerSpec extends Specification {

  private def setupTMs(transactionManagers: PlatformTransactionManager*) =
    new ChainedTransactionManager(new TestSynchronizationManager(), transactionManagers: _*)

  private def createAndCommitTx(tm: PlatformTransactionManager) {
    tm.commit(tm.getTransaction(new DefaultTransactionDefinition()))
  }

  private def createAndRollbackTx(tm: PlatformTransactionManager) {
    tm.rollback(tm.getTransaction(new DefaultTransactionDefinition()))
  }

  "Non failing TM" should {

    "complete successfully" in {
      val tm1 = createNonFailing("single")
      createAndCommitTx(setupTMs(tm1))
      tm1 match {
        case f if (f.isCommitted) => success
        case _ => failure
      }
    }

    "commit all registered TM" in {
      val (tm1, tm2) = (createNonFailing("first"), createNonFailing("second"))
      createAndCommitTx(setupTMs(tm1, tm2))
      (tm1, tm2) match {
        case (f, s) if (f.isCommitted && s.isCommitted) => success
        case _ => failure
      }
    }

    "commit TMs in reverse order" in {
      val (tm1, tm2) = (createNonFailing("first"), createNonFailing("second"))
      createAndCommitTx(setupTMs(tm1, tm2))
      (tm1, tm2) match {
        case (f, s) => (f.commitTime, s.commitTime) match {
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
      val chainedTM = setupTMs(createFailing("single"))
      allCatch.either {
        createAndCommitTx(chainedTM)
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
      val chainedTM = setupTMs(createFailing("first"), createNonFailing("second"))
      allCatch.either {
        createAndCommitTx(chainedTM)
      }.fold(
        fa => checker(fa),
        fb => {
          println("did not thrown any exception")
          failure
        }
      )
    }

    "rollback all TMs" in {
      val (tm1, tm2) = (createNonFailing("first"), createNonFailing("second"))
      createAndRollbackTx(setupTMs(tm1, tm2))
      (tm1, tm2) match {
        case (f, s) if (f.wasRolledBack && s.wasRolledBack) => success
        case _ => failure
      }
    }

    "throw unexpected exception on failing rollback" in {
      val chainedTM = setupTMs(createFailing("first"))
      allCatch.either {
        createAndRollbackTx(chainedTM)
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