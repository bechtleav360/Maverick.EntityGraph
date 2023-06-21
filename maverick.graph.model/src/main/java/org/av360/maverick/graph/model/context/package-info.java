package org.av360.maverick.graph.model.context;

/**
 * Mostly needed to transport contextual information (Authentication, Scope, Environment) across layers and events.
 * Thread context is a bit tricky to handle when using events (which cross thread boundaries)
 *
 *
 * Responsibilities:
 *
 *
 * Controller --
 *              |--> Services --> Repositories
 * Scheduler  --
 *
 * Authentication only within Controller
 * Scheduler without Authentication
 * Controller creates a SessionContext
 *
 * Authorization only within Services
 *
 * Repositories only receive environment
 *
 *
 *
 * Controller -- RequestContext --> Services
 * Scheduler -- RequestContext --> Services
 *
 * Services -- Environment --> Repositories
 */