package io.stackgres.jobs.dbops.clusterrestart;

public class FailedRestartPostgresException extends RuntimeException{

  private static final long serialVersionUID = 1L;
  
  public FailedRestartPostgresException(String message) {
    super(message);
  }

}
