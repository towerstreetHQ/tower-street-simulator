
package io.towerstreet.slick.models.generated.public

import shapeless.tag.@@
import shapeless.tag
import slick.collection.heterogeneous._
import slick.collection.heterogeneous.syntax.HNil
import io.towerstreet.slick.models.generated._

object Model {

  
  trait AssessmentTypeTag
  type AssessmentTypeId = Int @@ AssessmentTypeTag
  
  object AssessmentTypeId {
    def apply(id: Int): AssessmentTypeId = tag[AssessmentTypeTag][Int](id)
  }
             
  /** Entity class storing rows of table AssessmentTypeTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param assessmentKey Database column assessment_key SqlType(varchar), Length(30,true)
   *  @param description Database column description SqlType(varchar), Default(None) */
  case class AssessmentType(id: AssessmentTypeId, assessmentKey: String, description: Option[String] = None)
  
  
  trait CampaignVisitorTag
  type CampaignVisitorId = Int @@ CampaignVisitorTag
  
  object CampaignVisitorId {
    def apply(id: Int): CampaignVisitorId = tag[CampaignVisitorTag][Int](id)
  }
             
  /** Entity class storing rows of table CampaignVisitorTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param customerId Database column customer_id SqlType(int4)
   *  @param createdAt Database column created_at SqlType(timestamp)
   *  @param inetAddr Database column inet_addr SqlType(inet), Length(2147483647,false), Default(None)
   *  @param userAgent Database column user_agent SqlType(varchar), Default(None)
   *  @param accessToken Database column access_token SqlType(varchar), Default(None) */
  case class CampaignVisitor(id: CampaignVisitorId, customerId: CustomerId, createdAt: java.time.LocalDateTime, inetAddr: Option[com.github.tminglei.slickpg.InetString] = None, userAgent: Option[String] = None, accessToken: Option[String] = None)
  
  
  trait CustomerAssessmentTag
  type CustomerAssessmentId = Int @@ CustomerAssessmentTag
  
  object CustomerAssessmentId {
    def apply(id: Int): CustomerAssessmentId = tag[CustomerAssessmentTag][Int](id)
  }
             
  /** Entity class storing rows of table CustomerAssessmentTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param assessmentTypeId Database column assessment_type_id SqlType(int4)
   *  @param customerId Database column customer_id SqlType(int4)
   *  @param createdAt Database column created_at SqlType(timestamp)
   *  @param closedAt Database column closed_at SqlType(timestamp), Default(None) */
  case class CustomerAssessment(id: CustomerAssessmentId, assessmentTypeId: AssessmentTypeId, customerId: CustomerId, createdAt: java.time.LocalDateTime, closedAt: Option[java.time.LocalDateTime] = None)
  
  
  trait CustomerTag
  type CustomerId = Int @@ CustomerTag
  
  object CustomerId {
    def apply(id: Int): CustomerId = tag[CustomerTag][Int](id)
  }
             
  /** Entity class storing rows of table CustomerTable
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   *  @param companyName Database column company_name SqlType(varchar)
   *  @param normalizedName Database column normalized_name SqlType(varchar)
   *  @param isCampaign Database column is_campaign SqlType(bool), Default(false)
   *  @param isActive Database column is_active SqlType(bool), Default(true) */
  case class Customer(id: CustomerId, companyName: String, normalizedName: String, isCampaign: Boolean = false, isActive: Boolean = true)
}
        
