package pers.solid.ecmd.exception;

/**
 * 用于在特殊的 forEach 型循环中表示中止执行，直接抛出即可。注意需要捕获，否则会直接抛出。为节省性能，所有不需要的 stacktrace 都会被禁用
 */
public class StopIterationException extends RuntimeException {
  public static final StopIterationException INSTANCE = new StopIterationException();

  public StopIterationException() {
    super(null, null, false, false);
  }
}
