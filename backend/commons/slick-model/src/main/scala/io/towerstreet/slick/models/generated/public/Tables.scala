package io.towerstreet.slick.models.generated.public
// AUTO-GENERATED Slick data model
/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = io.towerstreet.slick.db.TsPostgresProfile
} with Tables

/** Slick data model trait for extension, choice of backend or usage in the cake pattern. (Make sure to initialize this late.) */
trait Tables {
  val profile: io.towerstreet.slick.db.TsPostgresProfile
  import profile.api._
  
  import io.towerstreet.slick.models.generated.public.Model._
  import io.towerstreet.slick.models.generated._
  import io.towerstreet.slick.db.ColumnMappers._

  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{GetResult => GR}

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = AssessmentTypeTable.schema ++ CampaignVisitorTable.schema ++ CustomerAssessmentTable.schema ++ CustomerTable.schema
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /** Table description of table assessment_type. Objects of this class serve as prototypes for rows in queries. */
  class AssessmentTypeTable(_tableTag: Tag) extends profile.api.Table[AssessmentType](_tableTag, Some("public"), "assessment_type") {
    def * = (id, assessmentKey, description) <> (AssessmentType.tupled, AssessmentType.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(assessmentKey), description).shaped.<>({r=>import r._; _1.map(_=> AssessmentType.tupled((_1.get, _2.get, _3)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[AssessmentTypeId] = column[AssessmentTypeId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column assessment_key SqlType(varchar), Length(30,true) */
    val assessmentKey: Rep[String] = column[String]("assessment_key", O.Length(30,varying=true))
    /** Database column description SqlType(varchar), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))

    /** Uniqueness Index over (assessmentKey) (database name assessment_type_assessment_key_key) */
    val index1 = index("assessment_type_assessment_key_key", assessmentKey, unique=true)
  }
  /** Collection-like TableQuery object for table AssessmentTypeTable */
  lazy val AssessmentTypeTable = new TableQuery(tag => new AssessmentTypeTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val AssessmentTypeTableInsert = AssessmentTypeTable returning AssessmentTypeTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table campaign_visitor. Objects of this class serve as prototypes for rows in queries. */
  class CampaignVisitorTable(_tableTag: Tag) extends profile.api.Table[CampaignVisitor](_tableTag, Some("public"), "campaign_visitor") {
    def * = (id, customerId, createdAt, inetAddr, userAgent, accessToken) <> (CampaignVisitor.tupled, CampaignVisitor.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(customerId), Rep.Some(createdAt), inetAddr, userAgent, accessToken).shaped.<>({r=>import r._; _1.map(_=> CampaignVisitor.tupled((_1.get, _2.get, _3.get, _4, _5, _6)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[CampaignVisitorId] = column[CampaignVisitorId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column customer_id SqlType(int4) */
    val customerId: Rep[CustomerId] = column[CustomerId]("customer_id")
    /** Database column created_at SqlType(timestamp) */
    val createdAt: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("created_at")
    /** Database column inet_addr SqlType(inet), Length(2147483647,false), Default(None) */
    val inetAddr: Rep[Option[com.github.tminglei.slickpg.InetString]] = column[Option[com.github.tminglei.slickpg.InetString]]("inet_addr", O.Length(2147483647,varying=false), O.Default(None))
    /** Database column user_agent SqlType(varchar), Default(None) */
    val userAgent: Rep[Option[String]] = column[Option[String]]("user_agent", O.Default(None))
    /** Database column access_token SqlType(varchar), Default(None) */
    val accessToken: Rep[Option[String]] = column[Option[String]]("access_token", O.Default(None))
  }
  /** Collection-like TableQuery object for table CampaignVisitorTable */
  lazy val CampaignVisitorTable = new TableQuery(tag => new CampaignVisitorTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val CampaignVisitorTableInsert = CampaignVisitorTable returning CampaignVisitorTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table customer_assessment. Objects of this class serve as prototypes for rows in queries. */
  class CustomerAssessmentTable(_tableTag: Tag) extends profile.api.Table[CustomerAssessment](_tableTag, Some("public"), "customer_assessment") {
    def * = (id, assessmentTypeId, customerId, createdAt, closedAt) <> (CustomerAssessment.tupled, CustomerAssessment.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(assessmentTypeId), Rep.Some(customerId), Rep.Some(createdAt), closedAt).shaped.<>({r=>import r._; _1.map(_=> CustomerAssessment.tupled((_1.get, _2.get, _3.get, _4.get, _5)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[CustomerAssessmentId] = column[CustomerAssessmentId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column assessment_type_id SqlType(int4) */
    val assessmentTypeId: Rep[AssessmentTypeId] = column[AssessmentTypeId]("assessment_type_id")
    /** Database column customer_id SqlType(int4) */
    val customerId: Rep[CustomerId] = column[CustomerId]("customer_id")
    /** Database column created_at SqlType(timestamp) */
    val createdAt: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("created_at")
    /** Database column closed_at SqlType(timestamp), Default(None) */
    val closedAt: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("closed_at", O.Default(None))
  }
  /** Collection-like TableQuery object for table CustomerAssessmentTable */
  lazy val CustomerAssessmentTable = new TableQuery(tag => new CustomerAssessmentTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val CustomerAssessmentTableInsert = CustomerAssessmentTable returning CustomerAssessmentTable.map(_.id) into ((item, id) => item.copy(id = id))
             

  /** Table description of table customer. Objects of this class serve as prototypes for rows in queries. */
  class CustomerTable(_tableTag: Tag) extends profile.api.Table[Customer](_tableTag, Some("public"), "customer") {
    def * = (id, companyName, normalizedName, isCampaign, isActive) <> (Customer.tupled, Customer.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(id), Rep.Some(companyName), Rep.Some(normalizedName), Rep.Some(isCampaign), Rep.Some(isActive)).shaped.<>({r=>import r._; _1.map(_=> Customer.tupled((_1.get, _2.get, _3.get, _4.get, _5.get)))}, (_:Any) =>  throw new Exception("Inserting into ? projection not supported."))

    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[CustomerId] = column[CustomerId]("id", O.AutoInc, O.PrimaryKey)
    /** Database column company_name SqlType(varchar) */
    val companyName: Rep[String] = column[String]("company_name")
    /** Database column normalized_name SqlType(varchar) */
    val normalizedName: Rep[String] = column[String]("normalized_name")
    /** Database column is_campaign SqlType(bool), Default(false) */
    val isCampaign: Rep[Boolean] = column[Boolean]("is_campaign", O.Default(false))
    /** Database column is_active SqlType(bool), Default(true) */
    val isActive: Rep[Boolean] = column[Boolean]("is_active", O.Default(true))

    /** Uniqueness Index over (companyName) (database name company_company_name_key) */
    val index1 = index("company_company_name_key", companyName, unique=true)
    /** Uniqueness Index over (normalizedName) (database name company_short_name_key) */
    val index2 = index("company_short_name_key", normalizedName, unique=true)
  }
  /** Collection-like TableQuery object for table CustomerTable */
  lazy val CustomerTable = new TableQuery(tag => new CustomerTable(tag))

  /** Collection-like TableQuery object resolving inserted object's primary key for table OutcomeTaskResultTable */
  lazy val CustomerTableInsert = CustomerTable returning CustomerTable.map(_.id) into ((item, id) => item.copy(id = id))
             
          
}
