package com.github.mustachejava.codes;

import com.github.mustachejava.*;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

/**
 * Simplest possible code implementaion with some default shared behavior
 */
public class DefaultCode implements Code, Cloneable {
  // Final once init() is complete
  protected String appended;

  protected final ObjectHandler oh;
  protected final String name;
  protected final TemplateContext tc;
  protected final Mustache mustache;
  protected final String type;
  protected final boolean returnThis;
  protected final Binding binding;

  // Debug callsites
  protected static boolean debug = Boolean.getBoolean("mustache.debug");
  protected static Logger logger = Logger.getLogger("mustache");

  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw new MustacheException("Clone not supported");
    }
  }

  public DefaultCode() {
    this(null, null, null, null, null);
  }

  public DefaultCode(TemplateContext tc, ObjectHandler oh, Mustache mustache, String name, String type) {
    this.oh = oh;
    this.mustache = mustache;
    this.type = type;
    this.name = name;
    this.tc = tc;
    this.binding = oh == null ? null : oh.createBinding(name, tc, this);
    this.returnThis = ".".equals(name);
  }

  public Code[] getCodes() {
    return mustache == null ? null : mustache.getCodes();
  }

  @Override
  public synchronized void init() {
    Code[] codes = getCodes();
    if (codes != null) {
      for (Code code : codes) {
        code.init();
      }
    }
  }

  public void setCodes(Code[] newcodes) {
    mustache.setCodes(newcodes);
  }

  /**
   * Retrieve the first value in the stacks of scopes that matches
   * the give name. The method wrappers are cached and guarded against
   * the type or number of scopes changing.
   * <p/>
   * Methods will be found using the object handler, called here with
   * another lookup on a guard failure and finally coerced to a final
   * value based on the ObjectHandler you provide.
   *
   * @param scopes An array of scopes to interrogate from right to left.
   * @return The value of the field or method
   */
  public Object get(Object[] scopes) {
    if (returnThis) {
      return scopes[scopes.length - 1];
    }
    return binding.get(scopes);
  }

  @Override
  public Writer execute(Writer writer, Object scope) {
    return execute(writer, new Object[]{scope});
  }

  /**
   * The default behavior is to run the codes and append the captured text.
   *
   * @param writer The writer to write the output to
   * @param scopes The scopes to evaluate the embedded names against.
   */
  @Override
  public Writer execute(Writer writer, Object[] scopes) {
    return appendText(runCodes(writer, scopes));
  }

  @Override
  public void identity(Writer writer) {
    try {
      if (name != null) {
        tag(writer, type);
        if (getCodes() != null) {
          runIdentity(writer);
          tag(writer, "/");
        }
      }
      appendText(writer);
    } catch (IOException e) {
      throw new MustacheException(e);
    }
  }

  protected void runIdentity(Writer writer) {
    int length = getCodes().length;
    for (int i = 0; i < length; i++) {
      getCodes()[i].identity(writer);
    }
  }

  private void tag(Writer writer, String tag) throws IOException {
    writer.write(tc.startChars());
    writer.write(tag);
    writer.write(name);
    writer.write(tc.endChars());
  }

  protected Writer appendText(Writer writer) {
    if (appended != null) {
      try {
        writer.write(appended);
      } catch (IOException e) {
        throw new MustacheException(e);
      }
    }
    return writer;
  }

  protected Writer runCodes(Writer writer, Object[] scopes) {
    Code[] codes = getCodes();
    if (codes != null) {
      for (Code code : codes) {
        writer = code.execute(writer, scopes);
      }
    }
    return writer;
  }

  @Override
  public void append(String text) {
    if (appended == null) {
      appended = text;
    } else {
      appended = appended + text;
    }
  }

  // Expand the current set of scopes
  protected Object[] addScope(Object[] scopes, Object scope) {
    if (scope == null) {
      return scopes;
    } else {
      int length = scopes.length;
      Object[] newScopes = new Object[length + 1];
      System.arraycopy(scopes, 0, newScopes, 0, length);
      newScopes[length] = scope;
      return newScopes;
    }
  }
}
