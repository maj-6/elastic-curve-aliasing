import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import processing.core.PApplet;
import processing.core.PFont;

public class ElasticCurves extends PApplet {

	static double pi = 3.14159265359;

	static int w = 3840; // window resolution
	static int h = 2160;
	static int skip = 1;

	static float mx;
	static float my;

	static Scanner s = new Scanner(System.in);

	static int unit = 800; // number of pixels in one unit on the graph

	static double k_step = 0.0000005;
	static int pause_state = 1;

	static double k = 2.404; // starting multiplier of d(t); increases over time
	static int res = 200; // resolution of turtle plot
	static double cycles = 1;

	static float translate_x = w / 2;
	static float translate_y = h - (h / 4);

	static double period;
	static float j0 = 1;

	static int num_periods = 0;

	static int turtle_line_weight = 2; // stroke weight of lines
	static String blend_mode = "mix"; // blending mode

	static int r = 100;
	static int g = 300;
	static int b = 400;

	PFont f;

	static double status_message_duration = 100; // frames
	static DecimalFormat round;
	static DecimalFormat long_round;
	static DecimalFormat short_round;

	static boolean show_info_text = true;

	static int trace_quality = 60;
	static int line_height = 15;

	static int anim_frame = 0;
	static boolean anim_capture = false;

	static float dash_length = 10; // length in pixels of graph unit marker
	static float dash_num = 4; // number of markers

	static double audio_trace_duration = 2.0; // seconds
	static int audio_sample_rate = 44100;
	static int audio_bit_depth = 16;
	static double audio_gain = 1.0;

	static ArrayList<float[]> bessel_plot = new ArrayList<>();
	static ArrayList<float[]> turtle_plot = new ArrayList<>();
	static boolean show_axes = true;
	static int trace_mode = 0;

	static boolean normalize = true;

	static int periodicity = 0;

	static double error;
	static double error_threshold = 0.0000003;
	static boolean error_threshold_exceeded = false;
	static boolean diverging = false;

	static float[][] x_points;
	static float[][] y_points;
	static float[][] theta_points;
	static float[][] turtle_points;

	private static String status_message = "";

	@Override
	public void settings() {
		size(w, h);
	}

	@Override
	public void setup() {
		frameRate(60);
		// smooth();
		blendMode(BLEND);
		strokeCap(PROJECT);
		f = createFont("ProggyCleanTT", 12, true);

	}

	/////////////////////////////////////////////////////////////////////////////

	@Override
	public void draw() {

		clear();
		background(0);

		error = Math.abs(period - j0);
		error_threshold_exceeded = error > error_threshold;

		turtle_plot_gen();

		period = -(turtle_points[turtle_points.length - 1][1]) / (cycles * unit);
		// j0 = (float) SpecialFunction.j0(k); // value of bessel function j0 at k

		translate(translate_x, translate_y);

		if (show_axes) {

			strokeWeight(1);
			stroke(50);
			line(-width / 2 - Math.abs(translate_x), 0, width + Math.abs(translate_x), 0);
			line(0, -height / 2 - Math.abs(translate_y), 0, height + Math.abs(translate_x));

			/*
			 * for (int i = 0; i < 10; ++i) { float x = (float) i * unit / dash_num; line(x,
			 * -dash_length / 2, x, dash_length / 2); text(x / unit, x, -12); }
			 */
		}

		strokeWeight(turtle_line_weight);

		stroke(r, g, b);
		drawGraph(turtle_points);

		// stroke(r, g / 4, b / 8);
		// drawGraph(x_points);

		// stroke(r / 8, g / 4, b);
		// drawGraph(y_points);

		// stroke(r / 4, g / 8, b);
		// drawGraph(theta_points);

		cycles += k_step * (pause_state ^ 1);
		// k -= 0.02 * (pause_state ^ 1);

		if (trace_mode > 1) {

			strokeWeight(1);
			stroke(255, 128, 64);

			for (int i = 1; i < turtle_plot.size(); ++i) {

				float x_1 = turtle_plot.get(i - 1)[0];
				float y_1 = turtle_plot.get(i - 1)[1];

				float x_2 = turtle_plot.get(i)[0];
				float y_2 = turtle_plot.get(i)[1];

				line(x_1 * unit, y_1 * unit, x_2 * unit, y_2 * unit);

				turtle_plot.get(i - 1)[0] -= 0.0015 * (pause_state ^ 1);

			}

			if (pause_state == 0 && frameCount % (60 - (trace_quality - 1)) == 0) {
				turtle_plot.add(new float[] { 0, (float) (-period * cycles) });
			}
		}

		if (trace_mode > 0) {

			strokeWeight(1);
			stroke(64, 128, 255);

			for (int i = 1; i < bessel_plot.size(); ++i) {

				float x_1 = bessel_plot.get(i - 1)[0];
				float y_1 = bessel_plot.get(i - 1)[1];

				float x_2 = bessel_plot.get(i)[0];
				float y_2 = bessel_plot.get(i)[1];

				line(x_1 * unit, y_1 * unit, x_2 * unit, y_2 * unit);

				bessel_plot.get(i - 1)[0] -= 0.0015 * (pause_state ^ 1);

			}

			if (pause_state == 0 && frameCount % (60 - (trace_quality - 1)) == 0) {
				bessel_plot.add(new float[] { 0, -j0 });
			}

		}

		if (show_info_text) {

			translate(-translate_x, -translate_y);
			showText();

		}

		if (anim_capture) {
			save("/anim/" + anim_frame + ".png");
			++anim_frame;
		}
	}

