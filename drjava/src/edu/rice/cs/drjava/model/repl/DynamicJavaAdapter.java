package edu.rice.cs.drjava.model.repl;

import java.util.*;
import java.io.*;
import java.net.URL;

import koala.dynamicjava.interpreter.*;
import koala.dynamicjava.interpreter.context.*;
import koala.dynamicjava.interpreter.error.*;
import koala.dynamicjava.interpreter.throwable.*;
import koala.dynamicjava.parser.wrapper.*;
import koala.dynamicjava.tree.*;
import koala.dynamicjava.tree.visitor.*;
import koala.dynamicjava.util.*;

import edu.rice.cs.util.classloader.StickyClassLoader;
import edu.rice.cs.drjava.DrJava;

/**
 * An implementation of the interpreter for the repl pane.
 * @version $Id$
 */
public class DynamicJavaAdapter implements JavaInterpreter {
  private Interpreter _djInterpreter;

  /**
   * Constructor.
   */
  public DynamicJavaAdapter() {
    _djInterpreter = new InterpreterExtension();
    // Allow access to private fields/methods from interpreter!
    //_djInterpreter.setAccessible(true);
  }

  /**
   * Interprets a string as Java source.
   * @param s the string to interpret
   * @return the Object generated by the running of s
   */
  public Object interpret(String s) {
    boolean print = false;
    /**
     * trims the whitespace from beginning and end of string
     * checks the end to see if it is a semicolon
     * adds a semicolon if necessary
     */
    s = s.trim();
    if (!s.endsWith(";")) {
      s += ";";
      print = true;
    }
    StringReader reader = new StringReader(s);
    try {
      Object result = _djInterpreter.interpret(reader, "DrJava");
      if (print)
        return  result; 
      else 
        return  JavaInterpreter.NO_RESULT;
    } catch (InterpreterInterruptedException iie) {
      throw iie;
    } catch (Throwable ie) {
      System.err.print(new Date() + ": ");
      System.err.println(ie);
      ie.printStackTrace();
      System.err.println("\n");
      throw  new RuntimeException(ie.toString());
    }
  }

  /**
   * Adds a path to the current classpath.
   * @param path the path to add
   */
  public void addClassPath(String path) {
    //DrJava.consoleErr().println("Added class path: " + path);
    _djInterpreter.addClassPath(path);
  }

  /**
   * Set the scope for unqualified names to the given package.
   * @param packageName Package to assume scope of.
   */
  public void setPackageScope(String packageName) {
    StringReader reader = new StringReader("package " + packageName + ";");
    _djInterpreter.interpret(reader, "DrJava");
  }

  /**
   * An extension of DynamicJava's interpreter that makes sure classes are
   * not loaded by the system class loader (when possible) so that future
   * interpreters will be able to reload the classes.
   * We also override the evaluation visitor to allow the interpreter to be
   * interrupted and to return NO_RESULT if there was no result.
   */
  public static class InterpreterExtension extends TreeInterpreter {

    /**
     * Constructor.
     */
    public InterpreterExtension() {
      super(new JavaCCParserFactory());
      classLoader = new ClassLoaderExtension(this);
      // We have to reinitialize these variables because they automatically
      // fetch pointers to classLoader in their constructors.
      nameVisitorContext = new GlobalContext(this);
      nameVisitorContext.setAdditionalClassLoaderContainer(classLoader);
      checkVisitorContext = new GlobalContext(this);
      checkVisitorContext.setAdditionalClassLoaderContainer(classLoader);
      evalVisitorContext = new GlobalContext(this);
      evalVisitorContext.setAdditionalClassLoaderContainer(classLoader);
      //System.err.println("set loader: " + classLoader);
    }

