import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;


public class badSmells {

    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("Grid.java");

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(in);
        } finally {
            in.close();
        }

        MethodAndClassDeclarationVisitor md = new MethodAndClassDeclarationVisitor();
        MessageChainVisitor mc = new MessageChainVisitor();
        TemporaryFieldVisitor fv = new TemporaryFieldVisitor();

        md.visit(cu, null);
        mc.visit(cu, null);
        fv.visit(cu, null);
    }

    public static class TemporaryFieldVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(FieldDeclaration fd, Object arg) {

            List<String> fieldNamesList = fd.getVariables().stream()
                    .map(var -> var.getName().asString())
                    .collect(Collectors.toCollection(ArrayList::new));

            for(String fieldName : fieldNamesList) {
                if (!isFieldUsedWithinMethods(fd, fieldName)) {
                    System.out.println("Temporary Field Detected: " + fieldName);
                }
            }
            super.visit(fd, arg);
        }

        private boolean isFieldUsedWithinMethods(FieldDeclaration fd, String fieldName) {

            //need to get cu as the methods in the whole AST will be out of scope here
            Optional<CompilationUnit> cu = fd.findCompilationUnit();
            List<MethodDeclaration> methodDeclarations = new ArrayList<>();

            if (cu.isPresent()) {
                methodDeclarations = cu.get().findAll(MethodDeclaration.class);
            } else {
                System.out.println("CompilationUnit not found.");
            }

            for (MethodDeclaration md : methodDeclarations) {
                List<MethodCallExpr> methodCallExpressions = new ArrayList<>();
                md.accept(new MethodCallVisitor(methodCallExpressions), null);

                for (MethodCallExpr methodCallExpr : methodCallExpressions) {
                    NodeList<Expression> arguments = methodCallExpr.getArguments();
                    for (Expression expression : arguments) {
                        if (expression.isNameExpr() && expression.asNameExpr().getName().asString().equals(fieldName)) {
                            return true; // Field is used within a method call argument
                        }
                    }
                }
            }
            return false; // Field is not used within method call arguments

        }

        private static class MethodCallVisitor extends VoidVisitorAdapter<Void> {
            private final List<MethodCallExpr> methodCallExpressions;

            public MethodCallVisitor(List<MethodCallExpr> methodCallExpressions) {
                this.methodCallExpressions = methodCallExpressions;
            }

            @Override
            public void visit(MethodCallExpr n, Void arg) {
                super.visit(n, arg);
                methodCallExpressions.add(n);
            }
        }
    }

    private static class MessageChainVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(MethodCallExpr call, Object arg) {
            if (call.getScope().isPresent() && call.getScope().get() instanceof MethodCallExpr) {
                System.out.println("Warning! Message Chain Detected: " + call.getScope().get() + " -> " + call.getName());
            }
            super.visit(call, arg);
        }
    }

    private static class ClassOrInterfaceDeclarationVisitor extends VoidVisitorAdapter {
        @Override
        public void visit(ClassOrInterfaceDeclaration cd, Object arg) {

            // Large Class (Easy: counting all lines of code - only including lines from the method body)
            if((cd.getEnd().get().line) - (cd.getBegin().get().line) - 1 > 100){
                System.out.println("Warning: class " + cd.getNameAsString() + " contains too many lines");
            }

            int statementCounter = 0;
            NodeList<BodyDeclaration<?>> members = cd.getMembers();

            for(BodyDeclaration member : members){
                //statementCounter += totalClassSize(member, statementCounter);
                if(member.isFieldDeclaration()) {
                    statementCounter++;
                }
                if(member.isMethodDeclaration()){
                    MethodDeclaration method = (MethodDeclaration) member;
                    statementCounter += method.getBody().get().getStatements().size() + 2;
                }
                if(member.isClassOrInterfaceDeclaration()) {
                    statementCounter +=2;
                    //member.accept(this,null);
                }
            }

            //System.out.println("statement counter = " + statementCounter + " - in class " + cd.getNameAsString());

            super.visit(cd, arg);
        }
    }

    private static class MethodAndClassDeclarationVisitor extends VoidVisitorAdapter {

        @Override
        public void visit(MethodDeclaration md, Object arg) {
            BlockStmt body = md.getBody().get();

            // Long Parameter List
            if (md.getParameters().size() > 5) {
                System.out.println("Warning: long parameter list in method " + md.getName() + "!\n");
            }

            // Long Method (Easy: counting all lines of code - only including lines from the method body)
            if ((body.getEnd().get().line) - (body.getBegin().get().line) - 1 > 20) {
                System.out.println("Warning: method " + md.getName() + " contains too many lines of code!\n");
            }

            // Long Method (Medium: counting statements - only including lines within the method declaration's curly braces)
            // All curly brace sets are counted as statements except the set belonging to the method declaration
            if(md.findAll(Statement.class).size() - 1 > 20) {
                System.out.println("Warning: method " + md.getName() + " contains too many statements!\n");
            }

            super.visit(md, arg);
        }
//                           _____________________________________________________________________
//                           | KEEP THE BELOW METHOD, JUST IN CASE (Works the same as md.findAll)|
//                           ---------------------------------------------------------------------
//        public int searchStatements(Node statementContents, int statementCounter) {
//            if(statementContents.getChildNodes().size() > 1) {
//                Node statement = statementContents.getChildNodes().get(1);
//                for (Node statementLine : statement.getChildNodes()) {
//                    System.out.println(statementLine + " statement line");
//                    statementCounter++;
//                    if ((statementLine.findAncestor(IfStmt.class).isPresent() || statementLine.findAncestor(WhileStmt.class).isPresent())
//                            && statementLine.getChildNodes().size() > 1) {
//                        searchStatements(statement, statementCounter);
//                    }
//                }
//            }
//            return statementCounter;
//        }

        public void visit(ClassOrInterfaceDeclaration cd, Object arg) {

            // Large Class (Easy: counting all lines of code - only including lines from the method body)
            if((cd.getEnd().get().line) - (cd.getBegin().get().line) - 1 > 100){
                System.out.println("Warning: class " + cd.getNameAsString() + " contains too many lines\n");
            }

            // Large Class (Medium: Counting all statements - only inside the class body (not including the set of curly braces belonging to the class declaration))
            int statementCounter = cd.findAll(Statement.class).size();
            statementCounter += ((cd.findAll(ClassOrInterfaceDeclaration.class).size() - 1) * 2); // Finds all nested class declarations and doubles the number to count the curly braces belonging to it (which are missed by finding statements)
            statementCounter += cd.findAll(FieldDeclaration.class).size(); // Finds all field declarations outside of methods within the class 'cd' and all nested classes
            statementCounter += cd.findAll(MethodDeclaration.class).size(); // Finds all method declarations within the class 'cd' and all nested classes
            statementCounter += cd.findAll(ConstructorDeclaration.class).size(); // Finds all constructor declarations
            statementCounter += cd.findAll(EnumDeclaration.class).size(); // Finds all Enum declarations

            if(statementCounter > 100){
                System.out.println("Warning: class " + cd.getNameAsString() + " contains too many statements!\n");
            }

            // Middle Man (middle man class is defined as one which the method calls used all belong to other classes and no functionality is provided)
            boolean middleMan = true;
            if(cd.findAll(MethodDeclaration.class).size() == 0) {
                for (ConstructorDeclaration c : cd.findAll(ConstructorDeclaration.class)) { // If the class creates an object with a constructor and has no methods then this is not a middle man class
                    for (Statement s : c.getBody().findAll(Statement.class))
                        middleMan = middleManCheck(s, middleMan);
                }
            } else {
                for(MethodDeclaration md : cd.findAll(MethodDeclaration.class)){
                    for(Statement s : md.getBody().get().findAll(Statement.class)){
                        middleMan = middleManCheck(s, middleMan);
                    }
                }
            }
            if(middleMan == true) // If middleMan is true then the class must be a middle man since there is not statement that adds functionality to the class
                System.out.println("Warning: class " + cd.getNameAsString() + " is a middle man class!");

            super.visit(cd, arg);
        }

        public boolean middleManCheck(Statement s, boolean middleMan){
            if(s instanceof ExpressionStmt){
                if (s.asExpressionStmt().getExpression().isMethodCallExpr()) {
                    MethodCallExpr mc = (MethodCallExpr) s.asExpressionStmt().getExpression();
                    if (mc.getScope().isEmpty()) {
                        middleMan = false; // If the method call does not use another class's method then the class 'cd' is not a middle man class
                    }
                    for(Expression e : mc.getArguments()){
                        if(!e.isMethodCallExpr())
                            middleMan = false; // If the statement is not a method call then the class cannot be a middle man
                        else{
                            if(e.asMethodCallExpr().getScope().isEmpty())
                                middleMan = false; // If the method call does not use another class's method then the class 'cd' is not a middle man class
                        }
                    }
                } else
                    middleMan = false; // If the statement is not a method call then the class cannot be a middle man
            } else if(s instanceof LocalClassDeclarationStmt){
                middleMan = false; // If the class has a nested class then it is not a middle man
            } else if(s instanceof ReturnStmt){
                if (s.asReturnStmt().getExpression().isPresent()) {
                    if(s.asReturnStmt().getExpression().get().isMethodCallExpr()) {
                        MethodCallExpr mc = (MethodCallExpr) s.asReturnStmt().getExpression().get();
                        if (mc.getScope().isEmpty()) {
                            middleMan = false; // If the return statement does not use another class's method then the class 'cd' is not a middle man class
                        }
                    } else if(s.asReturnStmt().getExpression().get().isNameExpr()){
                        middleMan = false; // If it returns something other than a variable call then this is not a middle man class
                    }
                }
            }
            return middleMan;
        }

    }
}
