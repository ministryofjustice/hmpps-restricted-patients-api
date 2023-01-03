package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.model.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "RESTRICTED_PATIENTS")
@EntityListeners(AuditingEntityListener::class)
class RestrictedPatient(
  @Id
  val prisonerNumber: String,
  val fromLocationId: String,
  val hospitalLocationCode: String,
  val supportingPrisonId: String,
  val dischargeTime: LocalDateTime,
  val commentText: String? = null
) {
  @CreatedDate
  @Column(name = "CREATE_DATETIME", nullable = false)
  var createDateTime: LocalDateTime? = null

  @CreatedBy
  @Column(name = "CREATE_USER_ID", nullable = false)
  var createUserId: String? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false

    other as RestrictedPatient
    return prisonerNumber == other.prisonerNumber
  }

  override fun hashCode(): Int = 100428483
}
