package org.av360.maverick.graph.feature.jobs.model;

import org.av360.maverick.graph.model.context.SessionContext;

import java.time.Duration;
import java.time.Instant;

public class ScheduledJob implements Runnable {

    private org.av360.maverick.graph.model.entities.ScheduledJob job;


    private SessionContext context;
    private boolean isCompleted = false;
    private boolean isFailed = false;
    private boolean isActive = false;

    private boolean isSubmitted = false;
    private String errorMessage;


    private String identifier;
    private Instant submissionTime;
    private Instant startingTime;

    private Instant completionTime;

    public String getName() {
        return this.job.getName();
    }

    public ScheduledJob(org.av360.maverick.graph.model.entities.ScheduledJob job, SessionContext context, String identifier) {
        this.job = job;
        this.context = context;
        this.identifier = identifier;
    }


    @Override
    public void run() {
        this.job.run(this.context)
                .doOnSubscribe(subscription -> {
                    this.startingTime = Instant.now();
                    this.isActive = true;
                    this.isCompleted = false;
                    this.isFailed = false;
                })
                .doOnSuccess(success -> {
                    this.isCompleted = true;
                    this.isActive = false;
                    this.isFailed = false;
                    this.completionTime = Instant.now();
                })
                .doOnError(error -> {
                    this.isFailed = true;
                    this.isActive = false;
                    this.isCompleted = false;
                    this.errorMessage = error.getMessage();
                    this.completionTime = Instant.now();
                })
                .subscribe();
    }




    public String getIdentifier() {
        return identifier;
    }

    public Instant getCompletionTime() {
        return completionTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isActive() {
        return isActive && !this.isCompleted && !this.isFailed;
    }

    public boolean isFailed() {
        return isFailed;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public boolean isSubmitted() {
        return this.isSubmitted && !this.isActive && !this.isCompleted && !this.isFailed;
    }

    public Instant getSubmissionTime() {
        return this.submissionTime;
    }

    public void setSubmitted() {
        this.isSubmitted = true;
        this.submissionTime = Instant.now();
    }

    public Duration getWaitingDuration() {
        if(this.startingTime == null) {
            return Duration.between(this.submissionTime, Instant.now());
        } else {
            return Duration.between(this.submissionTime, this.startingTime);
        }
    }

    public Instant getStartingTime() {
        return this.startingTime;
    }

    public Duration getRunningTime() {
        if(this.startingTime == null) return null;

        if(this.completionTime == null) {
            return Duration.between(this.startingTime, Instant.now());
        } else {
            return Duration.between(this.startingTime, this.completionTime);
        }
    }
}
