package fr.notuscloud.meetup.sentinel.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfiguration {

    @Value("${elasticsearch.cluster.nodes}")
    private String elasticsearchHost;
    @Value("${elasticsearch.cluster.port}")
    private Integer elasticsearchPort;
    @Value("${elasticsearch.cluster.scheme}")
    private String elasticsearchScheme;

    @Bean(destroyMethod = "close")
    public RestHighLevelClient client() {

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort, elasticsearchScheme)));

        return client;

    }
}


