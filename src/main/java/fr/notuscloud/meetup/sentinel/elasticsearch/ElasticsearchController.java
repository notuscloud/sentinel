package fr.notuscloud.meetup.sentinel.elasticsearch;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ElasticsearchController {

    private ElasticsearchService elasticsearchService;

    // constructor
    public ElasticsearchController(ElasticsearchService elasticsearchService) {
        this.elasticsearchService = elasticsearchService;
    }

    @GetMapping(value="all")
    public @ResponseBody String all(){

        Long countVaultDecryptRequest      = elasticsearchService.countVaultDecryptRequests();
        Long countGatekeeperDecryptRequest = elasticsearchService.countGatekeeperDecryptRequests();

        return countGatekeeperDecryptRequest.toString() + "=" + countVaultDecryptRequest.toString();

    }

}
