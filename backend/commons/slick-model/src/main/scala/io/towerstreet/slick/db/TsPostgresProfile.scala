package io.towerstreet.slick.db

import com.github.tminglei.slickpg._

trait TsPostgresProfile extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  with PgPlayJsonSupport
  with PgNetSupport
  with PgLTreeSupport
  with PgRangeSupport
  with PgHStoreSupport
  with PgSearchSupport {

  override val pgjson = "jsonb"
  ///
  override val api = new API with ArrayImplicits
    with DateTimeImplicits
    with Date2DateTimePlainImplicits
    with PlayJsonImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SimpleArrayPlainImplicits
    with PlayJsonPlainImplicits
    with SearchAssistants {}
}

object TsPostgresProfile extends TsPostgresProfile
