package com.nexse.swat.springscala.tx.neo4j

import org.springframework.beans.factory.annotation.Configurable
import org.neo4j.helpers.Service
import org.neo4j.kernel.impl.transaction.{TxHook, XaDataSourceManager, TransactionManagerProvider}
import org.neo4j.kernel.impl.core.KernelPanicEventGenerator
import org.neo4j.kernel.impl.util.StringLogger
import org.neo4j.kernel.impl.nioneo.store.FileSystemAbstraction

@Configurable
@Service.Implementation(Array(classOf[TransactionManagerProvider]))
class SpringProvider extends TransactionManagerProvider("spring-jta") {

  def loadTransactionManager(txLogDir: String,
                             xaDataSourceManager: XaDataSourceManager,
                             kpeg: KernelPanicEventGenerator,
                             rollbackHook: TxHook,
                             msgLog: StringLogger,
                             fileSystem: FileSystemAbstraction) = new SpringServiceImpl()

}
