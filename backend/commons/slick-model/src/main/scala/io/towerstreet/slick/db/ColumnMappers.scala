package io.towerstreet.slick.db

import enumeratum.{Enum, EnumEntry, SlickEnumSupport}
import shapeless.tag
import shapeless.tag.@@

import scala.reflect.ClassTag


object ColumnMappers extends SlickEnumSupport {

  override val profile = TsPostgresProfile

  import profile.api._

  implicit def tagIntColumnType[Class] = MappedColumnType.base[Int @@ Class, Int](
    { ld => ld },
    { d  => tag[Class](d) }
  )

  implicit def tagLongColumnType[Class] = MappedColumnType.base[Long @@ Class, Long](
    { ld => ld },
    { d  => tag[Class](d) }
  )

  implicit def tagStringColumnType[Class] = MappedColumnType.base[String @@ Class, String](
    { ld => ld },
    { d  => tag[Class](d) }
  )

  implicit def enumStringColumnType[E <: EnumEntry](implicit enum: Enum[E], tag: ClassTag[E])= mappedColumnTypeForEnum(enum)


  implicit def tagIntListColumnType[Class] = MappedColumnType.base[List[Int @@ Class], List[Int]](
    { ld => ld },
    { l  => l.map((d: Int) => tag[Class](d)) }
  )
}
