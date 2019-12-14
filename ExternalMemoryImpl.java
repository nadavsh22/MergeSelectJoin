package Join;

import javafx.scene.control.cell.TextFieldListCell;

import java.awt.image.AreaAveragingScaleFilter;
import java.io.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import static java.lang.Integer.min;

public class ExternalMemoryImpl extends IExternalMemory {
    private static final int offset = 0;
    private static final int KB = 1024;
    private static final int MB = KB * KB;
    private static final int blockSize = KB * 4;
    private static final int charSize = 2;
    private static final int lenOfFirstCol = 10;
    private static final int lenOfSecondCol = 20;
    private static final int lenOfDelimiter = 20;
    private static final int charsInRow = lenOfFirstCol + (lenOfDelimiter + lenOfSecondCol) * 2;
    private static final int sizeOfRow = charsInRow * charSize;
    private static final int rowsInBlock = (int) Math.ceil(blockSize / sizeOfRow);
    private static final int bufferSizeInBytes = 10 * MB; //change back to whatever
    private static final int bufferSizeInBlocks = (int) Math.ceil(bufferSizeInBytes / blockSize);

    class BlockHandler {
        private final BufferedReader reader;
        private int pIndex = 0;
        private ArrayList<String> blockRows = new ArrayList<>();

        BlockHandler(String tmpFilePath) {
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(tmpFilePath));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                reader = null;
            }
            this.reader = reader;
            try {
                this.loadBlockIntoMemory();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        protected String advancePointer() throws IOException {
            this.pIndex++;
            if (this.blockRows.size() == pIndex) {
                this.loadBlockIntoMemory();
            }
            return this.blockRows.get(pIndex);
        }

        protected String currentRow() {
            return this.blockRows.get(pIndex);
        }

        protected void loadBlockIntoMemory() throws IOException {
            this.blockRows.clear();
            for (int i = 0; i < rowsInBlock; i++) {
                this.blockRows.add(this.reader.readLine());
            }
            this.pIndex = 0;
        }
    }

