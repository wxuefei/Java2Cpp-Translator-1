package edu.nyu.oop;

import java.io.*;

import edu.nyu.oop.util.ChildToParentMap;
import edu.nyu.oop.util.RecursiveVisitor;
import edu.nyu.oop.util.XtcProps;
import org.slf4j.Logger;
import xtc.tree.*;

/**
 * This class implements pretty printing of C++ code with C++ AST as input.
 */
public class CppPrinter extends RecursiveVisitor {
    private Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    private static int flag = -1;

    private ChildToParentMap childParentMap;

    private Printer printer;

    private String outputLocation = XtcProps.get("output.location");

    public CppPrinter(String outputFile) {
        Writer w = null;
        try {
            FileOutputStream fos = new FileOutputStream(outputLocation + outputFile);
            OutputStreamWriter ows = new OutputStreamWriter(fos, "utf-8");
            w = new BufferedWriter(ows);
            this.printer = new Printer(w);
        } catch (Exception e) {
            throw new RuntimeException("Output location not found. Create the /output directory.");
        }

        // Register the visitor as being associated with this printer.
        // We do this so we get some nice convenience methods on the printer,
        // such as "dispatch", You should read the code for Printer to learn more.
        printer.register(this);
    }


    // Auxiliary methods

    private String generate_temp_name(int x){
        String temp = "temp";
        temp = temp + Integer.toString(x);
        return temp;
    }

    public void printHeader(Node source) {
        childParentMap = new ChildToParentMap(source);
        flag=0;
        headerHeadOfFile();
        visit(source);
        printer.flush(); // important!
    }

    public void printCpp(Node source) {
        childParentMap = new ChildToParentMap(source);
        flag=1;
        cppHeadOfFile();
        visit(source);
        printer.flush();
    }

    public void printMain(Node source) {
        childParentMap = new ChildToParentMap(source);
        flag=2;
        mainHeadOfFile();
        visit(source);
        printer.flush();
    }

    private void headerHeadOfFile() {
        printer.pln("#pragma once");
        printer.pln();
        printer.pln("#include \"java_lang.h\"");
        printer.pln();
        printer.pln("using namespace java::lang;");
        printer.pln();
    }

    private void cppHeadOfFile(){
        printer.pln("#include <iostream>");
        printer.pln("#include \"output.h\"");
        printer.pln();
        printer.pln("using namespace java::lang;");
        printer.pln();
    }

    private void mainHeadOfFile(){
        printer.pln("#include \"output.h\"");
        printer.pln();
        printer.pln("using namespace java::lang;");
        printer.pln();
    }


    // Basics first

    public void visitBlock(GNode source){
        printer.pln("{");
        visit(source);
        printer.indent().decr().pln("}");
    }

    public void visitType(GNode source){
        String type = TypeResolver.typeToString(source);
        printer.p(type+" ");
    }

    public void visitVoidType(GNode source){
        String type = TypeResolver.typeToString(source);
        printer.p(type+" ");
    }

    public void visitPrimitiveType(GNode source) {
        printer.p(TypeResolver.primitiveTypeToString(source.getString(0)));
    }

    public void visitQualifiedIdentifier(GNode source){
        String qualifiedIndentifier=source.getString(0);
        printer.p(qualifiedIndentifier);
    }

    public void visitPrimaryIdentifier(GNode n){
        //System.out.println("printing Primary Identifier:");
        printer.p(n.getString(0));
    }

    public void visitNullLiteral(GNode n){
        printer.p("NULL");
    }

    public void visitStringLiteral(GNode source){
        String literal=source.getString(0);
        literal="__rt::literal("+literal+")";
        printer.p(literal);
    }

    public void visitIntegerLiteral(GNode source){
        String literal=source.getString(0);
        printer.p(literal);
    }

    public void visitFloatingPointLiteral(GNode source){
        String literal=source.getString(0);
        printer.p(literal);
    }

    public void visitDeclarators(GNode source){
        int declaratorSize = source.size();

        for (int i = 0; i < declaratorSize; i++) {
            Node declarator = source.getNode(i);
            String string_i = declarator.getString(0);
            Node assigner = declarator.getNode(2);

            if(assigner!=null) {
                printer.p(string_i + "=");
                dispatch(assigner);
            }
            else printer.p(string_i);

            if (i < declaratorSize - 1)
                printer.p(", ");
        }
    }

    public void visitFormalParameters(GNode source){

        int parameterSize = source.size();

        if(parameterSize==0) printer.p(")");

        for(int i=0;i<parameterSize;i++){
            Node formalParameter = source.getNode(i);
            //traverse modifiers and type
            visit(formalParameter);
            //output parameterName
            String parameterName = formalParameter.getString(3);
            printer.p(parameterName);
            if(i<parameterSize-1) printer.p(", ");
            else printer.p(")");
        }
    }

    public void visitModifiers(GNode source){
        if(flag==0) visit(source);
    }

    public void visitModifier(GNode source){
        if(source.getString(0).equals("static")) printer.p(source.getString(0)+" ");
    }

