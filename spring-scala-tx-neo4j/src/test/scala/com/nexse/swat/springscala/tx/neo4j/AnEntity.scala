package com.nexse.swat.springscala.tx.neo4j

import beans.BeanProperty
import javax.persistence.{GeneratedValue, GenerationType, Id, Entity}

@Entity
class AnEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @BeanProperty
  var id: Long = _

  var name: String = _

}