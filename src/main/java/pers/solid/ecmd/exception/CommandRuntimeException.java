package pers.solid.ecmd.exception;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

/**
 * 类似于 {@link CommandSyntaxException}，但是允许在运行时抛出。
 */
public class CommandRuntimeException extends RuntimeException {
  public final @Nullable Message rawMessage;

  public CommandRuntimeException() {
    this.rawMessage = null;
  }

  @Override
  public String getMessage() {
    final String message = super.getMessage();
    if (message == null && this.rawMessage != null) {
      return rawMessage.getString();
    }
    return message;
  }

  public CommandRuntimeException(@Nullable Message message) {
    super();
    this.rawMessage = message;
  }

  public CommandRuntimeException(CommandSyntaxException cause) {
    super(cause);
    this.rawMessage = cause.getRawMessage();
  }

  public CommandRuntimeException(Throwable cause) {
    super(cause);
    this.rawMessage = null;
  }

  public CommandRuntimeException(String message, Throwable cause) {
    super(message, cause);
    this.rawMessage = null;
  }

  public CommandRuntimeException(@Nullable Message message, Throwable cause) {
    super(cause);
    this.rawMessage = message;
  }

  public CommandRuntimeException(CommandSyntaxException cause, boolean enableSuppression, boolean writableStackTrace) {
    super(null, cause, enableSuppression, writableStackTrace);
    this.rawMessage = cause.getRawMessage();
  }

  public CommandRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.rawMessage = null;
  }
}
