package pers.solid.ecmd.exception;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.jetbrains.annotations.Nullable;

/**
 * 类似于 {@link CommandSyntaxException}，但是允许在运行时抛出。
 */
public class CommandRuntimeException extends RuntimeException {
  public final Message rawMessage;

  @Override
  public @Nullable String getMessage() {
    final String message = super.getMessage();
    if (message == null) {
      return rawMessage.getString();
    }
    return message;
  }

  public CommandRuntimeException(Message message) {
    super();
    this.rawMessage = message;
  }

  public CommandRuntimeException(CommandSyntaxException cause) {
    super(cause);
    this.rawMessage = cause.getRawMessage();
  }

  public CommandRuntimeException(Message message, Throwable cause) {
    super(cause);
    this.rawMessage = message;
  }

  public CommandRuntimeException(CommandSyntaxException cause, boolean enableSuppression, boolean writableStackTrace) {
    super(null, cause, enableSuppression, writableStackTrace);
    this.rawMessage = cause.getRawMessage();
  }
}
