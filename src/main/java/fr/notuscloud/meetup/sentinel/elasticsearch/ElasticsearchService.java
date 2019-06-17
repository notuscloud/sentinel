package fr.notuscloud.meetup.sentinel.elasticsearch;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

@Service
public class ElasticsearchService {

    // Number of hours from now(), used to calculate the time range for ES queries
    @Value("${sentinel.period}")
    private Integer HOURS;

    // Statics
    // Not used anymore
    private final String dateFormatPattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // High Rest Client singleton
    private RestHighLevelClient restHighLevelClient;

    // Logger
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchConfiguration.class);

    // Constructor
    public ElasticsearchService(RestHighLevelClient restHighLevelClient) {
        this.restHighLevelClient = restHighLevelClient;
    }

    public Long countVaultDecryptRequests(){

        // Instanciate a SearchRequest
        SearchRequest searchRequest = new SearchRequest().indices("filebeat-*");
        // Is used to configure the search
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // Working with time
        SimpleDateFormat format = new SimpleDateFormat(dateFormatPattern, Locale.FRENCH);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));

        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("audit.request.path", "transit/decrypt/gatekeeper"))
                .must(QueryBuilders.matchQuery("audit.type", "request"))
                .filter(QueryBuilders.rangeQuery("@timestamp")
                        .from("now-"+HOURS+"h")
                        .timeZone("GMT")
                )
        );

        Long count = null;
        try {
            searchRequest.source(searchSourceBuilder);
            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            count = response.getHits().getTotalHits().value;
        }catch(ElasticsearchException e) {
            LOG.error(e.getDetailedMessage());
        } catch (IOException ex){
            LOG.error(ex.getLocalizedMessage());
        }

        return count;
    }

    public Long countGatekeeperDecryptRequests(){

        CountRequest countRequest = new CountRequest()
                .indices("gatekeeper");

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // Working with time
        SimpleDateFormat format = new SimpleDateFormat(dateFormatPattern, Locale.FRENCH);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));

        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.matchAllQuery())
                .filter(QueryBuilders.rangeQuery("timestamp")
                        .from(format.format(LocalDateTime.now().minusHours(HOURS).toDate()))
                        .to(format.format(LocalDateTime.now().toDate()))
                )
        );
        return countDocuments(countRequest, searchSourceBuilder);

    }

    private Long countDocuments(CountRequest request, SearchSourceBuilder searchSourceBuilder){

        request.source(searchSourceBuilder);

        Long result = null;
        try {
            CountResponse count = restHighLevelClient.count(request, RequestOptions.DEFAULT);
            result = count.getCount();

        }catch(ElasticsearchException e) {
            LOG.error(e.getDetailedMessage());
        } catch (IOException ex){
            LOG.error(ex.getLocalizedMessage());
        }
        return result;

    }

    private AggregationBuilder buildTimeAggregation(Integer hours){

        // Math current time minus one hour
        LocalDateTime currentDateAndTime = LocalDateTime.now();
        LocalDateTime minusOneHour = currentDateAndTime.minusHours(hours);
        // Format the date
        SimpleDateFormat format = new SimpleDateFormat(dateFormatPattern, Locale.FRENCH);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String unboundedTime = format.format(minusOneHour.toDate());

        AggregationBuilder aggregationBuilder = AggregationBuilders.dateRange("agg")
                .field("timestamp")
                .format(dateFormatPattern)
                .addUnboundedFrom(unboundedTime);

        return aggregationBuilder;

    }
    private Long countDocumentsAggregation(SearchRequest searchRequest, AggregationBuilder aggregation, SearchSourceBuilder searchSourceBuilder) {
        searchSourceBuilder.aggregation(aggregation);
        searchRequest.source(searchSourceBuilder);

        Long countDocument = null;
        try {

            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            Range agg = searchResponse.getAggregations().get("agg");
            Range.Bucket bucket = agg.getBuckets().get(0);
            countDocument = bucket.getDocCount();

            LOG.info("DEBUG: " + searchResponse.getHits().getTotalHits().toString());

        }catch(ElasticsearchException e) {
            LOG.error(e.getDetailedMessage());
        } catch (IOException ex){
            LOG.error(ex.getLocalizedMessage());
        }

        return  countDocument;
    }


}
