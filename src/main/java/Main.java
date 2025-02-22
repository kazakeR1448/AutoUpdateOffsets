import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static Scanner scanner = new Scanner(System.in);
    private static PatternClass patternClass;
    private static int[] offsets;
    private static long timeout;

    public static void main(String[] args) {
        patternClass = new PatternClass("arm64");
        System.out.print(" ____ _________ _____   ____   ___  _   _ _  _   ____  \n" +
                "| __ )___ / ___|_   _| / ___| / _ \\| | | | || | |  _ \\ \n" +
                "|  _ \\ |_ \\___ \\ | |   \\___ \\| | | | | | | || |_| | | |\n" +
                "| |_) |__) |__) || |    ___) | |_| | |_| |__   _| |_| |\n" +
                "|____/____/____/ |_|   |____/ \\__\\_\\\\___/   |_| |____/ ");
        System.out.println("\n");
        System.out.println("[B3ST-LOG] Main code: https://github.com/0NullBit0/NullUpdater");
        System.out.println("[B3ST-LOG] Code modified by B3ST SQU4D");
        System.out.println("[B3ST-LOG] Lead Developer: Rowell");
        System.out.println("-------------");
        System.out.println("[B3ST-LOG] Need 2 files: 'libOLD.so' (old) and 'libNEW.so' (new). ONLY 64 BIT");

        try {
            if (!patternClass.testCapstone()) {
                System.out.println("[B3ST-LOG] Capstone not properly working, quitting...");
                System.exit(1);
            } else {
                System.out.println("[B3ST-LOG] Status capstone: working");
            }

            String workingDir = System.getProperty("user.dir");
            Path offFilePath = Paths.get(workingDir, "offsets.txt");

            if (!Files.exists(offFilePath)) {
                Files.createFile(offFilePath);
                System.out.println("[B3ST-LOG] offsets.txt has been created because it did not exist");
                System.out.println("[B3ST-LOG] Please add the required offsets to offsets.txt in hexadecimal format, each on a new line");
            }

            offsets = Utils.extractOffsets(offFilePath);

            if (offsets == null || offsets.length == 0) {
                System.out.println("[B3ST-LOG] No offsets found in file offsets.txt. Please add offsets and try again");
                return;
            }

            Path oldLibFile = Paths.get("libOLD.so");
            Path newLibFile = Paths.get("libNEW.so");

            if (!Files.exists(oldLibFile) || !Files.exists(newLibFile)) {
                System.out.println("[B3ST-LOG] One or both required libraries (libOLD.so or libNEW.so) are missing. Please ensure both files are present.");
                return;
            }

            byte[] oldData = Utils.getData(oldLibFile);
            byte[] newData = Utils.getData(newLibFile);

            System.out.print("[B3ST-LOG] Enter the timeout in seconds for searching all offsets: ");
            timeout = TimeUnit.SECONDS.toMillis(scanner.nextLong());
            scanner.nextLine();

            System.out.println("-------------");
            System.out.println("[B3ST-LOG] Putting offsets in different streams and updating...");

            String outputPath = workingDir + "/out.txt";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath, false))) {
                ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
                ArrayList<Runnable> tasks = new ArrayList<>();

                for (int offset : offsets) {
                    tasks.add(() -> {
                        try {
                            processOffset(offset, newData, oldData, writer);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }

                for (Runnable task : tasks) {
                    executorService.submit(task);
                }

                executorService.shutdown();
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                System.out.println("[B3ST-LOG] Successfully and wrote offsets to out.txt");
            } catch (IOException | InterruptedException e) {
                System.out.println("[B3ST-LOG] WARNING: Could not write to out.txt or interrupted");
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.out.println("-------------");
            System.out.println("[B3ST-LOG] An error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("-------------");
            System.out.print("[B3ST-LOG] Press any key to exit...");
            scanner.nextLine();
        }
    }

    private static void processOffset(int offset, byte[] newData, byte[] oldData, BufferedWriter writer) throws IOException {
        int count = 0;
        long startTime = System.currentTimeMillis();

        while (true) {
            if (System.currentTimeMillis() - startTime > timeout) {
                System.out.println("[B3ST-LOG] Timeout: NO OFFSET FOUND FOR - " + Integer.toHexString(offset).toUpperCase());
                return;
            }

            ByteClass[] pattern = patternClass.getPattern(oldData, offset, count);
            if (pattern == null) {
                System.out.println("[B3ST-LOG] NO OFFSET FOUND FOR - " + Integer.toHexString(offset).toUpperCase());
                return;
            }

            ArrayList<String> newOffsets = Utils.kmpSearch(pattern, newData);
            if (newOffsets.isEmpty()) {
                count--;
            } else {
                String newOffset = newOffsets.get(0);
                if (newOffset.startsWith("0x")) {
                    newOffset = newOffset.substring(2);
                }

                System.out.println("[B3ST-LOG] old: " + Integer.toHexString(offset).toUpperCase() + " new: " + newOffset.toUpperCase());

                writer.write("Old: " + Integer.toHexString(offset).toUpperCase() + " New: " + newOffset.toUpperCase());
                writer.newLine();
                return;
            }
        }
    }
}