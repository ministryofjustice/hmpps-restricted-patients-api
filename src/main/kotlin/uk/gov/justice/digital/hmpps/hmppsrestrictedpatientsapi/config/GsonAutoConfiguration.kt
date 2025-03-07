package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.gson.GsonProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Gson::class)
@EnableConfigurationProperties(GsonProperties::class)
class GsonAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean
  fun gson(gsonBuilder: GsonBuilder): Gson = gsonBuilder
    .setPrettyPrinting()
    .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
    .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeAdapter())
    .create()

  internal class LocalDateAdapter :
    JsonSerializer<LocalDate?>,
    JsonDeserializer<LocalDate?> {
    override fun serialize(src: LocalDate?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement = JsonPrimitive(src?.format(DateTimeFormatter.ISO_LOCAL_DATE))

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDate? = LocalDate.parse(json?.asJsonPrimitive?.asString)
  }

  internal class LocalDateTimeAdapter :
    JsonSerializer<LocalDateTime?>,
    JsonDeserializer<LocalDateTime> {
    override fun serialize(
      localDateTime: LocalDateTime?,
      srcType: Type,
      context: JsonSerializationContext,
    ): JsonElement = JsonPrimitive(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(localDateTime))

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDateTime = LocalDateTime.parse(
      json.asString,
      DateTimeFormatter.ISO_LOCAL_DATE_TIME.withLocale(Locale.ENGLISH),
    )
  }
}
