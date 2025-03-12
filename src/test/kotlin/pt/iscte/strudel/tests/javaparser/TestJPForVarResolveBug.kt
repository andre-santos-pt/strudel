package pt.iscte.strudel.tests.javaparser

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import org.junit.jupiter.api.Test
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.simpleNameAsString
import pt.iscte.strudel.vm.IVirtualMachine

class TestJPForVarResolveBug {


    @Test
    fun testPosition() {
        val src = """
            record Position(int line, int column) {
                    Position right() {
                        return new Position(line, column + 1);
                    }
                    
                    Position left() {
                        return new Position(line, column - 1);
                    }
                    
                    Position up() {
                        return new Position(line - 1, column);
                    }
                    
                    Position down() {
                        return new Position(line + 1, column);
                    }
                    
                    Position[] adjacentStraight() {
                        return new Position[] {
                            up(), right(), down(), up()
                        };
                    }
                    
                    Position[] adjacentStraightNoNegative() {
                        int len = 2;
                        if(line > 0)
                            len++;
                        if(column > 0)
                            len++;
                            
                        Position[] array = new Position[len];
                        int i = 0;
                        if(line > 0) {
                            array[i] = up();
                            i++;    
                        }
                        array[i] = right();
                        i++;
                        array[i] = down();
                        i++;
                        if(column > 0)
                            array[i] = left();
                        return array;
                    }
                    
                    Position[] adjacentWithDiagonal() {
                       return new Position[] {
                            up(), 
                            new Position(line - 1, column + 1),
                            right(),
                            new Position(line + 1, column + 1),
                            down(),
                            new Position(line + 1, column - 1),
                            left(),
                            new Position(line - 1, column - 1),
                       };
                    }
                }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)
        IVirtualMachine.create()


    }

    @Test
    fun testLiterals() {
        val src = """
           class Test {
                    static void main() {
                        boolean[] v = new boolean[] {true,false};
                        }
                }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)
        IVirtualMachine.create()


    }

    @Test
    fun test() {
        val src = """
            record Position(int line, int column) {
                    Position right() {
                        return new Position(line, column + 1);
                    }
                    
                    Position left() {
                        return new Position(line, column - 1);
                    }
                    
                    Position up() {
                        return new Position(line - 1, column);
                    }
                    
                    Position down() {
                        return new Position(line + 1, column);
                    }
                    
                    Position[] adjacentStraight() {
                        return new Position[] {
                            up(), right(), down(), up()
                        };
                    }
                    
                    Position[] adjacentStraightNoNegative() {
                        int len = 2;
                        if(line > 0)
                            len++;
                        if(column > 0)
                            len++;
                            
                        Position[] array = new Position[len];
                        int i = 0;
                        if(line > 0) {
                            array[i] = up();
                            i++;    
                        }
                        array[i] = right();
                        i++;
                        array[i] = down();
                        i++;
                        if(column > 0)
                            array[i] = left();
                        return array;
                    }
                    
                    Position[] adjacentWithDiagonal() {
                       return new Position[] {
                            up(), 
                            new Position(line - 1, column + 1),
                            right(),
                            new Position(line + 1, column + 1),
                            down(),
                            new Position(line + 1, column - 1),
                            left(),
                            new Position(line - 1, column - 1),
                       };
                    }
                }
                
           class PositionUtil {
            static Position[] diagonal(int n) {
                Position[] diagonal = new Position[n];
                for(int i = 0; i < n; i++) {
                    diagonal[i] = new Position(i, i);
                }
                return diagonal;
            }
            
            static Position[] gridPositions(int lines, int columns) {
                Position[] grid = new Position[lines * columns];
                for(int l = 0; l < lines; l++)
                    for(int c = 0; c < columns; c++)
                        grid[l * columns + c] = new Position(l, c);
                return grid;
            }
         }
        """.trimIndent()
        val module = Java2Strudel().load(src)
        println(module)


    }

    @Test
    fun `bug Report case -- supposed to fail for now`() {
        val src = """
            class Dummy { 
                Dummy(int a, int b) {
                
                }
            }
           class Test {
            static Dummy[] test(int n) {
                Dummy[] array = new Dummy[n];
                for(int i = 0; i < n; i++) {
                    array[i] = new Dummy(i, i);
                 }
                return array;
            }
          
         }
        """.trimIndent()
        StaticJavaParser.getParserConfiguration().languageLevel = ParserConfiguration.LanguageLevel.JAVA_20
        StaticJavaParser.getParserConfiguration()
            .setSymbolResolver(JavaSymbolSolver(CombinedTypeSolver()))

        val cu = StaticJavaParser.parse(src)
        cu.findAll(NameExpr::class.java).forEach {
            val r = it.calculateResolvedType()
            println(it.nameAsString + ": " + r.toString())
        }
        println(cu)
    }


}