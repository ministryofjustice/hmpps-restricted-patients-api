package uk.gov.justice.digital.hmpps.hmppsrestrictedpatientsapi.config

import com.amazon.sqs.javamessaging.ProviderConfiguration
import com.amazon.sqs.javamessaging.SQSConnectionFactory
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.AnonymousAWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.support.destination.DynamicDestinationResolver
import javax.jms.Session

@Configuration
@ConditionalOnExpression("{'aws', 'localstack'}.contains('\${domain-events-sqs.provider}')")
@EnableJms
class OffenderEventsJmsConfig {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Bean("jmsListenerContainerFactoryForDomainEvents")
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  fun jmsListenerContainerFactory(awsSqsClientForDomainEvents: AmazonSQS): DefaultJmsListenerContainerFactory {
    val factory = DefaultJmsListenerContainerFactory()
    factory.setConnectionFactory(SQSConnectionFactory(ProviderConfiguration(), awsSqsClientForDomainEvents))
    factory.setDestinationResolver(DynamicDestinationResolver())
    factory.setConcurrency("1")
    factory.setSessionAcknowledgeMode(Session.CLIENT_ACKNOWLEDGE)
    factory.setErrorHandler { t: Throwable? -> log.error("Error caught in jms listener", t) }
    return factory
  }

  @Bean
  @ConditionalOnProperty(name = ["domain-events-sqs.provider"], havingValue = "aws")
  fun awsSqsClientForDomainEvents(
    @Value("\${domain-events-sqs.aws.access.key.id}") accessKey: String,
    @Value("\${domain-events-sqs.aws.secret.access.key}") secretKey: String,
    @Value("\${dmain-events-sqs.endpoint.region}") region: String
  ): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKey, secretKey)))
      .withRegion(region)
      .build()

  @Bean
  @ConditionalOnProperty(name = ["domain-events-sqs.provider"], havingValue = "aws")
  fun awsSqsDlqClientForDomainEvents(
    @Value("\${domain-events-sqs.aws.dlq.access.key.id}") accessKey: String,
    @Value("\${domain-events-sqs.aws.dlq.secret.access.key}") secretKey: String,
    @Value("\${domain-events-sqs.endpoint.region}") region: String
  ): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKey, secretKey)))
      .withRegion(region)
      .build()

  @Bean("awsSqsClientForDomainEvents")
  @ConditionalOnProperty(name = ["domain-events-sqs.provider"], havingValue = "localstack")
  fun awsSqsClientForDomainEventsLocalstack(
    @Value("\${domain-events-sqs.endpoint.url}") serviceEndpoint: String,
    @Value("\${domain-events-sqs.endpoint.region}") region: String
  ): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
      .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
      .build()

  @Bean("awsSqsDlqClientForDomainEvents")
  @ConditionalOnProperty(name = ["domain-events-sqs.provider"], havingValue = "localstack")
  fun awsSqsDlqClientForDomainEventsLocalstack(
    @Value("\${domain-events-sqs.endpoint.url}") serviceEndpoint: String,
    @Value("\${domain-events-sqs.endpoint.region}") region: String
  ): AmazonSQS =
    AmazonSQSClientBuilder.standard()
      .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(serviceEndpoint, region))
      .withCredentials(AWSStaticCredentialsProvider(AnonymousAWSCredentials()))
      .build()

  @Bean("queueUrlForOffenderEvents")
  @ConditionalOnProperty(name = ["domain-events-sqs.provider"], havingValue = "localstack")
  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  fun queueUrl(
    @Autowired awsSqsClientForDomainEvents: AmazonSQS,
    @Value("\${domain-events-sqs.queue.name}") queueName: String
  ): String = awsSqsClientForDomainEvents.getQueueUrl(queueName).queueUrl
}
