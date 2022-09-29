package cougar.graph.services.services.handler;

import cougar.graph.store.rdf.models.AbstractModel;
import cougar.graph.services.services.EntityServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class DelegatingValidator implements Validator {
    private List<Validator> validators;

    @Autowired(required = false)
    public void setRegisteredBeans(List<Validator> validators) {
        this.validators = validators;
    }

    @Override
    public Mono<? extends AbstractModel> handle(EntityServices entityServices, AbstractModel triples, Map<String, String> parameters) {
        if(this.validators == null) return Mono.just(triples);

        return Flux.fromIterable(validators)
                .reduce(Mono.just(triples), (modelMono, validator) ->
                        modelMono.map(model ->
                                validator.handle(entityServices, model, parameters)).flatMap(mono -> mono))
                .flatMap(mono -> mono);
    }
}
