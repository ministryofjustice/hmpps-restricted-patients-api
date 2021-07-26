package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities

import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Table

@Entity
@Table(name = "RESTRICTED_PATIENTS")
@EntityListeners(AuditingEntityListener::class)
class RestrictedPatient(
  id: Long? = null,
  val prisonerNumber: String,
  val fromLocationId: String,
  val hospitalLocationCode: String,
  val supportingPrisonId: String? = null,
  val dischargeTime: LocalDateTime,
  val commentText: String? = null
) : BaseEntity(id) {
  @CreatedDate
  @Column(name = "CREATE_DATETIME", nullable = false)
  var createDateTime: LocalDateTime? = null

  @CreatedBy
  @Column(name = "CREATE_USER_ID", nullable = false)
  var createUserId: String? = null
}
