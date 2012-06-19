package ro.uaic.info.fmse.jkrun;

import java.util.ArrayList;
import java.util.List;

public class K {

	public static boolean verbose = false;

	// os specific
	public static String userdir = System.getProperty("user.dir");
	public static String fileSeparator = System.getProperty("file.separator");
	public static String lineSeparator = System.getProperty("line.separator");
	public static String k_base = KPaths.getKBase(false);

	public static String kdir = userdir + fileSeparator + ".k";
	public static String krunDir = kdir + fileSeparator + "krun";
	public static String maude_in = krunDir + fileSeparator + FileUtil.generateUniqueName("maude_in.txt");
	public static String maude_out = krunDir + fileSeparator + FileUtil.generateUniqueName("maude_out.txt");
	public static String maude_err = krunDir + fileSeparator + FileUtil.generateUniqueName("maude_err.txt");

	// kast
	public static String kast = k_base + K.fileSeparator + "bin" + K.fileSeparator + getKastOnOs();

	public static String maude_io_cmd = krunDir + fileSeparator + FileUtil.generateUniqueName("io-cmd.maude");
	public static String maude_output = krunDir + fileSeparator + FileUtil.generateUniqueName("maudeoutput.xml");
	public static String processed_maude_output = krunDir + fileSeparator + FileUtil.generateUniqueName("maudeoutput_simplified.xml");
	
	// where to write the pretty-printed output of jkrun
	public static String krun_output = krunDir + fileSeparator + FileUtil.generateUniqueName("krun_output.txt");

	// the default values for jkrun commandline options
	public static String desk_file;
	public static String pgm;
	public static String k_definition;
	public static String main_module;
	public static String syntax_module;
	public static String parser = "kast";
	public static String compiled_def;
	public static String maude_cmd = "erewrite";
	public static String output_mode = "pretty";
	public static String xsearch_pattern = "=>! B:Bag";
	public static String rule_labels = "";

	// variables to store if that specific option was set; also set default values for options
	public static boolean help = false;
	public static boolean version = false;
	public static boolean io = true;
	public static boolean statistics = false;
	public static boolean color = true;
	public static boolean do_search = false;
	public static boolean parens = false;
	public static boolean log_io = false;
	public static boolean debug = false;
	public static boolean trace = false;

	private static String getKastOnOs() {
		if (System.getProperty("os.name").toLowerCase().contains("win"))
			return "kast.bat";
		return "kast";
	}
	
	public static void main(String args[]) {
		

	}
	
}
