/**
 * 
 * 线程安全的InputStream
 * 
 *
 */
public class ThreadLocalInputStream extends InputStream {

	private InheritableThreadLocal<InputStream> streams = null;

	private InputStream defaultInputStream = null;

	public ThreadLocalInputStream(InputStream defaultInputStream) {
		super();
		streams = new InheritableThreadLocal<InputStream>();
		this.defaultInputStream = defaultInputStream;
		init(null);
	}

	public void init(InputStream streamForCurrentThread) {
		streams.set(streamForCurrentThread);
	}

	public InputStream getInputStream() {
		InputStream result = (InputStream) streams.get();
		return ((result == null) ? defaultInputStream : result);
	}

	public int available() throws IOException {
		return (getInputStream().available());
	}

	public void close() throws IOException {
		getInputStream().close();
	}

	public void mark(int readlimit) {
		getInputStream().mark(readlimit);
	}

	public boolean markSupported() {
		return (getInputStream().markSupported());
	}

	public int read() throws IOException {
		return (getInputStream().read());
	}

	public int read(byte[] b) throws IOException {
		return (getInputStream().read(b));
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return (getInputStream().read(b, off, len));
	}

	public void reset() throws IOException {
		getInputStream().reset();
	}

	public long skip(long n) throws IOException {
		return (getInputStream().skip(n));
	}
}