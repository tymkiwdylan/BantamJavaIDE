package proj10LoverudeTymkiwCorrell.bantam.transpiler;

import proj10LoverudeTymkiwCorrell.bantam.ast.Program;
import proj10LoverudeTymkiwCorrell.bantam.parser.Parser;
import proj10LoverudeTymkiwCorrell.bantam.util.ErrorHandler;
import proj10LoverudeTymkiwCorrell.bantam.util.FileExtensionChanger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class TranspilerWriter {

    private final TranspilerVisitor transpilerVisitor;

    public TranspilerWriter(){
        transpilerVisitor = new TranspilerVisitor();
    }


    /**
     * Writes the program to a given file
     *
     * @param fileName the name of the file to be written
     * @throws IOException
     * */
    private void writeToFile(String fileName) throws IOException {
        String programStringBuilder = transpilerVisitor.getProgramStringBuilder().toString();
        File currFile = new File(fileName);
        if(currFile.exists()){
            currFile.delete();
        }
        PrintWriter writer = new PrintWriter(fileName);
        writer.print(programStringBuilder);
        writer.close();

    }

    /**
     * Visits the program and transpiles it
     *
     * @param filePath the name of the file to compile
     * @return fileToCompile the name of file that needs to be compiled
     * */
    public String visitAndWrite(String filePath) {
        ErrorHandler errorHandler = new ErrorHandler();
        Parser parser = new Parser(errorHandler);
        Program program = parser.parse(filePath);
        String fileToCompile = null;
        transpilerVisitor.visit(program);
        try {
            fileToCompile = FileExtensionChanger.fileWithChangedExtension(filePath);
            this.writeToFile(fileToCompile);
        }catch (IOException ex){
            System.out.println("Failed to rename and create java file");
        }
        transpilerVisitor.clearProgramStringBuilder();
        return fileToCompile;
    }


    public static void main(String[] args){
        TranspilerWriter transpilerWriter = new TranspilerWriter();
        String file =  transpilerWriter.visitAndWrite(args[0]);
    }

}
