import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.FileInputStream;


public class badSmells {

    public static void main(String[] args) throws Exception {
        FileInputStream in = new FileInputStream("AoT.java");

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(in);
        } finally {
            in.close();
        }

        new ClassDiagramVisitor().visit(cu, null);
    }


    private static class ClassDiagramVisitor extends VoidVisitorAdapter {

        /* public void visit(ClassOrInterfaceDeclaration n, Object arg){
            System.out.println("Class Name: " + n.getName());
            System.out.println("Class Implements: ");
            for (ClassOrInterfaceType coi : n.getImplementedTypes()) {
                System.out.println(coi.getName());
            }
            super.visit(n, arg);
        }

        public void visit(FieldDeclaration n, Object a){
            System.out.println("Field Type is: " + n.getElementType());
            for(VariableDeclarator v : n.getVariables()){
                System.out.println("Name: " + v.getName());
            }
        } */
    }
}
