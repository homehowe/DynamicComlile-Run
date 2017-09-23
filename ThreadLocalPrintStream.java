/**
 * 
 * 线程安全的PrintStream
 *
 */
public class ThreadLocalPrintStream extends PrintStream {

	private InheritableThreadLocal<PrintStream> streams = null;

	private PrintStream defaultPrintStream = null;

	public ThreadLocalPrintStream(PrintStream defaultPrintStream) {
		super(defaultPrintStream);
		streams = new InheritableThreadLocal<PrintStream>();
		this.defaultPrintStream = defaultPrintStream;
		init(null);
	}

	public void init(PrintStream streamForCurrentThread) {
		streams.set(streamForCurrentThread);
	}

	public PrintStream getPrintStream() {
		PrintStream result = (PrintStream) streams.get();
		return ((result == null) ? defaultPrintStream : result);
	}

	public boolean checkError() {
		return (getPrintStream().checkError());
	}

	public void close() {
		getPrintStream().close();
	}

	public void flush() {
		getPrintStream().flush();
	}

	public void print(boolean b) {
		getPrintStream().print(b);
	}

	public void print(char c) {
		getPrintStream().print(c);
	}

	public void print(char[] s) {
		getPrintStream().print(s);
	}

	public void print(double d) {
		getPrintStream().print(d);
	}

	public void print(float f) {
		getPrintStream().print(f);
	}

	public void print(int i) {
		getPrintStream().print(i);
	}

	public void print(long l) {
		getPrintStream().print(l);
	}

	public void print(Object obj) {
		getPrintStream().print(obj);
	}

	public void print(String s) {
		getPrintStream().print(s);
	}

	public void println() {
		getPrintStream().println();
	}

	public void println(boolean x) {
		getPrintStream().println(x);
	}

	public void println(char x) {
		getPrintStream().println(x);
	}

	public void println(char[] x) {
		getPrintStream().println(x);
	}

	public void println(double x) {
		getPrintStream().println(x);
	}

	public void println(float x) {
		getPrintStream().println(x);
	}

	public void println(int x) {
		getPrintStream().println(x);
	}

	public void println(long x) {
		getPrintStream().println(x);
	}

	public void println(Object x) {
		getPrintStream().println(x);
	}

	public void println(String x) {
		getPrintStream().println(x);
	}

	public void write(byte[] buf, int off, int len) {
		getPrintStream().write(buf, off, len);
	}

	public void write(int b) {
		getPrintStream().write(b);
	}

	public void write(byte[] b) throws IOException {
		getPrintStream().write(b);
	}
}