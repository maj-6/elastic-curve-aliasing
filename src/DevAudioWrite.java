import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class DevAudioWrite {
	PCMInputStream ps;

	public static void main(String[] args) {
		DevAudioWrite dw = new DevAudioWrite();
		dw.ps = dw.new PCMInputStream();
		dw.ps.framesToFetch = 44100 * 2;

		AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, false);

		// params: InputStream, AudioFormat, length in frames
		AudioInputStream ais = new AudioInputStream(dw.ps, audioFormat, dw.ps.framesToFetch);

		try {
			System.out.println("ais.format()=" + ais.getFormat());
			System.out.println("ais.frameLength()=" + ais.getFrameLength());
			System.out.println("ais.available()=" + ais.available());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		File file = new File("test.wav");
		System.out.println("file is at following location:");
		System.out.println("" + file.getAbsolutePath());

		try {

			AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);

			System.out.println("finished AIS, available() = " + ais.available());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class PCMInputStream extends InputStream {
		private int cursor, idx;
		private int[] frameBytes = new int[2];
		int framesToFetch;
		private float[] data;

		void setData(float[] d) {
			data = d;
		}

		@Override
		public int read() throws IOException {
			while (available() > 0) {
				idx &= 1;
				if (idx == 0) // set up next frame's worth of data
				{
					cursor++; // count elapsing frames

					// Your audio data source call goes here.
					float audioVal = audioGet(cursor, data);

					// convert signed, normalized float to bytes:
					audioVal *= 32767; // scale value to 16 bits
					frameBytes[0] = (char) audioVal; // little byte
					frameBytes[1] = (char) ((int) audioVal >> 8); // big byte
				}
				return frameBytes[idx++]; // but only return one of the bytes per read()
			}
			return -1;
		}

		// Following is a substitute for your audio data source. Can be
		// an external audio call instead.
		// Input: if your function needs no inputs, eliminate the input param
		// Output: must be normalized signed float, one track of one frame.
		private float audioGet(int cursor, float[] data) {
			return data[cursor-1];
		}

		@Override
		public int available() {
			// Took a while to get this!
			// NOTE: not concurrency safe.
			// 1st half of sum: there are 2 reads available per frame to be read
			// 2nd half of sum: the bytes of the current frame that remain to be read
			return 2 * ((framesToFetch - 1) - cursor) + (2 - (idx % 2));
		}

		@Override
		public void reset() {
			cursor = 0;
			idx = 0;
		}
	}
}