import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;

public class Utils {
    public static ArrayList<String> kmpSearch(ByteClass[] pattern, byte[] data) {
        ArrayList<String> foundAddresses = new ArrayList<>();
        int m = pattern.length;
        int n = data.length;
        int[] longestPS = computeLPS(pattern, m);
        int i = 0;
        int j = 0;
        while (i < n) {
            if (pattern[j].getValue() == data[i] || pattern[j].isWildcard()) {
                j++;
                i++;
            }
            if (j == m) {
                foundAddresses.add("0x" + Integer.toHexString(i - j).toUpperCase());
                j = longestPS[j - 1];
            } else if (i < n && pattern[j].getValue() != data[i] && !pattern[j].isWildcard()) {
                if (j != 0) {
                    j = longestPS[j - 1];
                } else {
                    i++;
                }
            }
        }
        return foundAddresses;
    }

    private static int[] computeLPS(ByteClass[] pattern, int m) {
        int[] longestPS = new int[m];
        int length = 0;
        int i = 1;
        longestPS[0] = 0;
        while (i < m) {
            if (pattern[i].getValue() == pattern[length].getValue() || pattern[i].isWildcard()) {
                length++;
                longestPS[i] = length;
                i++;
            } else if (length != 0) {
                length = longestPS[length - 1];
            } else {
                longestPS[i] = length;
                i++;
            }
        }
        return longestPS;
    }

    public static byte[] getData(Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean hasTwoComma(String opStr) {
        int commaCount = 0;
        for (int i = 0; i < opStr.length(); i++) {
            if (opStr.charAt(i) == ',') {
                commaCount++;
            }
        }
        return commaCount == 2;
    }

    public static void printPattern(ByteClass[] pattern) {
        System.out.print(getPatternString(pattern));
        System.out.println();
    }

    public static String getPatternString(ByteClass[] pattern) {
        String str;
        String buf = "";
        for (int i = 0; i < pattern.length; i++) {
            if (pattern[i].isWildcard()) {
                str = buf + "?? ";
            } else {
                str = buf + String.format("%02X", Byte.valueOf(pattern[i].getValue())) + " ";
            }
            buf = str;
        }
        return buf;
    }

    public static int getINTMAX() {
        int val = 0 ^ (-1);
        return val >>> 1;
    }

    public static int[] extractOffsets(Path filePath) {
        ArrayList<Integer> offsetList = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(filePath);
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    try {
                        int offset;
                        if (line.startsWith("0x")) {
                            offset = Integer.parseInt(line.substring(2), 16);
                        } else {
                            offset = Integer.parseInt(line, 16);
                        }
                        offsetList.add(offset);
                    } catch (NumberFormatException e) {
                        System.err.println("[B3ST-LOG] Invalid offset format in offsets.txt: " + line);
                    }
                }
            }
            return offsetList.stream().mapToInt(Integer::intValue).toArray();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("[B3ST-LOG] WARNING: No offsets found in offsets.txt");
            return new int[0];
        }
    }

    public static Path getValidPath(Scanner inpGetter, String prompt) {
        Path filePath;
        do {
            System.out.print(prompt);
            String path = inpGetter.nextLine();
            filePath = Paths.get(path, new String[0]);
            if (!Files.exists(filePath, new LinkOption[0])) {
                System.out.println("[B3ST-LOG] Path doesnt exist");
            }
        } while (!Files.exists(filePath, new LinkOption[0]));
        return filePath;
    }
}