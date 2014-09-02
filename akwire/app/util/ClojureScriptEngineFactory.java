/*
 * Copyright (c) 2009 Armando Blancas. All rights reserved.
 * 
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * 
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * 
 * You must not remove this notice, or any other, from this software.
 */
package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * Implementation of a {@code ScriptEngineFactory} for Clojure.
 *
 * @author Armando Blancas
 * @version 1.2
 */
public class ClojureScriptEngineFactory
        implements ScriptEngineFactory {

    private ScriptEngine engine;
    private List<String> extensions;
    private List<String> names;
    private List<String> mimeTypes;

    /*
     * Default Constructor.
     *
     * Initializes the cached immutable lists and creates the local
     * engine used to return default settings.
     */
    public ClojureScriptEngineFactory() {
        List<String> list = new ArrayList<String>(1);
        list.add("clj");
        extensions =  Collections.unmodifiableList(list);

        list = new ArrayList<String>(1);
        list.add("Clojure");
        names = Collections.unmodifiableList(list);

        list = new ArrayList<String>(2);
        list.add("application/clojure");
        list.add("text/clojure");
        mimeTypes =  Collections.unmodifiableList(list);

        engine = new ClojureScriptEngine(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns <i>Clojure Scripting Engine</i>.
     */
    public String getEngineName() {
        return (String) engine.get(ScriptEngine.ENGINE);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns <i>1.2</i>.
     */
    public String getEngineVersion() {
        return (String) engine.get(ScriptEngine.ENGINE_VERSION);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns a list with a single value <i>clj</i>.
     */
    public List<String> getExtensions() {
        return extensions;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns <i>Clojure</i>.
     */
    public String getLanguageName() {
        return (String) engine.get(ScriptEngine.LANGUAGE);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns <i>1.2</i>.
     */
    public String getLanguageVersion() {
        return (String) engine.get(ScriptEngine.LANGUAGE_VERSION);
    }

    /**
     * {@inheritDoc}
     * Uses the recommended sugared shortcut for calling Java methods
     * in idiomatic Clojure:
     * <p>
     * {@code (.method object arg)}
     */
    public String getMethodCallSyntax(String obj, String m, String... args) {
        if (obj == null)
            throw new NullPointerException("obj is null");
        if (m == null)
            throw new NullPointerException("m is null");

        StringBuilder b = new StringBuilder("(.");
        b.append(m).append(' ').append(obj);
        for (int i = 0; i < args.length; i++)
            b.append(' ').append(args[i]);
        b.append(')');
        return b.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns a list with two values:
     * <pre>
     * application/clojure
     * text/clojure
     * </pre>
     */
    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns a list with a single value <i>Clojure</i>.
     */
    public List<String> getNames() {
        return names;
    }

    /**
     * {@inheritDoc}
     * Uses the {@code println} function with the argument inlined:
     * <p>
     * {@code (println "Now is the time")}
     */
    public String getOutputStatement(String toDisplay) {
        if (toDisplay == null)
            throw new NullPointerException("toDisplay is null");

        return "(println \"" + toDisplay + "\")";
    }

    /**
     * {@inheritDoc}
     * <p>
     * The Clojure Scripting Engine supports these additional properties:
     * <p>
     * {@code clojure.source.path} Additional locations of Clojure source files,
     * to be appended to the value of "java.class.path". This is an optional
     * property and defaults to {@code null}.
     * <p>
     * {@code clojure.compile.path} The location for the generated .class files.
     * Defaults to {@code "classes"}.
     * <p>
     * {@code clojure.compile.warn-on-reflection} Whether to get a warning when Clojure will
     * use Java reflection. Defaults to {@code Boolean false}.
     */
    public Object getParameter(String key) {
        return (String) engine.get(key);
    }

    /**
     * {@inheritDoc}
     * It prints a Clojure program as a list of statements, each in its own line:
     * <pre>
     * (foo...)
     * (bar...)
     * </pre>
     */
    public String getProgram(String... statements) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < statements.length; i++)
            b.append(statements[i]).append('\n');
        return b.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * It always returns a new instance of the Clojure script engine. The Clojure runtime
     * is the same for all engines, but keeping multiple engines can simplify usage if
     * each one will set different bindings and redirection.
     * <p>
     */
    public ScriptEngine getScriptEngine() {
        return new ClojureScriptEngine(this);
    }

}