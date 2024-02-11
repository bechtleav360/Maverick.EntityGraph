package org.av360.maverick.graph.services.preprocessors;

/**
 * Transformers are modifying the incoming model. No order can be assumed.
 * <p>
 * They are not allowed to access the repositories (that must happen after the transaction through reconciliation (see postprocessors and jobs))
 */