    public void visitArguments(GNode source) {
        printer.p("(");
        for(int i=0;i<source.size();i++){
            dispatch(source.getNode(i));
            if(i!=source.size()-1) printer.p(", ");
        }
        printer.p(")");
    }

    public void visitArgument(GNode source) {
        visit(source);
    }


    // Big structures

    public void visitCompilationUnit(GNode source) {
        visit(source);
    }

    public void visitNamespaceDeclaration(GNode source) {

        String namespace = source.getString(0);
        printer.indent().incr().p("namespace " + namespace);
        printer.pln(" {").pln();

        //continue to visit sub-nodes
        visit(source);

        printer.decr().indent().pln("}").pln();
    }

    public void visitForwardDeclarations(GNode source){
        visit(source);
    }

    public void visitForwardDeclaration(GNode source) {
        String type = source.getString(0);
        String name = source.getString(1);
        printer.indent().p(type).pln(" "+name+";").pln();
    }

    public void visitTypeSpecifiers(GNode source){
        visit(source);
    }

    public void visitTypeSpecifier(GNode source){
        String types = source.getString(0);
        String systemType = source.getString(1);
        String CustomType = source.getString(2);
        printer.indent().pln(types+" "+systemType+" "+CustomType+";").pln();
    }

    public void visitClassDeclaration(GNode source){
        //get class name and class modifiers
        Node classModifiers = source.getNode(0);
        String className = source.getString(1);

        //get class body info
        //Node classBody = source.getNode(5);

        //print class dec
        printer.indent().pln("struct "+className+" {").pln().incr();

        //visit source
        visit(source);

        //close the parentheses
        printer.decr().indent().pln("};").pln();
    }

    public void visitClassBody(GNode source){
        visit(source);
    }

    public void visitConstructorDeclaration(GNode source){

        String className = source.getString(2);
        printer.indent();
        printer.p(className+"(");

        //Formal parameters
        Node formalParameters = source.getNode(3);

        if (formalParameters!=null && formalParameters.getName().compareTo("FormalParameters")==0) dispatch(formalParameters);
        //there is no Formal parameters
        else printer.p(")");

        //Initializations

        Node initializations = source.getNode(4);
        if(initializations!=null && initializations.getName().compareTo("Initializations")==0){
            printer.pln().indent().p(":");
            for (int i=0; i<initializations.size();i++){
                Node initialization = initializations.getNode(i);

                printer.p(initialization.getString(0)+"("+initialization.getString(1)+")");

                if (initializations.size()>=1 && i<initializations.size()-1){
                    printer.p(",").pln().indent();
                }

            }
        }

        // last node block
        Node block = source.getNode(5);
        if (block != null){
            //to be implemented in later phase
            printer.p(" {").pln();
            visit(block);
            printer.indent().p("}").pln();
        }
        else {
            printer.pln(";");
        }

        printer.pln();
    }

    public void visitFieldDeclaration(GNode source){
        visit(source);
        printer.pln(";");
    }

    public void visitMethodDeclaration(GNode source){

        //traverse down modifiers node
        Node modifiers = source.getNode(0);
        if(modifiers!=null && modifiers.getName().compareTo("Modifiers")==0) dispatch(modifiers);

        String returnType = TypeResolver.typeToString(source.getNode(2));
        String methodName = source.getString(3);

        //printing returnType and the left parenthesis
        printer.indent().p(returnType+" ");
        printer.p(methodName+"(");

        Node formalParameters = source.getNode(4);
        Node block = source.getNode(7);

        if(formalParameters!=null && formalParameters.getName().compareTo("FormalParameters")==0) dispatch(formalParameters);
        //else there is no parameter
        else printer.p(")");

        if(block != null){
            //print what is inside the block
            //not yet to be implemented in headerfile printing
            dispatch(block);
        }
        else {
            printer.pln(";");
        }
        printer.indent().decr().pln();
    }


    // Statements

    public void visitExpressionStatement(GNode source){
        visit(source);
    }

    public void visitReturnStatement(GNode source){
        printer.p("return ");
        visit(source);
        printer.p(";");
    }

    public void visitConditionalStatement(GNode source){
        printer.p("if (");
        dispatch(source.getNode(0));
        printer.p(")");
        dispatch(source.getNode(1));
    }

    public void visitForStatement(GNode source){
        Node basic = source.getNode(0);
        printer.p("for(");
        dispatch(basic);
        Node block = source.getNode(1);
        dispatch(block);
    }

    public void visitBasicForControl(GNode source){
        dispatch(source.getNode(0));
        dispatch(source.getNode(1));
        dispatch(source.getNode(2));
        printer.p("; ");
        dispatch(source.getNode(3));
        printer.p("; ");
        dispatch(source.getNode(4));
    }

    public void visitWhileStatement(GNode source){
        printer.p("while (");
        dispatch(source.getNode(0));
        printer.p(")");
        dispatch(source.getNode(1));
    }


    // Expressions

    public void visitExpression(GNode source){
        //System.out.println(source);
        dispatch(source.getNode(0));
        printer.p(source.getString(1));
        dispatch(source.getNode(2));
        printer.pln(";");
    }

    public void visitAdditiveExpression(GNode source){
        dispatch(source.getNode(0));
        printer.p(" " + source.getString(1) + " ");
        dispatch(source.getNode(2));
    }

