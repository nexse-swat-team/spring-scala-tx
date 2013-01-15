package com.nexse.swat.springscala.tx.neo4j

import javax.persistence.{Id, Entity}
import beans.BeanProperty

@Entity
class AnEntity {

  @Id
  @BeanProperty
  var id: Long = _

  var name: String = _

}
