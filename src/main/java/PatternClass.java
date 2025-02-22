import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.Math;
import capstone.Capstone;

public class PatternClass {
    private String aarch;
    private Capstone cs;

    public PatternClass(String arch) {
        this.aarch = arch;
        switch (this.aarch) {
            case "arm":
                this.cs = new Capstone(0, 0);
                break;
            case "arm64":
                this.cs = new Capstone(1, 0);
                break;
            default:
                this.cs = new Capstone(1, 0);
                this.aarch = "arm64";
                break;
        }
    }

    public ByteClass[] getPattern(byte[] data, int address, int mode) {
        int addition;
        Capstone.CsInsn[] insn;
        ByteClass[] pattern;
        int kount;

        if (mode == 0) {
            addition = 0;
        } else if (mode > 0) {
            addition = 4 * ((int) Math.pow(2.0d, mode + 2));
        } else {
            addition = 4 * mode;
        }

        int endOfFuncAddr = getEOF(data, address) + addition;
        if (endOfFuncAddr > data.length || endOfFuncAddr < address) {
            return null;
        }

        byte[] toEnd = new byte[endOfFuncAddr - address];
        System.arraycopy(data, address, toEnd, 0, toEnd.length);

        // Casting the result to CsInsn[]
        insn = (Capstone.CsInsn[]) this.cs.disasm(toEnd, address);
        pattern = new ByteClass[toEnd.length];
        kount = 0;

        switch (this.aarch) {
            case "arm64":
                for (int i2 = 0; i2 < insn.length; i2++) {
                    if (insn[i2].getMnemonic().equals("ldr") || (insn[i2].getMnemonic().equals("str") && Utils.hasTwoComma(insn[i2].getOpStr()))) {
                        for (int j = 0; j < 3; j++) {
                            pattern[kount] = new ByteClass((byte) -1, true);
                            kount++;
                        }
                        pattern[kount] = new ByteClass(data[((int) insn[i2].getAddress()) + 3], false);
                        kount++;
                    } else if (insn[i2].getMnemonic().equals("ldrb") || (insn[i2].getMnemonic().equals("strb") && Utils.hasTwoComma(insn[i2].getOpStr()))) {
                        for (int j2 = 0; j2 < 3; j2++) {
                            pattern[kount] = new ByteClass((byte) -1, true);
                            kount++;
                        }
                        pattern[kount] = new ByteClass((byte) 57, false);
                        kount++;
                    } else if (insn[i2].getMnemonic().equals("add")) {
                        for (int j3 = 0; j3 < 3; j3++) {
                            pattern[kount] = new ByteClass((byte) -1, true);
                            kount++;
                        }
                        pattern[kount] = new ByteClass(data[((int) insn[i2].getAddress()) + 3], false);
                        kount++;
                    } else {
                        Pattern regEx = Pattern.compile("#0x[0-9a-fA-F]{4,}", 2);
                        Matcher matcher = regEx.matcher(insn[i2].getOpStr());
                        if (matcher.find()) {
                            for (int j4 = 0; j4 < 4; j4++) {
                                pattern[kount] = new ByteClass((byte) -1, true);
                                kount++;
                            }
                        } else {
                            for (int j5 = 0; j5 < 4; j5++) {
                                pattern[kount] = new ByteClass(data[((int) insn[i2].getAddress()) + j5], false);
                                kount++;
                            }
                        }
                    }
                }
                break;
        }

        if (pattern.length < 0 || pattern[0] == null) {
            System.out.println("[B3ST-LOG] WARNING PROBABLY INVALID ADDRESS");
            return null;
        }
        return pattern;
    }

    private int getEOF(byte[] data, int address) {
        int lengthToEnd = data.length - address;
        byte[] toEnd = new byte[lengthToEnd];
        System.arraycopy(data, address, toEnd, 0, lengthToEnd);

        Capstone.CsInsn[] insn = (Capstone.CsInsn[]) this.cs.disasm(toEnd, address);
        for (int i2 = 0; i2 < insn.length; i2++) {
            if (insn[i2].getMnemonic().equals("ret")) {
                return ((int) insn[i2].getAddress()) + 4;
            }
        }
        return -1;
    }

    public boolean testCapstone() {
        byte[] armCODE = new byte[]{1, 0, -96, -29};
        byte[] arm64CODE = new byte[]{32, 0, Byte.MIN_VALUE, -46};

        switch (this.aarch) {
            case "arm64":
                Capstone.CsInsn[] insnARMV8 = (Capstone.CsInsn[]) this.cs.disasm(arm64CODE, 0L);
                if (insnARMV8.length > 0) {
                    return true;
                }
                break;
            case "arm":
                Capstone.CsInsn[] insnARMV7 = (Capstone.CsInsn[]) this.cs.disasm(armCODE, 0L);
                if (insnARMV7.length > 0) {
                    return true;
                }
                break;
        }
        return false;
    }
}