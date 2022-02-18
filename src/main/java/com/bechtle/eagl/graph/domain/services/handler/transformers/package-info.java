package com.bechtle.eagl.graph.domain.services.handler.transformers;

/**
 * Transformers are modifying the incoming model. No order can be assumed.
 *
 * They are not allowed to access the entity graph (that must happen after the transaction through reconciliation)
 */