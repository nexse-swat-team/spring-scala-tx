package com.nexse.swat.springscala.tx.neo4j

import org.neo4j.kernel.EmbeddedGraphDatabase
import org.springframework.context.annotation.{Bean, Configuration}
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import com.atomikos.jdbc.nonxa.AtomikosNonXADataSourceBean

@Configuration
@EnableTransactionManagement
class CrossStoreEmbeddedNeo4jConfiguration extends CrossStoreNeo4jConfiguration {

  lazy val emf = entityManagerFactory.getObject

  @Bean
  def entityManagerFactory = {
    val factoryBean = new LocalContainerEntityManagerFactoryBean()
    factoryBean.setDataSource(dataSource)
    factoryBean.setPackagesToScan("com.nexse.swat.springscala.tx.neo4j")
    factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter())
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
}