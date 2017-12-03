package edu.nyu.oop;

import edu.nyu.oop.util.*;

import xtc.tree.Node;
import xtc.tree.GNode;

import java.util.*;
import edu.nyu.oop.CppPrinter;

/**
 * This is the entry point to the translator facilities. While Boot.java implements the user-program
 * interaction interface--gets command from the command line, and calls the method from the translator,
 * it delegates the work of actually making the ASTs and generating the source files to this class.
 * This class, on the other hand, provides an interface for interaction between Boot.java and the remaining
 * part of the translator. It defines the set of valid methods provided by the translator, and calls them
 * accordingly to fulfill requests made by Boot.java. The delegation pattern is implemented here.
 */
public class Translator {

    private Node root;
    private Map<String, ClassSignature> classTreeMap;
    private List<Node> javaAstList;
    private Node headerAst;
    private List<Node> mutatedCppAstList;
    private Node mainAst;

    public Translator(Node n) {
        root = n;
    }

    private void makeJavaAstList() {
        javaAstList = new ArrayList<>();
        javaAstList.add(root);
        javaAstList.addAll(new JavaFiveImportParser().parse((GNode) root));
    }

    private void makeHeaderAst() {
        ClassTreeVisitor classTreeVisitor = new ClassTreeVisitor();
        classTreeMap = classTreeVisitor.getClassTree(javaAstList);
        List<String> packageInfo = classTreeVisitor.getPackageInfo();
        HeaderAstBuilder headerAstBuilder = new HeaderAstBuilder(classTreeMap, packageInfo);
        headerAst = headerAstBuilder.buildHeaderAst();
    }

    private void makeHeaderFile() {
        CppPrinter cppPrinter = new CppPrinter("/output.h");
        cppPrinter.printHeader(headerAst);
    }

    private void makeMutatedCppAst() {
        Mutator mutator = new Mutator(classTreeMap);
        mutatedCppAstList = mutator.mutate(javaAstList);
    }

    private void makeImplementationFiles() {
        CppPrinter cppPrinter = new CppPrinter("/output.cpp");
        return;
    }

    public void run() {
        makeJavaAstList();
        makeHeaderAst();
        makeHeaderFile();
        makeMutatedCppAst();
        makeImplementationFiles();
    }

    public List<Node> getJavaAstList() {
        makeJavaAstList();
        return javaAstList;
    }

    public Node getHeaderAst() {
        makeJavaAstList();
        makeHeaderAst();
        return headerAst;
    }

    public void printCppHeader() {
        makeJavaAstList();
        makeHeaderAst();
        makeHeaderFile();
    }

    public List<Node> getMutatedCppAst() {
        makeJavaAstList();
        makeMutatedCppAst();
        return mutatedCppAstList;
    }

    public void printCppImplementation() {
        makeJavaAstList();
        makeMutatedCppAst();
        makeImplementationFiles();
    }

}