    public void visitMultiplicativeExpression(GNode source){
        dispatch(source.getNode(0));
        printer.p(" " + source.getString(1) + " ");
        dispatch(source.getNode(2));
    }

    public void visitThisExpression(GNode n){
        printer.p("__this");
    }

    public void visitExpressionList(GNode source){
        visit(source);
    }

    public void visitSelectionExpression(GNode source){
        dispatch(source.getNode(0));
        String field = source.getString(1);
        printer.p("->"+field);
    }

    public void visitStaticSelectionExpression(GNode source){
        printer.p("__");
        dispatch(source.getNode(0));
        String field = source.getString(1);
        printer.p("::"+field);
    }

    public void visitPrefixExpression(GNode source){
        String string_i = source.getNode(0).getString(0);
        String operation = source.getString(1);
        printer.p(operation+string_i+")");
    }

    public void visitPostfixExpression(GNode source){
        String string_i = source.getNode(0).getString(0);
        String operation = source.getString(1);
        printer.p(string_i+operation+")");
    }

    public void visitSubscriptExpression(GNode source){
        Node first=source.getNode(0);
        Node second=source.getNode(1);
        printer.p("(*");
        dispatch(first);
        printer.p(")");
        printer.p("[");
        dispatch(second);
        printer.p("]");
    }

    public void visitRelationalExpression(GNode source){
        Node primaryIndentifier = source.getNode(0);
        dispatch(primaryIndentifier);
        String compare = source.getString(1);
        printer.p(compare);
        Node expression = source.getNode(2);
        dispatch(expression);
    }

    public void visitEqualityExpression(GNode source){
        dispatch(source.getNode(0));
        printer.p(source.getString(1));
        dispatch(source.getNode(2));
    }

    public void visitCallExpression(GNode n){
        if (n.getNode(0) != null) {
            dispatch(n.getNode(0));
            printer.p("->");
        }
        printer.p(n.getString(2));
        dispatch(n.getNode(3));
        printer.pln(";");
    }

    public void visitStaticCallExpression(GNode n){
        if (n.getNode(0) != null) {
            printer.p("__");
            dispatch(n.getNode(0));
            printer.p("::");
        }
        printer.p(n.getString(2));
        dispatch(n.getNode(3));
        printer.pln(";");
    }

    public void visitPrintingExpression(GNode source){
        String printType = source.getString(1);
        printer.p("std::cout << ");
        visit(source);
        if (printType.equals("println"))
            printer.pln(" << std::endl;");
        else
            printer.pln(";");
    }

    public void visitNewClassExpression(GNode source){
        Node args=source.getNode(3);
        printer.p("__");
        dispatch(source.getNode(2));
        printer.p("::__init");
        dispatch(args);
    }

    public void visitCppNewClassExpression(GNode source){
        printer.p("new ");
        printer.p("__" + source.getString(0));
        printer.p("()");
    }

    public void visitNewArrayExpression(GNode source){
        printer.p("new ");
        Node identifier = source.getNode(0);
        for (int i = 0; i < source.getNode(1).size(); i++)
            printer.p("__rt::__Array<");
        dispatch(identifier);
        for (int i = 0; i < source.getNode(1).size(); i++)
            printer.p(">");
        for (int i = 0; i < source.getNode(1).size(); i++) {
            printer.p("(");
            dispatch(source.getNode(1).getNode(i));
            printer.p(")");
        }
    }

    public void visitNewCastExpression(GNode n){
        printer.p(n.getString(2));
        dispatch(n.getNode(3));
    }


    // Other Custom nodes

    public void visitMainMethodDefinition(GNode source){
        String mainMethodLocation = source.getString(0);
        printer.pln("int main(int argc, char* argv[]) {");
            // Implement generic interface between C++'s main function and Java's main function
        printer.pln("__rt::Array<String> args = new __rt::__Array<String>(argc - 1);");
        printer.pln();
        printer.pln("for (int32_t i = 1; i < argc; i++) {");
        printer.pln("(*args)[i - 1] = __rt::literal(argv[i]);");
        printer.pln("}");
        printer.pln();
        printer.pln(mainMethodLocation+"::main(args);");
        printer.pln();
        printer.pln("return 0;");
        printer.pln("}");
    }

    public void visitClassMethodDefinition(GNode source){
        String name = source.getString(0);
        String parent = source.getString(1);
        String __name = "__"+name.substring(name.lastIndexOf(".") + 1, name.length());
        String __parent = "__"+parent;

        printer.pln("Class "+__name+"::__class() {");
        printer.indent().p("static Class k = ");
        printer.pln("new __Class(__rt::literal(\""+name+"\")," + __parent + "::__class());");
        printer.indent().pln("return k;");
        printer.pln("}");
    }

    public void visitCBlock(GNode n) {
        printer.p("({");
        visit(n);
        printer.pln("})");
        Node parent = childParentMap.fetchParentFor(n);
        String grandParentName = childParentMap.fetchParentFor(parent).getName();
        if ("Block".equals(grandParentName))
            printer.p(";");
    }

}

