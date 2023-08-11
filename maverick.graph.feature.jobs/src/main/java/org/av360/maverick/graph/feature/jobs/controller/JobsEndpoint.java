package org.av360.maverick.graph.feature.jobs.controller;

import org.av360.maverick.graph.feature.jobs.worker.JobWorker;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

@Component
@Endpoint(id = "jobs")
public class JobsEndpoint {

    private final JobWorker jobWorker;

    public JobsEndpoint(JobWorker jobWorker) {
        this.jobWorker = jobWorker;
    }

    @ReadOperation
    public Map<Category, Stream<JobDescription>> all() {



        return Map.of(
                Category.REGISTERED, jobWorker.getRegisteredJobs().stream().map(job -> new RegisteredJobDescription(job.getName())),
                Category.RUNNING, jobWorker.getActiveJobs().stream().map(job -> new ActiveJobDescription(job.getName(), job.getIdentifier(), job.getSubmissionTime(), job.getWaitingDuration().getSeconds(), job.getStartingTime(), job.getRunningTime().getSeconds())),
                Category.WAITING, jobWorker.getSubmittedJobs().stream().map(job -> new SubmittedJobDescription(job.getName(), job.getIdentifier(), job.getSubmissionTime(), job.getWaitingDuration().getSeconds())),
                Category.FAILED, jobWorker.getFailedJobs().stream().limit(10).map(job -> new FailedJobDescription(job.getName(), job.getIdentifier(), job.getSubmissionTime(), job.getWaitingDuration().getSeconds(), job.getStartingTime(), job.getRunningTime().getSeconds(), job.getErrorMessage())),
                Category.COMPLETED, jobWorker.getCompletedJobs().stream().limit(5).map(job -> new CompletedJobDescription(job.getName(), job.getIdentifier(), job.getSubmissionTime(), job.getWaitingDuration().getSeconds(), job.getStartingTime(), job.getRunningTime().getSeconds(), job.getCompletionTime()))
        );
    }



    public enum Category {
        REGISTERED,
        RUNNING,
        WAITING,
        COMPLETED,
        FAILED
    }


    private interface JobDescription {}

    private record RegisteredJobDescription(String name) implements JobDescription { }

    private record SubmittedJobDescription(String name, String identifier, Instant submittedAt, long waitingFor) implements JobDescription {}

    private record ActiveJobDescription(String name, String identifier, Instant submittedAt, long queueTime, Instant startedAt, long runningFor) implements JobDescription {}

    private record FailedJobDescription(String name, String identifier, Instant submittedAt, long queueTime, Instant startedAt, long duration, String errorMessage) implements JobDescription {}
    private record CompletedJobDescription(String name, String identifier, Instant submittedAt, long queueTime, Instant startedAt, long duration, Instant completedAt) implements JobDescription {}

}