    /*
    public static Object invokeMethod(String key, Object obj, Object[] params) {
      DrJava.consoleErr().println("invoke: key=" + key + " obj=" + obj + " params len=" + params.length);
      MethodDescriptor md = (MethodDescriptor)methods.get(key);
      DrJava.consoleErr().println("md=" + md);

      return TreeInterpreter.invokeMethod(key, obj, params);
    }

    public void registerMethod(String sig,
                               MethodDeclaration  md,
                               ImportationManager im)
    {
      DrJava.consoleErr().println("register method: sig=" + sig + " md=" + md + " im=" + im);

      super.registerMethod(sig, md, im);
    }
    */

    /**
     * Extends the interpret method to deal with possible interrupted
     * exceptions.
     * Unfortunately we have to copy all of this method to override it.
     * @param is    the reader from which the statements are read
     * @param fname the name of the parsed stream
     * @return the result of the evaluation of the last statement
     */
    public Object interpret(Reader r, String fname) throws InterpreterException
    {
      try {
        SourceCodeParser p = parserFactory.createParser(r, fname);
        List    statements = p.parseStream();
        ListIterator    it = statements.listIterator();
        Object result = JavaInterpreter.NO_RESULT;

        while (it.hasNext()) {
          Node n = (Node)it.next();

          Visitor v = new NameVisitor(nameVisitorContext);
          Object o = n.acceptVisitor(v);
          if (o != null) {
            n = (Node)o;
          }

          v = new TypeChecker(checkVisitorContext);
          n.acceptVisitor(v);

          evalVisitorContext.defineVariables
            (checkVisitorContext.getCurrentScopeVariables());

          v = new EvaluationVisitorExtension(evalVisitorContext);
          result = n.acceptVisitor(v);
        }

        return result;
      } catch (ExecutionError e) {
        throw new InterpreterException(e);
      } catch (ParseError e) {
        throw new InterpreterException(e);
      }
    }
  }

  /**
   * A class loader for the interpreter.
   */
  public static class ClassLoaderExtension extends TreeClassLoader {
    private StickyClassLoader _stickyLoader;

    /**
     * Constructor.
     * @param         Interpreter i
     */
    public ClassLoaderExtension(Interpreter i) {
      super(i);
      // The protected variable classLoader contains the class loader to use
      // to find classes. When a new class path is added to the loader,
      // it adds on an auxilary classloader and chains the old classLoader
      // onto the end.
      // Here we initialize classLoader to be the system class loader.
      classLoader = getClass().getClassLoader();

      // don't load the dynamic java stuff using the sticky loader!
      // without this, interpreter-defined classes don't work.
      String[] excludes = {
        "edu.rice.cs.drjava.model.repl.DynamicJavaAdapter$InterpreterExtension",
        "edu.rice.cs.drjava.model.repl.DynamicJavaAdapter$ClassLoaderExtension"
      };

      // we will use this to getResource classes
      _stickyLoader = new StickyClassLoader(this,
                                            getClass().getClassLoader(),
                                            excludes);
    }

    public Class defineClass(String name, byte[] code)  {
      File file = new File("debug-" + name + ".class");


      /*
      try {
        FileOutputStream out = new FileOutputStream(file);
        out.write(code);
        out.close();
        DrJava.consoleErr().println("debug class " + name + " to " + file.getAbsolutePath());
      }
      catch (Throwable t) {}
      */
      
      Class c = super.defineClass(name, code);
      return c;
    }

    /**
     * Delegates all resource requests to {@link #classLoader}.
     * This method is called by the {@link StickyClassLoader}.
     */
    public URL getResource(String name) {
      return classLoader.getResource(name);
    }

    protected Class loadClass(String name, boolean resolve)
      throws ClassNotFoundException
    {
      Class clazz;

      // check the cache
      if (classes.containsKey(name)) {
        clazz = (Class) classes.get(name);
      }
      else {
        try {
          clazz = _stickyLoader.loadClass(name);
        }
        catch (ClassNotFoundException e) {
          // If it exceptions, just fall through to here to try the interpreter.
          // If all else fails, try loading the class through the interpreter.
          // That's used for classes defined in the interpreter.
          clazz = interpreter.loadClass(name);


        }
      }

      if (resolve) {
        resolveClass(clazz);
      }

      return clazz;
    }
  }
}
