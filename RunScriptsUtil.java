package com.bgi.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bgi.model.ProcessParam;

public class RunScriptsUtil {

	public static int runPython(String[] param, String workName, Logger logger) {
		int exitCode = 1;
		try {
			Process proc = Runtime.getRuntime().exec(param);
			StreamProcess infoProcess = new StreamProcess(proc.getInputStream());
			StreamProcess errorProcess = new StreamProcess(proc.getErrorStream());
			infoProcess.start();
			errorProcess.start();
			exitCode = proc.waitFor();
		} catch (IOException e) {
			logger.error(workName + "  error", e);
		} catch (InterruptedException e) {
			logger.error(workName + "  error", e);
		} finally {
			logger.info(workName + "  finish exitCode==" + exitCode);
		}
		return exitCode;
	}

	public static ByteArrayOutputStream callPython(String[] param) throws Exception {
		int exitCode = 1;
		ByteArrayOutputStream baos = null;
		try {
			Process proc = Runtime.getRuntime().exec(param);
			baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len;
			InputStream inputStream = proc.getInputStream();
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			String errorLine;
			while ((len = inputStream.read(buffer)) > -1) {
				baos.write(buffer, 0, len);
			}
			while ((errorLine = errorReader.readLine()) != null) {
				System.out.println(errorLine);
			}
			errorReader.close();
			inputStream.close();
			baos.flush();
			exitCode = proc.waitFor();
		} finally {
			System.out.println("work finish exitCode==" + exitCode);
			if (exitCode != 0 && baos != null) {
				try {
					baos.close();
				} catch (IOException e) {
				}
			}
		}

		return exitCode == 0 ? baos : null;

	}

	static class StreamProcess extends Thread {

		private InputStream in;
		private Logger logger = LoggerFactory.getLogger(StreamProcess.class);

		public StreamProcess(InputStream in) {
			this.in = in;
		}

		@Override
		public void run() {
			BufferedReader isReader = new BufferedReader(new InputStreamReader(in));
			String isLine;
			try {
				while ((isLine = isReader.readLine()) != null) {
					logger.info(isLine);
				}
			} catch (IOException e) {
				logger.error("read error", e);
			} finally {
				try {
					isReader.close();
				} catch (IOException e) {
				}
			}
		}

		public void close() {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static ExecutorService threadPool = Executors.newCachedThreadPool();

	public static int callPythonToFile(ProcessParam params) throws Exception {

		OutputStream out = null;
		OutputStream errout = null;
		if (StringUtils.isNotEmpty(params.getOutputFile())) {
			Path fpath = Paths.get(params.getOutputFile());
			Files.createFile(fpath);
			out = Files.newOutputStream(fpath);
		}

		if (StringUtils.isNotEmpty(params.getLogFile())) {
			Path errPath = Paths.get(params.getLogFile());
			Files.createFile(errPath);
			errout = Files.newOutputStream(errPath);
		}

		StreamProcess2 info = null;
		StreamProcess2 error = null;
		int exitCode = 1;
		try {
			System.out.println(params.getRunParams());
			Process proc = Runtime.getRuntime().exec(params.getRunParams());
			info = new StreamProcess2(proc.getInputStream(), out);
			error = new StreamProcess2(proc.getErrorStream(), errout);
			threadPool.execute(info);
			threadPool.execute(error);
			exitCode = proc.waitFor();
		} finally {
			System.out.println("work finish exitCode==" + exitCode);
		}

		return exitCode;
	}

	static class StreamProcess2 extends Thread {

		private InputStream in;
		private OutputStream out;

		public StreamProcess2(InputStream in, OutputStream out) {
			this.in = in;
			this.out = out;
		}

		@Override
		public void run() {
			if (in == null)
				return;
			try {
				byte[] buffer = new byte[1024];
				int len;
				while ((len = in.read(buffer)) > -1) {
					out.write(buffer, 0, len);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
				}
				try {
					if (out != null) {
						out.close();
					}
				} catch (IOException e) {
				}
			}
		}

	}

}