	/////////////////////////////////////////////////////////////////////////////

	private void showText() {

		textFont(f, 16);
		fill(210);

		String[] strings = new String[] {

				"fps: " + frameRate,

				"view center: (" + round.format((-(translate_x - width / 2)) / unit) + ", "
						+ round.format((translate_y - height / 2) / unit) + ")",

				"cursor pos: (" + round.format((mx - translate_x) / unit) + ", "
						+ round.format(-(my - translate_y) / unit) + ")",

				"skip: " + skip, "paused = " + pause_state, "\n",

				"k = " + round.format(k),

				"k_step: " + round.format(k_step),

				new String(error_threshold_exceeded ? "\r" : "") + "error: " + long_round.format(error),

				"scale: " + unit + "px",

				"curve resolution: " + res,

				"trace size: " + bessel_plot.size() * trace_mode,

				"trace mode: " + trace_mode,

				"trace quality: " + trace_quality,

				"cycles: " + cycles,

				"n mod c = " + res % (int) cycles,

				"periodicity: " + periodicity,

				"periods: " + num_periods, "\n",

				"audio trace settings:", "\n",

				"gain: " + round.format(audio_gain) + "dB",

				"length: " + round.format(audio_trace_duration) + 's',

				"sample rate: " + audio_sample_rate,

				"bit depth: " + audio_bit_depth, "\n",

				"turtle graph settings:", "\n",

				"line weight: " + turtle_line_weight,

				"blend mode: " + blend_mode,

				"rgb: (" + r + ", " + g + ", " + b + ")", "\n",

				"animation frame: " + anim_frame, "\n", "\n",

		};

		for (int i = 0; i < strings.length; ++i) {

			int[] f = strings[i].charAt(0) == '\r' ? new int[] { 255, 80, 45 } : new int[] { 230, 230, 230 };
			fill(f[0], f[1], f[2]);
			text(strings[i], 6, line_height * (i + 1));
		}

		textAlign(CENTER);
		fill(100, 200, 100);
		text(status_message, width / 2, height - 50);
		textAlign(LEFT);

		if (frameCount % status_message_duration == 0) {
			status_message = "";
		}

	}

	@Override
	public void mouseDragged() {
		translate_x += mouseX - pmouseX;
		translate_y += mouseY - pmouseY;
	}

	@Override
	public void mouseMoved() {
		mx = mouseX;
		my = mouseY;
	}

