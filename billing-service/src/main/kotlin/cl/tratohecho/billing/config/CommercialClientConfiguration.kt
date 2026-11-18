package cl.tratohecho.billing.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class CommercialClientConfiguration {
    @Bean
    fun commercialRestClient(
        builder: RestClient.Builder,
        @Value("\${services.commercial.base-url:http://localhost:8083}") baseUrl: String
    ): RestClient = builder.baseUrl(baseUrl).build()
}
