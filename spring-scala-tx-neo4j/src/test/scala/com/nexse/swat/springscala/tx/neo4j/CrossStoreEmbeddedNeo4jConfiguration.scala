package com.nexse.swat.springscala.tx.neo4j

import com.atomikos.jdbc.nonxa.AtomikosNonXADataSourceBean
import org.springframework.context.annotation.{ComponentScan, Bean, Configuration}
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.neo4j.kernel.EmbeddedGraphDatabase
import sys.ShutdownHookThread
import java.io.File

@Configuration
@ComponentScan(basePackageClasses = Array(classOf[CrossStoreNeo4jConfiguration]))
@EnableTransactionManagement
class CrossStoreEmbeddedNeo4jConfiguration {

  @Bean
  def entityManagerFactory = {
    val factoryBean = new LocalContainerEntityManagerFactoryBean()
    factoryBean.setDataSource(dataSource)
    factoryBean.setPackagesToScan("com.nexse.swat.springscala.tx.neo4j")
    factoryBean.setJpaVendorAdapter {
      val va = new HibernateJpaVendorAdapter()
      va.setGenerateDdl(true)
      va.setShowSql(true)
      va
    }
    factoryBean.setPersistenceUnitName("testPU")
    factoryBean
  }

  @Bean
  def dataSource = {
    val dataSource = new AtomikosNonXADataSourceBean()
    dataSource.setUniqueResourceName("hsqldb")
    dataSource.setDriverClassName("org.hsqldb.jdbc.JDBCDriver")
    dataSource.setUrl("jdbc:hsqldb:mem:test")
    dataSource.setUser("sa")
    dataSource
  }

  @Bean
  def graphDatabaseService = {

    implicit def dirToPimpedDir(storeDir: String) = new {

      def rmdir() {
        def rmdirRec(f: File) {
          f.listFiles().foreach {
            file =>
              if (file.isDirectory) rmdirRec(file)
              file.delete()
          }
          f.delete()
        }
        rmdirRec(new File(storeDir))
      }

    }

    val ds = new EmbeddedGraphDatabase("testNeo4j")
    ShutdownHookThread {
      ds.shutdown()
      ds.getStoreDir.rmdir()
    }
    ds
  }

}