    private int B(String inPath) {
        try {
            Path path = Paths.get(inPath);
            float lineCount = Files.lines(path).count();
            return (int) (Math.ceil(lineCount / (float) rowsInBlock));
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

    }

    private ArrayList<String> sortBuffer(BufferedReader br, int numBlocks) throws IOException {
        int numRows = numBlocks * rowsInBlock;
        ArrayList<String> lines = new ArrayList<String>();
        String line;
        int counter = 0;
        while ((line = br.readLine()) != null) {
            lines.add(line);
            counter++;
            if (counter >= numRows) {
                break;
            }
        }
        Collections.sort(lines);
        return lines;
    }

    /**
     * generate a path to temp file and append it to list of paths
     *
     * @param tmpPath
     * @param tmpFilePaths
     */
    private void generateTmpFile(String tmpPath, ArrayList<String> tmpFilePaths) {
        File directory = new File(tmpPath);
        if (!directory.isDirectory()) {
            directory.mkdir();
        }
        try {
            String fullNewPath = File.createTempFile("ex3", ".txt", directory).getPath();
            tmpFilePaths.add(fullNewPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private ArrayList<String> phaseOneSort(String in, String tmpPath, BufferedReader br) throws IOException {
        int left = 0;
        int blocksInFile = B(in);
        ArrayList<String> tmpFilePaths = new ArrayList<String>();
        while (left < blocksInFile) {
            int right = min((left + bufferSizeInBlocks), blocksInFile);
            generateTmpFile(tmpPath, tmpFilePaths);
            String partitionTmpPath = tmpFilePaths.get(tmpFilePaths.size() - 1);
            ArrayList<String> sortedRows = sortBuffer(br, right - left);
            BufferedWriter currentTmpFile = new BufferedWriter(new FileWriter(partitionTmpPath));
            for (int i = 0; i < sortedRows.size(); i++) {
                currentTmpFile.write(sortedRows.get(i));
                if (i < sortedRows.size() - 1)
                    currentTmpFile.newLine();
            }
            currentTmpFile.close();
            left = right;
        }
        return tmpFilePaths;
    }

    //TODO: write method
    private void phaseTwoSort(ArrayList<String> filePaths, String out) throws IOException {
        ArrayList<BlockHandler> blockHandlers = getHandlers(filePaths);
        PriorityQueue<BlockHandler> blockPriorityQueue = new PriorityQueue<BlockHandler>(Comparator.comparing(BlockHandler::currentRow));
        blockPriorityQueue.addAll(blockHandlers);
        BufferedWriter writer = new BufferedWriter(new FileWriter(out), blockSize);
        while (!blockPriorityQueue.isEmpty()) {
            BlockHandler blockWithMinimalRow = blockPriorityQueue.peek();
            String currentRow = blockWithMinimalRow.currentRow();
            writer.write(currentRow);
            writer.newLine();
            String nextRow = blockWithMinimalRow.advancePointer();
            if (nextRow == null) {
                blockPriorityQueue.remove(blockWithMinimalRow);
            } else {
                blockPriorityQueue.remove(blockWithMinimalRow); // remove and add with the new current row pointer
                blockPriorityQueue.add(blockWithMinimalRow); // will priorities correctly
            }

        }
        writer.close();
        for (BlockHandler handler : blockHandlers) {
            handler.reader.close();
        }
    }

    private ArrayList<BlockHandler> getHandlers(ArrayList<String> filePaths) {
        ArrayList<BlockHandler> blockHandlers = new ArrayList<BlockHandler>();
        for (String path : filePaths) {
            blockHandlers.add(new BlockHandler(path));
        }
        return blockHandlers;
    }

    @Override
    public void sort(String in, String out, String tmpPath) {

        try {
            BufferedReader br = new BufferedReader(new FileReader(in), bufferSizeInBytes);
            ArrayList<String> filePaths = phaseOneSort(in, tmpPath, br);
            br.close();
            phaseTwoSort(filePaths, out);
            for (String path : filePaths) {
                new File(path).delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected int compareRowColumn(String row1, String row2) {
        String row1Col = row1.substring(offset, lenOfFirstCol);
        String row2Col = row2.substring(offset, lenOfFirstCol);
        return row1Col.compareTo(row2Col);
    }

    protected void joinSpecifics(BlockHandler Tr, BlockHandler Ts, BlockHandler Gs, BufferedWriter writer) throws IOException {
        String currTr = Tr.currentRow();
        String currTs = Ts.currentRow();
        String currGs = Gs.currentRow();
        while (currTr != null && currGs != null) {
            while (currTr != null && compareRowColumn(currTr, currGs) < 0) {
                currTr = Tr.advancePointer();
            }
            while (currGs != null && compareRowColumn(currTr, currGs) > 0) {
                currGs = Gs.advancePointer();
            }
            while (currTr != null && compareRowColumn(currTr, currGs) == 0) {
                Ts = Gs;
                currTs = currGs;
                while (currTs != null && compareRowColumn(currTs, currTr) == 0) {
                    writer.write(currTr + currTs.substring(lenOfFirstCol));
                    writer.newLine();
                    currTs = Ts.advancePointer();
                }
                currTr = Tr.advancePointer();
            }
            Gs = Ts;
            currGs = currTs;
        }
        writer.close();
    }

    @Override
    protected void join(String in1, String in2, String out, String tmpPath) {
        BlockHandler Tr = new BlockHandler(in1);
        BlockHandler Ts = new BlockHandler(in2);
        BlockHandler Gs = new BlockHandler(in2);
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(out));
            joinSpecifics(Tr, Ts, Gs, bufferedWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean checkCondition(String row, String substr) {
        String currRowCol1 = row.substring(offset, lenOfFirstCol);
        return currRowCol1.contains(substr);
    }

    protected void selector(String out, String substrSelect, BlockHandler handler) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(out));
        String currRow = handler.currentRow();
        while (currRow != null) {
            if (checkCondition(currRow, substrSelect)) {
                writer.write(currRow);
                writer.newLine();
            }
            currRow = handler.advancePointer();
        }
        writer.close();
    }

    @Override
    protected void select(String in, String out, String substrSelect, String tmpPath) {
        BlockHandler blockHandler = new BlockHandler(in);
        try {
            selector(out, substrSelect, blockHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String getTmpFilePath(String tmpPath, ArrayList<String> paths) {
        generateTmpFile(tmpPath, paths);
        return paths.get(paths.size() - 1);
    }

    @Override
    public void joinAndSelectEfficiently(String in1, String in2, String out,
                                         String substrSelect, String tmpPath) {

        ArrayList<String> paths = new ArrayList<>();
        String selectIn1 = getTmpFilePath(tmpPath, paths);
        String selectIn2 = getTmpFilePath(tmpPath, paths);
        String sortSelect1 = getTmpFilePath(tmpPath, paths);
        String sortSelect2 = getTmpFilePath(tmpPath, paths);

        select(in1, selectIn1, substrSelect, tmpPath);
        select(in2, selectIn2, substrSelect, tmpPath);

        sort(selectIn1, sortSelect1, tmpPath);
        sort(selectIn2, sortSelect2, tmpPath);

        join(sortSelect1, sortSelect2, out, tmpPath);
        for (String path : paths) {
            new File(path).delete();
        }
    }
}