package com.nexse.swat.springscala.tx.neo4j

import beans.BeanProperty
import javax.persistence.{Id, Entity}

@Entity
class AnEntity {

  @Id
  @BeanProperty
  var id: Long = _

  var name: String = _

}