	public void drawGraph(float[][] points) {

		for (int i = 1; i < points.length; ++i) {

			float x_1 = points[i - 1][0];
			float y_1 = points[i - 1][1];

			float x_2 = points[i][0];
			float y_2 = points[i][1];

			line(x_1, -y_1, x_2, -y_2);
			// point(x_1, y_1);
			// y coords inverted to follow convention of y pointing up
			// since pixel coords start in top left of the coord space

		}
	}

	public static void saveWav(float[] pcm_data, double audio_trace_duration) {

		DevAudioWrite dw = new DevAudioWrite();
		dw.ps = dw.new PCMInputStream();
		dw.ps.setData(pcm_data);
		dw.ps.framesToFetch = (int) (audio_sample_rate * audio_trace_duration);

		AudioFormat fmt = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audio_sample_rate, audio_bit_depth, 1, 2,
				audio_sample_rate, false);
		AudioInputStream ais = new AudioInputStream(dw.ps, fmt, dw.ps.framesToFetch);

		String fname = "audio_r" + res + "_c" + (int) cycles + "_k" + (int) k + "-"
				+ (int) (k + audio_sample_rate * audio_trace_duration) + ".wav";
		File file = new File(fname);

		try {

			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
			status_message = "saved audio trace to " + fname;
		} catch (Exception e) {
			e.printStackTrace();
			status_message = "failed writing audio";
		}

	}

	public static double turtle_displace(double k, int res) {

		// more efficient than plot_gen; only calculates
		// values necessary for audio trace

		double[] turtle_points = new double[res + 1];
		double p = 0;

		for (int n = 0; n < res + 1; ++n) {

			turtle_points[n] = p;

			p += Math.cos(k * Math.sin(n * (cycles * 2 * pi / res))) / res;
		}

		return turtle_points[turtle_points.length - 1];

	}

	public static void turtle_plot_gen() {

		periodicity = 0;

		double max_x = 0.0;
		num_periods = 0;
		float last_x = 0;

		turtle_points = new float[res + 1][2];

		double freq = cycles * 2 * pi / res;

		double theta = 0.0;
		float[] p = new float[2];

		// x_points = new float[res + 1][2];
		// y_points = new float[res + 1][2];
		theta_points = new float[res + 1][2];

		for (int n = 0; n < res + 1; ++n) {

			turtle_points[n] = new float[] { p[0], p[1] };
			// x_points[n] = new float[] { n * unit / res, p[0] };
			// y_points[n] = new float[] { n * unit / res, p[1] };
			// theta_points[n] = new float[] { n * unit / res, (float) (theta / k) * unit /
			// 4 };

			theta = k * Math.asin(Math.sin(n * freq));

			last_x = p[0];

			p[0] += (float) Math.asin(Math.sin(theta)) * unit / res;
			p[1] += (float) Math.acos(Math.cos(theta)) * unit / res;

			if (p[0] > max_x) {
				max_x = p[0];
			} else if (p[0] == max_x) {
				periodicity += 1;
			}

			if (last_x == turtle_points[0][0] && p[0] == turtle_points[1][0]) {
				num_periods++; // TODO fix this
			}

		}

		float sum_x = 0;
		float sum_y = 0;

		for (float[] t : turtle_points) {
			sum_x += t[0];
			sum_y += t[1];
		}

		float avg_x = sum_x / (turtle_points.length - 1);
		float avg_y = sum_y / (turtle_points.length - 1);

		for (float[] t : turtle_points) {
			t[0] -= avg_x;
			t[1] -= avg_y;
			// t[1] /= (turtle_points[turtle_points.length - 1][1]);
		}

		// return turtle_points;

	}

	@Override
	public void keyPressed() {

		switch (key) {
		case CODED:
			switch (keyCode) {
			case UP:
				unit *= 2;
				break;
			case DOWN:
				unit /= 2;
				break;
			case RIGHT:
				k += k_step * skip;
				break;
			case LEFT:
				k -= k_step * skip;
				break;
			}
			break;
		case '=':
			res += skip;
			break;
		case '-':
			if (res - skip > 0) {
				res -= skip;
			} else {
				res = 1;
			}
			break;
		case '+':
			trace_quality = trace_quality + skip > 60 ? 60 : trace_quality + skip;
			break;
		case '_':
			trace_quality = trace_quality + skip < 1 ? 1 : trace_quality - skip;
			break;
		case 'q':
			turtle_line_weight = turtle_line_weight - skip > 0 ? turtle_line_weight - skip : 1;
			break;
		case 'w':
			turtle_line_weight += skip;
			break;
		case 'a':
			skip /= 2;
			break;
		case 's':
			skip *= 2;
			break;
		case 'A':
			skip -= 1;
			break;
		case 'S':
			skip += 1;
			break;
		case 'd':
			skip = 1;
			break;
		case 'r':
			r = (r + skip) % 255;
			break;
		case 'g':
			g = (g + skip) % 255;
			break;
		case 'b':
			b = (b + skip) % 255;
			break;

		case 'R':
			r = (r - skip) % 255;
			break;
		case 'G':
			g = (g - skip) % 255;
			break;
		case 'B':
			b = (b - skip) % 255;
			break;

		case 'x':
			cycles += 0.000001 * skip;
			break;
		case 'z':
			cycles = cycles - 0.000001 * skip > 0 ? cycles - 0.000001 * skip : 1;
			break;
		case 'm':
			if (blend_mode.equals("mix")) {
				blendMode(ADD);
				blend_mode = "add";
			} else {
				blendMode(BLEND);
				blend_mode = "mix";
			}
			break;

		case '[':
			k_step -= 0.0001 * skip;
			break;
		case ']':
			k_step += 0.0001 * skip;
			break;
		case 'p':
			pause_state ^= 1; // xor toggles value
			break;
		case 'C':
			String fname = "screen_r" + res + "_c" + (int) cycles + "_k" + (int) k + ".png";
			save(fname);
			status_message = "saved screen capture to " + fname;
			break;
		case 'l':
			show_axes ^= true;
			break;
		case 't':

			if (trace_mode == 2) {
				bessel_plot.clear();
				turtle_plot.clear();
			}

			trace_mode = (trace_mode + 1) % 3;
			break;
		case 'T':
			show_info_text ^= true;
			break;
		case 'c':
			// saveWav(toPCM((int) (audio_trace_duration * audio_sample_rate)),
			// audio_trace_duration);
			break;
		case 'X':

			ArrayList<Float> pcmx = new ArrayList<>();

			for (float[] p : theta_points) {
				pcmx.add((float) (p[1] * audio_gain * 0.000001));
			}

			float[] pcmx2 = new float[pcmx.size()];

			for (int i = 0; i < pcmx.size(); ++i) {
				pcmx2[i] = pcmx.get(i);
			}

			saveWav(pcmx2, pcmx2.length / (double) audio_sample_rate);

			break;

		case 'n':
			audio_gain += 0.001 * skip;
		case 'N':
			audio_gain -= 0.001 * skip;

		case '>':
			audio_trace_duration += 0.01 * skip;
			break;
		case '<':
			audio_trace_duration -= 0.01 * skip;
			break;
		case 'J':
			try {
				makeOBJ(turtle_points);
				status_message = "generated obj file";
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			break;

		case 'P':
			anim_capture ^= true;
			anim_frame = 0;
			status_message = new String(anim_capture ? "started" : "stopped") + " screen capture";
			break;
		case 'D':
			try {
				get_divergence(6, 7);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;

		case 'V':

			ArrayList<Float> pcm = new ArrayList<>();

			try {
				BufferedReader in = new BufferedReader(new FileReader("test2.txt"));
				String v;
				while ((v = in.readLine()) != null) {
					pcm.add((float) 0.5 * Float.parseFloat(v));
					System.out.println(v);
				}
				in.close();

				float[] pcm2 = new float[pcm.size()];

				for (int i = 0; i < pcm.size(); ++i) {
					pcm2[i] = pcm.get(i);
				}

				saveWav(pcm2, pcm2.length / (double) audio_sample_rate);

			} catch (IOException e) {
				System.out.println("failed to read file");
			}

		}

	}

	private static void makeOBJ(float[][] data) throws IOException {

		String fname = "obj_r" + res + "_c" + (int) cycles + "_k" + (int) k + "-"
				+ (int) (k + audio_sample_rate * audio_trace_duration) + ".obj";

		prt("writing obj file to " + fname);

		ArrayList<String> edges = new ArrayList<>(data.length);

		for (int i = 0; i < data.length; ++i) {

			edges.add("l " + (i + 1) + " " + (i + 2));
			write("v " + data[i][0] + " " + data[i][1] + " " + (float) (0.01 * i), fname);

		}

		edges.remove(edges.size() - 1);// last edge connects to a nonexistent vertex

		for (String e : edges) {
			write(e, fname);
		}

	}

	private static void prt(String string) {
		System.out.println(string);

	}

	private void get_divergence(int range_min, int range_max) throws IOException {
		/*
		 * for some range of turtle resolutions, calculates the values of k at which the
		 * displacement of the turtle begins to diverge from the value of j0(k)
		 */

		int skip = 2;
		double last_value = 0.0;
		// ArrayList<Double> last_values = new ArrayList<>();

		System.out.println("calculating divergence...");

		for (int r = range_min; r < range_max; ++r) {

			double target_precision = 0.000000001;

			double step = 1.0;

			double j = 0.0;

			// int total_steps = 0;

			double start_time = millis();

			while (Math.abs(step) > target_precision) {
				boolean diverging = Math.abs(turtle_displace(j, r * skip) - (SpecialFunction.j0(j))) > target_precision;
				write(Double.toString(j), "steps.txt");
				step = diverging ? step / 4 : step * 4;
				j = diverging ? j - step : j + step;
				// total_steps += 1;
			}

			// write(Double.toString(millis() - start_time), (range_min + "-" + range_max +
			// "_time.txt"));
			// write(total_steps, "odds_rough_steps.txt");

			System.out.println("found value for j = " + j + " in " + (millis() - start_time) + "ms");
			write(Double.toString(j - last_value), (range_min + "-" + range_max + "_odds.txt"));
			last_value = j;

		}

		// float[] pcm = new float[last_values.size()+1];

		// for(int i = 0; i < last_values.size()-1; ++i) {
		// pcm[i] = (float) last_values.get(i).doubleValue();
		// }

		// saveWav(pcm, audio_trace_duration);
		// double sum = 0.0;
		// last_values.remove(0);
		// for (Double d : last_values) {
		// System.out.println(d);
		// sum += d;
		// }

		// System.out.println(sum / (range/2-1));

	}

	private float[] toPCM(float[] data) {

		int start_time = millis();
		int len = data.length;

		System.out.print("converting data to PCM");

		int progress_display_threshold = 32000000; // determines number of progress updates
		int estimate_sample = 24 * audio_sample_rate / res;
		double est = 0.0;

		float[] pcm = new float[len];

		for (int i = 0; i < len; ++i) {

			if (i == estimate_sample) {

				est = ((millis() - start_time) / (float) estimate_sample) * len / 1000.0;

				System.out.println("; this should take about "
						+ new String(est < 90 ? short_round.format(est) + "s" : short_round.format(est / 60.0) + "m"));
			}

			if (est > 30) {
				if (i % (len / ((len * res) / progress_display_threshold)) == 0) {
					System.out.println("... " + (int) (100 * (i / (float) len)) + "%");
				}
			}

			double p = audio_gain * (turtle_displace(k + (i * k_step), res) - SpecialFunction.j0(k + (i * k_step)));

			pcm[i] = (float) p;
		}

		System.out.println("finished in " + (millis() - start_time) / 1000.0 + "s\n");
		return pcm;

	}

	private static void write(String string, String fname) throws IOException {

		BufferedWriter bw = new BufferedWriter(new FileWriter(fname, true));
		bw.write(string + "\n");
		bw.flush();

	}

	public static void main(String[] args) {
		String[] processingArgs = { "Test" };
		short_round = new DecimalFormat("0.00");
		round = new DecimalFormat("0.0000");
		long_round = new DecimalFormat("0.0000000");
		ElasticCurves mySketch = new ElasticCurves();
		PApplet.runSketch(processingArgs, mySketch);
	}
}
