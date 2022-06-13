package cougar.graph.store.behaviours;



public interface Selectable extends RepositoryBehaviour {


    /*default Mono<TupleQueryResult> select(String query) {
        return getRepository().map(repository -> {
            try (RepositoryConnection connection = repository.getConnection()) {
                TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
                try (TupleQueryResult result = q.evaluate()) {
                    return result;
                } catch (Exception e) {
                    throw e;
                }
            } catch (Exception e) {
                throw e;
            }
        });
    }*/


}
